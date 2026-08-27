[CmdletBinding()]
param(
    [switch]$SkipBackend,
    [switch]$SkipWeb,
    [switch]$SkipMobile,
    [switch]$SkipAi,
    [switch]$ContinueOnError
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$results = [System.Collections.Generic.List[object]]::new()
$hasFailure = $false

function Invoke-VerificationStep {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,

        [Parameter(Mandatory = $true)]
        [scriptblock]$Action
    )

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $status = "PASSED"
    $message = ""

    Write-Host ""
    Write-Host "==> $Name" -ForegroundColor Cyan

    Push-Location $WorkingDirectory
    try {
        & $Action
        if ($LASTEXITCODE -ne 0) {
            throw "Command exited with code $LASTEXITCODE."
        }
    }
    catch {
        $status = "FAILED"
        $message = $_.Exception.Message
        $script:hasFailure = $true
        Write-Host "FAILED: $message" -ForegroundColor Red
    }
    finally {
        Pop-Location
        $stopwatch.Stop()
    }

    $script:results.Add([PSCustomObject]@{
        Step = $Name
        Status = $status
        DurationSeconds = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 2)
        Message = $message
    })

    if ($status -eq "FAILED" -and -not $ContinueOnError) {
        Write-Host ""
        Write-Host "Verification stopped after the first failure." -ForegroundColor Red
        $script:results | Format-Table -AutoSize
        exit 1
    }
}

if (-not $SkipBackend) {
    Invoke-VerificationStep `
        -Name "Java backend tests" `
        -WorkingDirectory $repoRoot `
        -Action {
            & "$repoRoot\mvnw.cmd" test
        }
}

if (-not $SkipWeb) {
    Invoke-VerificationStep `
        -Name "Web type-check and production build" `
        -WorkingDirectory (Join-Path $repoRoot "teach-frontend") `
        -Action {
            npm run build
        }
}

if (-not $SkipMobile) {
    Invoke-VerificationStep `
        -Name "Mobile type-check and production build" `
        -WorkingDirectory (Join-Path $repoRoot "teach-mobile") `
        -Action {
            npm run build
        }

    Invoke-VerificationStep `
        -Name "Mobile source boundary check" `
        -WorkingDirectory (Join-Path $repoRoot "teach-mobile") `
        -Action {
            npm run check:boundary
        }
}

if (-not $SkipAi) {
    Invoke-VerificationStep `
        -Name "Python AI service syntax check" `
        -WorkingDirectory $repoRoot `
        -Action {
            $python = Get-Command python -ErrorAction SilentlyContinue
            if ($null -eq $python) {
                throw "Python executable was not found on PATH."
            }

            $aiRoot = Join-Path $repoRoot "teach-ai-server"
            $pythonFiles = @(
                Get-ChildItem -Path $aiRoot -Recurse -File -Filter "*.py" |
                    Where-Object {
                        $_.FullName -notmatch "[\\/](venv|__pycache__|\.pytest_cache)[\\/]"
                    } |
                    Select-Object -ExpandProperty FullName
            )

            if ($pythonFiles.Count -eq 0) {
                throw "No Python source files were found under teach-ai-server."
            }

            & $python.Source -m py_compile @pythonFiles
        }

    Invoke-VerificationStep `
        -Name "Python Agent tests and fixed evaluation" `
        -WorkingDirectory (Join-Path $repoRoot "teach-ai-server") `
        -Action {
            $venvPython = Join-Path $repoRoot "teach-ai-server\venv\Scripts\python.exe"
            $pythonCommand = if (Test-Path $venvPython) {
                $venvPython
            }
            else {
                (Get-Command python -ErrorAction Stop).Source
            }
            & $pythonCommand -m unittest discover -s tests -p "test_*.py"
            if ($LASTEXITCODE -ne 0) {
                throw "Python Agent tests failed with code $LASTEXITCODE."
            }
            & $pythonCommand -m evaluation.runner --check
        }
}

Write-Host ""
Write-Host "Verification summary" -ForegroundColor Cyan
$results | Format-Table -AutoSize

if ($hasFailure) {
    exit 1
}

Write-Host "All selected verification steps passed." -ForegroundColor Green
exit 0
