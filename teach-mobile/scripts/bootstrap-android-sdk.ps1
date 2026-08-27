param(
  [string]$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk",
  [switch]$AcceptAndroidSdkLicense,
  [switch]$InstallPackages,
  [switch]$WriteLocalProperties
)

$ErrorActionPreference = 'Stop'

$commandLineToolsUrl = 'https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip'
$projectRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $projectRoot 'android'
$localProperties = Join-Path $androidRoot 'local.properties'
$cmdlineToolsRoot = Join-Path $SdkRoot 'cmdline-tools'
$latestRoot = Join-Path $cmdlineToolsRoot 'latest'
$sdkManager = Join-Path $latestRoot 'bin\sdkmanager.bat'

function Write-Info {
  param([string]$Message)
  Write-Host "[android-bootstrap] $Message"
}

function Ensure-Directory {
  param([string]$Path)
  if (-not (Test-Path $Path)) {
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
  }
}

Write-Info "SDK root: $SdkRoot"

if (-not (Test-Path $sdkManager)) {
  if (-not $AcceptAndroidSdkLicense) {
    Write-Host ''
    Write-Host 'Android command-line tools are not installed.'
    Write-Host 'Downloading them requires accepting the Android SDK License.'
    Write-Host ''
    Write-Host 'Run this only after you have reviewed and accepted the license terms:'
    Write-Host "powershell -ExecutionPolicy Bypass -File scripts/bootstrap-android-sdk.ps1 -AcceptAndroidSdkLicense"
    Write-Host ''
    Write-Host 'Official download page: https://developer.android.com/studio'
    exit 0
  }

  $tmpRoot = Join-Path ([System.IO.Path]::GetTempPath()) "smartedu-android-sdk-$([System.Guid]::NewGuid().ToString('N'))"
  $zipPath = Join-Path $tmpRoot 'commandlinetools-win_latest.zip'
  $extractRoot = Join-Path $tmpRoot 'extract'

  Ensure-Directory $tmpRoot
  Ensure-Directory $extractRoot
  Ensure-Directory $cmdlineToolsRoot

  Write-Info 'Downloading Android command-line tools...'
  Invoke-WebRequest -Uri $commandLineToolsUrl -OutFile $zipPath -TimeoutSec 300

  Write-Info 'Extracting Android command-line tools...'
  Expand-Archive -LiteralPath $zipPath -DestinationPath $extractRoot -Force

  if (Test-Path $latestRoot) {
    Remove-Item -LiteralPath $latestRoot -Recurse -Force
  }
  New-Item -ItemType Directory -Force -Path $latestRoot | Out-Null

  $extractedTools = Join-Path $extractRoot 'cmdline-tools'
  if (-not (Test-Path $extractedTools)) {
    throw "Unexpected archive layout: $extractedTools not found"
  }

  Get-ChildItem -LiteralPath $extractedTools -Force | Move-Item -Destination $latestRoot
  Remove-Item -LiteralPath $tmpRoot -Recurse -Force
  Write-Info "Installed command-line tools: $latestRoot"
}

if ($WriteLocalProperties) {
  Ensure-Directory $androidRoot
  $escapedSdkRoot = $SdkRoot.Replace('\', '\\')
  Set-Content -LiteralPath $localProperties -Value "sdk.dir=$escapedSdkRoot" -Encoding UTF8
  Write-Info "Wrote local SDK path: $localProperties"
}

if ($InstallPackages) {
  if (-not (Test-Path $sdkManager)) {
    throw "sdkmanager not found: $sdkManager"
  }

  Write-Host ''
  Write-Host 'sdkmanager may ask you to accept Android SDK package licenses.'
  Write-Host 'Install set: platform-tools, platforms;android-35, build-tools;35.0.0'
  Write-Host ''

  & $sdkManager --sdk_root=$SdkRoot 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
  if ($LASTEXITCODE -ne 0) {
    throw "sdkmanager failed with exit code $LASTEXITCODE"
  }
}

Write-Host ''
Write-Host 'Next commands for this PowerShell session:'
Write-Host "`$env:ANDROID_HOME='$SdkRoot'"
Write-Host "`$env:ANDROID_SDK_ROOT='$SdkRoot'"
Write-Host "`$env:Path='$SdkRoot\platform-tools;$latestRoot\bin;' + `$env:Path"
Write-Host 'npm run android:doctor'
Write-Host 'npm run android:debug'
