# Cross-instance verification for the Redis migration.
# Checks: shared captcha, shared token revocation blacklist.
#
# Usage: powershell -ExecutionPolicy Bypass -File .\scripts\verify-redis-two-instance.ps1
# Prerequisite: two instances running on 8820 / 8821, sharing one Redis and one AUTH_TOKEN_SECRET.
#
# NOTE: keep this file pure ASCII. Windows PowerShell 5.1 reads .ps1 files as ANSI unless a BOM
# is present, so non-ASCII text here becomes mojibake and breaks the parser.

param(
    [string]$UrlA = 'http://127.0.0.1:8820',
    [string]$UrlB = 'http://127.0.0.1:8821',
    [string]$Account = '123456',
    [string]$Password = '1234567',
    [string]$RedisCli = 'C:\Program Files\Redis\redis-cli.exe',
    [string]$RedisHost = '127.0.0.1',
    [int]$RedisPort = 6379
)

$ErrorActionPreference = 'Continue'
$jar = Join-Path $env:TEMP 'verify-redis.jar'
$hdr = Join-Path $env:TEMP 'verify-redis.hdr'
$body = Join-Path $env:TEMP 'verify-redis.body'
Remove-Item $jar, $hdr, $body -ErrorAction SilentlyContinue

$script:failed = 0

function Read-Code($raw) {
    [regex]::Match(($raw -join ''), '"code"\s*:\s*(\d+)').Groups[1].Value
}

function Assert-Code($label, $raw, $expect, $hint) {
    $code = Read-Code $raw
    if ($code -eq $expect) {
        Write-Host "  [PASS] $label (code=$code)" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $label expected code=$expect actual=$code" -ForegroundColor Red
        Write-Host "         raw: $(($raw -join ' ').Trim())" -ForegroundColor DarkGray
        if ($hint) { Write-Host "         hint: $hint" -ForegroundColor DarkYellow }
        $script:failed++
    }
}

function Read-Token($path) {
    if (-not (Test-Path $path)) { return '' }
    foreach ($line in (Get-Content $path)) {
        if ($line -match '^\s*X-Auth-Token:\s*(\S+)') { return $Matches[1] }
    }
    return ''
}

Write-Host ""
Write-Host "=== 0) Preconditions ===" -ForegroundColor Cyan
foreach ($url in @($UrlA, $UrlB)) {
    $stamp = curl.exe -s -D - -o NUL "$url/api/user/captcha" 2>$null | Select-String 'X-Build-Stamp'
    if ($stamp) {
        Write-Host "  [PASS] $url alive, $($stamp.ToString().Trim())" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $url has no X-Build-Stamp: not running, or still the old build" -ForegroundColor Red
        $script:failed++
    }
}
if ($script:failed -gt 0) {
    Write-Host ""
    Write-Host "Preconditions failed, aborting." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 1) Captcha across instances: get on A, log in on B ===" -ForegroundColor Cyan
$cap = (curl.exe -s -c $jar "$UrlA/api/user/captcha" | ConvertFrom-Json).data
Write-Host "  captchaId=$($cap.captchaId)" -ForegroundColor Gray
[ordered]@{
    userAccount = $Account
    userPassword = $Password
    captchaId = $cap.captchaId
    captchaCode = $cap.captchaCode
} | ConvertTo-Json -Compress | Set-Content $body -Encoding ascii -NoNewline

# Headers go to a file; the body stays on stdout so Assert-Code can parse it.
$loginOut = curl.exe -s -D $hdr -b $jar -H 'Content-Type: application/json' --data-binary "@$body" "$UrlB/api/user/login/captcha"
Assert-Code 'captcha login across instances' $loginOut 0 'binding mismatch usually means the two instances use different AUTH_TOKEN_SECRET'

$token = Read-Token $hdr
if (-not $token) {
    Write-Host "  no X-Auth-Token returned, cannot continue" -ForegroundColor Red
    exit 1
}
Write-Host "  token length=$($token.Length)" -ForegroundColor Gray

Write-Host ""
Write-Host "=== 2) Token is accepted by A ===" -ForegroundColor Cyan
Assert-Code 'token valid on the other instance' (curl.exe -s -H "Authorization: Bearer $token" "$UrlA/api/user/get/login") 0 ''

Write-Host ""
Write-Host "=== 3) Log out on B (revokes the token) ===" -ForegroundColor Cyan
Assert-Code 'logout' (curl.exe -s -b $jar -H "Authorization: Bearer $token" -X POST "$UrlB/api/user/logout") 0 ''

Write-Host ""
Write-Host "=== 4) Same token against A must now be rejected ===" -ForegroundColor Cyan
Assert-Code 'token revoked across instances' (curl.exe -s -H "Authorization: Bearer $token" "$UrlA/api/user/get/login") 40100 'if this passes, the blacklist is not shared'

Write-Host ""
Write-Host "=== 5) Blacklist keys in Redis ===" -ForegroundColor Cyan
$keys = @()
if (Test-Path $RedisCli) {
    $keys = & $RedisCli -h $RedisHost -p $RedisPort --scan --pattern 'teach:auth:revoked:*' 2>$null
} else {
    Write-Host "  redis-cli not found at $RedisCli, trying via docker" -ForegroundColor DarkYellow
    $keys = docker compose exec -T redis redis-cli -h host.docker.internal -p $RedisPort --scan --pattern 'teach:auth:revoked:*' 2>$null
}
if ($keys) {
    Write-Host "  [PASS] found $($keys.Count) revoked key(s)" -ForegroundColor Green
    $keys | Select-Object -First 5 | ForEach-Object { Write-Host "         $_" -ForegroundColor Gray }
} else {
    Write-Host "  [FAIL] no teach:auth:revoked:* found" -ForegroundColor Red
    Write-Host "         check you are querying the Redis the app actually connects to" -ForegroundColor DarkYellow
    $script:failed++
}

Write-Host ""
if ($script:failed -eq 0) {
    Write-Host "ALL CHECKS PASSED" -ForegroundColor Green
    exit 0
}
Write-Host "$script:failed CHECK(S) FAILED" -ForegroundColor Red
exit 1
