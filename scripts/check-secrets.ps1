[CmdletBinding()]
param(
    [switch]$ReportOnly
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$findings = [System.Collections.Generic.List[object]]::new()

$supportedExtensions = @(
    ".yml", ".yaml", ".properties", ".env", ".json", ".xml",
    ".py", ".java", ".js", ".ts", ".vue", ".ps1", ".sh"
)

function Add-SecretFinding {
    param(
        [string]$Path,
        [int]$LineNumber,
        [string]$Kind,
        [string]$Key
    )

    $findings.Add([PSCustomObject]@{
        Path = $Path
        Line = $LineNumber
        Kind = $Kind
        Key = $Key
    })
}

function Test-PlaceholderValue {
    param([string]$Value)

    $normalized = $Value.Trim().Trim('"').Trim("'").TrimEnd(",")
    if ([string]::IsNullOrWhiteSpace($normalized)) {
        return $true
    }

    $placeholderPattern = '^(?i)(null|none|nil|example|placeholder|changeme|change-me|dummy|sample|your[-_].*|<.*>|__.*__)$'
    if ($normalized -match $placeholderPattern) {
        return $true
    }

    # Environment references are safe only when they do not conceal a real fallback.
    if ($normalized -match '^\$\{[A-Za-z0-9_]+\}$') {
        return $true
    }
    if ($normalized -match '^\$\{[A-Za-z0-9_]+(?::-|:)([^}]*)\}$') {
        $fallback = $matches[1]
        return [string]::IsNullOrWhiteSpace($fallback) -or $fallback -match $placeholderPattern
    }

    return $normalized -match '^\$(' `
        -or $normalized -match '^\$env:' `
        -or $normalized -match '^(?i)(process\.env|import\.meta\.env|os\.getenv|os\.environ|System\.getenv)'
}

# Only scan files Git would publish: tracked files plus non-ignored untracked files.
$candidatePaths = @(
    git -C $repoRoot ls-files --cached --others --exclude-standard
)

$configAssignmentPattern = '(?i)^\s*[#-]*\s*["'']?([A-Za-z0-9_.-]*(?:password|passwd|pwd|api[-_]?key|api[-_]?secret|access[-_]?key(?:[-_]?id|[-_]?secret)?|secret|auth[-_]?token|app[-_]?id)[A-Za-z0-9_.-]*)["'']?\s*[:=]\s*["'']?(.*?)["'']?[,]?\s*$'
$codeLiteralPattern = '(?i)\b(apiKey|apiSecret|accessKeyId|accessKeySecret|authToken|password|secret)\s*[=:]\s*["'']([^"'']{8,})["'']'
$dictionaryLiteralPattern = '(?i)["''](password|passwd|pwd|api[-_]?key|api[-_]?secret|access[-_]?key(?:[-_]?id|[-_]?secret)?|secret|auth[-_]?token|cookie)["'']\s*:\s*["'']([^"'']+)["'']'
$cookieLiteralPattern = '(?i)\b[A-Za-z0-9_]*cookie\s*=\s*["'']([^"'']{20,})["'']'

foreach ($relativePath in $candidatePaths) {
    $fullPath = Join-Path $repoRoot $relativePath
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        continue
    }

    $file = Get-Item -LiteralPath $fullPath
    if ($file.Length -ge 2MB) {
        continue
    }

    $extension = $file.Extension.ToLowerInvariant()
    if ($supportedExtensions -notcontains $extension -and $file.Name -notlike ".env*") {
        continue
    }

    $isConfigFile = $extension -in @(".yml", ".yaml", ".properties", ".env", ".json") `
        -or $file.Name -like ".env*" `
        -or $file.Name -like "*.env"
    $lineNumber = 0

    foreach ($rawLine in Get-Content -LiteralPath $fullPath -ErrorAction SilentlyContinue) {
        $lineNumber++

        if ($isConfigFile) {
            $configMatch = [regex]::Match($rawLine.Trim(), $configAssignmentPattern)
            if ($configMatch.Success) {
                $key = $configMatch.Groups[1].Value
                $value = $configMatch.Groups[2].Value
                $isDurationSetting = $key -match '(?i)(ttl|timeout|max[-_.]?age)$'
                if (-not $isDurationSetting -and -not (Test-PlaceholderValue $value)) {
                    Add-SecretFinding $relativePath $lineNumber "literal-config-value" $key
                }
            }
        }

        foreach ($pattern in @(
            @{ Regex = $codeLiteralPattern; Kind = "literal-code-value" },
            @{ Regex = $dictionaryLiteralPattern; Kind = "literal-dictionary-value" }
        )) {
            $match = [regex]::Match($rawLine, $pattern.Regex)
            if ($match.Success -and -not (Test-PlaceholderValue $match.Groups[2].Value)) {
                Add-SecretFinding $relativePath $lineNumber $pattern.Kind $match.Groups[1].Value
            }
        }

        $cookieMatch = [regex]::Match($rawLine, $cookieLiteralPattern)
        if ($cookieMatch.Success -and -not (Test-PlaceholderValue $cookieMatch.Groups[1].Value)) {
            Add-SecretFinding $relativePath $lineNumber "literal-cookie" "cookie"
        }

        foreach ($knownPattern in @(
            @{ Regex = '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'; Kind = "private-key" },
            @{ Regex = '\b(gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\b'; Kind = "github-token" },
            @{ Regex = '\bLTAI[A-Za-z0-9]{12,}\b'; Kind = "aliyun-access-key-id" },
            @{ Regex = '\bsk-[A-Za-z0-9_-]{20,}\b'; Kind = "openai-style-key" },
            @{ Regex = '\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b'; Kind = "jwt" },
            @{ Regex = '://[^/\s:@]+:[^/\s@]+@'; Kind = "credential-in-url" }
        )) {
            if ($rawLine -match $knownPattern.Regex) {
                Add-SecretFinding $relativePath $lineNumber $knownPattern.Kind $knownPattern.Kind
            }
        }
    }
}

$uniqueFindings = @($findings | Sort-Object Path, Line, Kind, Key -Unique)
if ($uniqueFindings.Count -eq 0) {
    Write-Host "Secret scan passed: no literal credentials were detected in publishable files." -ForegroundColor Green
    exit 0
}

Write-Host "Secret scan found $($uniqueFindings.Count) potential credential(s)." -ForegroundColor Red
Write-Host "Values are intentionally omitted." -ForegroundColor Yellow
$uniqueFindings | Format-Table Path, Line, Kind, Key -AutoSize

if ($ReportOnly) {
    exit 0
}

exit 1
