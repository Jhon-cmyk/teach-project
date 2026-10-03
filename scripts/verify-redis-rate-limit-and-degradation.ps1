# Cross-instance verification part 2: AI rate limit sharing + Redis-down degradation.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\scripts\verify-redis-rate-limit-and-degradation.ps1
#
# What it does:
#   Phase 1  starts an ISOLATED Redis container, starts two app instances pointed at it,
#            and proves the AI rate limit quota is shared (4th request on the other instance is rejected).
#   Phase 2  stops that isolated Redis and checks the documented degradation matrix:
#              token auth        -> fail-closed (40100)
#              captcha login     -> still works on the same instance (local session fallback)
#              AI rate limiting  -> fail-open (request is not rejected by the limiter)
#            then restores everything.
#
# Why an isolated Redis: the normal way to test degradation is to stop the Redis the app uses.
# On this machine that is a Windows service ("Redis" on 127.0.0.1:6379), which cannot be stopped
# without elevation, and stopping it would disturb other work. An isolated instance on its own
# port gives the same signal with no privileges and no side effects.
#
# Requirements: Docker running, the project built (mvn package), DEEPSEEK_API_KEY irrelevant.
# NOTE: keep this file pure ASCII (Windows PowerShell 5.1 reads .ps1 as ANSI without a BOM).

param(
    [string]$Jar = 'target\teach-0.0.1-SNAPSHOT.jar',
    [string]$Account = '123456',
    [string]$Password = '1234567',
    [int]$Limit = 3,
    [int]$PortA = 8840,
    [int]$PortB = 8841,
    [int]$RedisPort = 6380,
    [string]$RedisContainer = 'verify-redis-isolated',
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Continue'
$root = (Get-Location).Path
$jarPath = Join-Path $root $Jar
if (-not (Test-Path $jarPath)) {
    Write-Host "jar not found: $jarPath  (run: mvnw.cmd -DskipTests package)" -ForegroundColor Red
    exit 1
}

$tmpJar = Join-Path $env:TEMP 'deg.jar'
$tmpHdr = Join-Path $env:TEMP 'deg.hdr'
$tmpBody = Join-Path $env:TEMP 'deg.body'
$tmpAi = Join-Path $env:TEMP 'deg-ai.body'
Remove-Item $tmpJar, $tmpHdr, $tmpBody, $tmpAi -ErrorAction SilentlyContinue
'{"question":"rate limit probe","type":"chat"}' | Set-Content $tmpAi -Encoding ascii -NoNewline

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

function Assert-True($label, $condition, $hint) {
    if ($condition) {
        Write-Host "  [PASS] $label" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $label" -ForegroundColor Red
        if ($hint) { Write-Host "         hint: $hint" -ForegroundColor DarkYellow }
        $script:failed++
    }
}

function Wait-Healthy([int]$port) {
    $deadline = (Get-Date).AddMinutes(3)
    while ((Get-Date) -lt $deadline) {
        $r = curl.exe -s --max-time 2 "http://127.0.0.1:$port/api/actuator/health" 2>$null
        if ($r -match 'UP') { return $true }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Start-App([int]$port, [string]$uploadDir) {
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:AUTH_TOKEN_SECRET = 'verify-two-instance-secret'
    $env:AI_RATE_LIMIT_PER_MINUTE = "$Limit"
    $env:DEEPSEEK_API_KEY = 'sk-invalid-on-purpose'
    $env:REDIS_HOST = '127.0.0.1'
    $env:REDIS_PORT = "$RedisPort"
    $env:SERVER_PORT = "$port"
    $env:RUYI_UPLOAD_PATH = $uploadDir
    $env:RUYI_BACKUP_PATH = "$uploadDir/../backups$port/"
    return Start-Process -FilePath 'java' -ArgumentList @('-jar', $jarPath) `
        -WorkingDirectory $root -PassThru -WindowStyle Hidden
}

function Login([string]$url, [string]$jarFile) {
    $cap = (curl.exe -s -c $jarFile "$url/api/user/captcha" | ConvertFrom-Json).data
    [ordered]@{
        userAccount = $Account
        userPassword = $Password
        captchaId = $cap.captchaId
        captchaCode = $cap.captchaCode
    } | ConvertTo-Json -Compress | Set-Content $tmpBody -Encoding ascii -NoNewline
    $result = curl.exe -s -D $tmpHdr -b $jarFile -H 'Content-Type: application/json' `
        --data-binary "@$tmpBody" "$url/api/user/login/captcha"

    return $result
}

function Read-Token($path) {
    if (-not (Test-Path $path)) { return '' }
    foreach ($line in (Get-Content $path)) {
        if ($line -match '^\s*X-Auth-Token:\s*(\S+)') { return $Matches[1] }
    }
    return ''
}

$urlA = "http://127.0.0.1:$PortA"
$urlB = "http://127.0.0.1:$PortB"
$processes = @()

try {
    Write-Host ""
    Write-Host "=== 0) Start isolated Redis and two app instances ===" -ForegroundColor Cyan
    docker rm -f $RedisContainer 2>$null | Out-Null
    docker run -d --name $RedisContainer -p "${RedisPort}:6379" redis:7.4-alpine 2>$null | Out-Null
    Start-Sleep -Seconds 3
    $ping = docker exec $RedisContainer redis-cli PING 2>$null
    Assert-True "isolated Redis on $RedisPort is up" ("$ping".Trim() -eq 'PONG')

    $processes += Start-App $PortA './files-verify-a/'
    $processes += Start-App $PortB './files-verify-b/'
    Assert-True "instance A ($PortA) healthy" (Wait-Healthy $PortA)
    Assert-True "instance B ($PortB) healthy" (Wait-Healthy $PortB)
    if ($script:failed -gt 0) { throw "setup failed" }

    Write-Host ""
    Write-Host "=== 1) AI rate limit is shared across instances (limit=$Limit) ===" -ForegroundColor Cyan
    Login $urlA $tmpJar | Out-Null
    $token = Read-Token $tmpHdr
    Assert-True "logged in" ($token -ne '')

    $blockedAt = -1
    for ($i = 1; $i -le ($Limit + 1); $i++) {
        $url = if ($i % 2 -eq 1) { $urlA } else { $urlB }
        $raw = curl.exe -s -H "Authorization: Bearer $token" -H 'Content-Type: application/json' `
            --data-binary "@$tmpAi" "$url/api/ai/stream"
        $code = Read-Code $raw
        $shown = if ($code) { $code } else { 'stream/no-code' }
        Write-Host "  request $i -> $url code=$shown" -ForegroundColor Gray
        if ($code -eq '50001') { $blockedAt = $i; break }
    }
    Assert-True "request $($Limit + 1) rejected, on the OTHER instance" ($blockedAt -eq ($Limit + 1)) `
        'a per-instance limit would let request 4 through'

    $windowSize = (docker exec $RedisContainer redis-cli ZCARD 'teach:ratelimit:ai:5' 2>$null | Select-Object -First 1)
    Assert-True "Redis window holds $Limit entries (actual=$windowSize)" ("$windowSize".Trim() -eq "$Limit") `
        'the sliding window is a ZSET keyed by user id'

    Write-Host ""
    Write-Host "=== 2) Degradation while Redis is down ===" -ForegroundColor Cyan
    $preDownToken = Read-Token $tmpHdr
    docker exec $RedisContainer redis-cli DEL 'teach:ratelimit:ai:5' 2>$null | Out-Null

    docker stop $RedisContainer 2>$null | Out-Null
    Start-Sleep -Seconds 3
    $ping = docker exec $RedisContainer redis-cli PING 2>&1
    Assert-True "isolated Redis is really down" ($ping -notmatch 'PONG')

    $tokenResp = curl.exe -s -H "Authorization: Bearer $preDownToken" "$urlA/api/user/get/login"
    Assert-Code "token auth fails CLOSED" $tokenResp 40100 `
        'the revoked-token blacklist is a security switch: it must not silently pass when Redis is unreachable'

    $degradedJar = Join-Path $env:TEMP "deg-fresh.jar"
    Remove-Item $degradedJar -ErrorAction SilentlyContinue
    $sameInstance = Login $urlA $degradedJar
    Assert-Code "captcha login still works on the SAME instance" $sameInstance 0 `
        'degraded captcha answers fall back to the per-instance session'

    $aiRaw = curl.exe -s -b $tmpJar -H 'Content-Type: application/json' --data-binary "@$tmpAi" "$urlA/api/ai/stream"
    $aiCode = Read-Code $aiRaw
    Assert-True "AI rate limit fails OPEN (not blocked)" ($aiCode -ne '50001') `
        'cost control must not take the whole AI surface down during a Redis blip'

    docker start $RedisContainer 2>$null | Out-Null
} finally {
    foreach ($p in $processes) {
        if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    }
    if (-not $KeepRunning) {
        docker start $RedisContainer 2>$null | Out-Null
        docker rm -f $RedisContainer 2>$null | Out-Null
    }
    Remove-Item $tmpJar, $tmpHdr, $tmpBody, $tmpAi -ErrorAction SilentlyContinue
}

Write-Host ""
if ($script:failed -eq 0) {
    Write-Host "ALL CHECKS PASSED" -ForegroundColor Green
    exit 0
}
Write-Host "$script:failed CHECK(S) FAILED" -ForegroundColor Red
exit 1





