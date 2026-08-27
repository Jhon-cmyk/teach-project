$ErrorActionPreference = 'Stop'

function Write-Check {
  param(
    [string]$Name,
    [bool]$Ok,
    [string]$Detail = ''
  )

  $mark = if ($Ok) { '[OK]' } else { '[!!]' }
  if ($Detail) {
    Write-Host "$mark $Name - $Detail"
  } else {
    Write-Host "$mark $Name"
  }
}

function Get-CommandPath {
  param([string]$Command)
  $cmd = Get-Command $Command -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  return $null
}

function Get-JavaVersion {
  param([string]$JavaExe)
  if (-not $JavaExe -or -not (Test-Path $JavaExe)) { return $null }
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    $line = & $JavaExe -version 2>&1 | Select-Object -First 1
    return [string]$line
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $projectRoot 'android'
$localProperties = Join-Path $androidRoot 'local.properties'
$gradleWrapper = Join-Path $androidRoot 'gradlew.bat'
$apkPath = Join-Path $androidRoot 'app\build\outputs\apk\debug\app-debug.apk'

Write-Host 'SmartEdu Student Android environment check'
Write-Host ''

$javaPath = Get-CommandPath 'java'
Write-Check 'java on PATH' ([bool]$javaPath) $(if ($javaPath) { "$javaPath; $(Get-JavaVersion $javaPath)" } else { '' })
Write-Check 'JAVA_HOME' ([bool]$env:JAVA_HOME) $env:JAVA_HOME

$javaHomeExe = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { $null }
$javaHomeVersion = Get-JavaVersion $javaHomeExe
$javaHomeIsModern = $javaHomeVersion -match 'version "(1[7-9]|[2-9][0-9])\.'
Write-Check 'JAVA_HOME Java 17+' $javaHomeIsModern $(if ($javaHomeVersion) { $javaHomeVersion } else { 'set JAVA_HOME to JDK 17 or newer' })

$sdkCandidates = @()
if ($env:ANDROID_HOME) { $sdkCandidates += $env:ANDROID_HOME }
if ($env:ANDROID_SDK_ROOT) { $sdkCandidates += $env:ANDROID_SDK_ROOT }
if (Test-Path $localProperties) {
  $sdkLine = Get-Content $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
  if ($sdkLine) {
    $sdkCandidates += ($sdkLine -replace '^sdk\.dir=', '').Replace('\\', '\')
  }
}

$validSdk = $sdkCandidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
Write-Check 'Android SDK' ([bool]$validSdk) $(if ($validSdk) { $validSdk } else { 'set ANDROID_HOME or android/local.properties sdk.dir' })

$sdkManager = Get-CommandPath 'sdkmanager'
if (-not $sdkManager -and $validSdk) {
  $sdkManagerCandidate = Join-Path $validSdk 'cmdline-tools\latest\bin\sdkmanager.bat'
  if (Test-Path $sdkManagerCandidate) { $sdkManager = $sdkManagerCandidate }
}
Write-Check 'sdkmanager' ([bool]$sdkManager) $(if ($sdkManager) { $sdkManager } else { 'install Android Studio command-line tools' })

$adb = Get-CommandPath 'adb'
if (-not $adb -and $validSdk) {
  $adbCandidate = Join-Path $validSdk 'platform-tools\adb.exe'
  if (Test-Path $adbCandidate) { $adb = $adbCandidate }
}
Write-Check 'adb' ([bool]$adb) $(if ($adb) { $adb } else { 'install Android SDK platform-tools' })

Write-Check 'Gradle wrapper' (Test-Path $gradleWrapper) $gradleWrapper
Write-Check 'Debug APK' (Test-Path $apkPath) $apkPath

Write-Host ''
if (-not $validSdk) {
  Write-Host 'Next step: install Android Studio, or run the guarded command-line bootstrap after accepting Android SDK License:'
  Write-Host 'powershell -ExecutionPolicy Bypass -File scripts/bootstrap-android-sdk.ps1 -AcceptAndroidSdkLicense -InstallPackages -WriteLocalProperties'
  Write-Host ''
  Write-Host 'If Android Studio is already installed, set ANDROID_HOME or create android/local.properties with:'
  Write-Host 'sdk.dir=C:\Users\<you>\AppData\Local\Android\Sdk'
}
