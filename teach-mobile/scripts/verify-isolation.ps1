$ErrorActionPreference = 'Stop'

function Invoke-Step {
  param(
    [string]$Name,
    [string]$Command,
    [string]$WorkingDirectory
  )

  Write-Host ''
  Write-Host "==> $Name"
  Push-Location $WorkingDirectory
  try {
    cmd /c $Command
    if ($LASTEXITCODE -ne 0) {
      throw "$Name failed with exit code $LASTEXITCODE"
    }
  } finally {
    Pop-Location
  }
}

function Get-GitStatus {
  param([string]$Pathspec)
  $output = git status --short -- $Pathspec
  if ($LASTEXITCODE -ne 0) {
    throw "git status failed for $Pathspec"
  }
  return ($output -join "`n").Trim()
}

$mobileRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $mobileRoot
$webRoot = Join-Path $repoRoot 'teach-frontend'

Write-Host 'SmartEdu mobile isolation verification'
Write-Host "Repo: $repoRoot"

if (-not (Test-Path $webRoot)) {
  throw "Web frontend not found: $webRoot"
}

$webStatusBefore = Get-GitStatus 'teach-frontend'

Invoke-Step 'Mobile source boundary check' 'npm run check:boundary' $mobileRoot
Invoke-Step 'Mobile render smoke' 'npm run smoke:render' $mobileRoot
Invoke-Step 'Capacitor Android sync' 'npm run android:sync' $mobileRoot
Invoke-Step 'Mobile generated boundary check' 'npm run check:boundary:generated' $mobileRoot
Invoke-Step 'Web frontend build' 'npm run build' $webRoot

$webStatusAfter = Get-GitStatus 'teach-frontend'

Write-Host ''
if ($webStatusBefore -eq $webStatusAfter) {
  Write-Host '[OK] teach-frontend git status is unchanged by this verification run.'
} else {
  Write-Host '[!!] teach-frontend git status changed during verification.'
  Write-Host ''
  Write-Host 'Before:'
  if ($webStatusBefore) { Write-Host $webStatusBefore } else { Write-Host '<clean>' }
  Write-Host ''
  Write-Host 'After:'
  if ($webStatusAfter) { Write-Host $webStatusAfter } else { Write-Host '<clean>' }
  exit 1
}

Write-Host '[OK] Mobile build, Android sync, and Web build completed.'
