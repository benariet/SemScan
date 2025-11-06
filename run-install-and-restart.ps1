<# 
Run from your project root:
  .\run-install-and-restart.ps1
Optional: target a specific device:
  $env:ANDROID_SERIAL="emulator-5554"; .\run-install-and-restart.ps1
#>

$ErrorActionPreference = 'Stop'

# === Config ===
$PKG = 'org.example.semscan'           # <-- change if your package id is different
$ACTIVITY = '.MainActivity'            # <-- change if your launcher activity differs
$GRADLEW = '.\gradlew'                 # Path to gradlew in your project root

# Build ADB command (respect ANDROID_SERIAL if set)
$adb = 'adb'
if ($env:ANDROID_SERIAL) { $adb = "adb -s $($env:ANDROID_SERIAL)" }

Write-Host "==> Verifying adb is available..."
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
  throw "adb not found in PATH"
}

Write-Host "==> Connected devices:"
adb devices

Write-Host "==> Stopping app: $PKG"
try { & $adb shell am force-stop $PKG | Out-Null } catch { }

Write-Host "==> Installing debug build via Gradle"
& $GRADLEW installDebug

Write-Host "==> Launching $PKG/$ACTIVITY"
& $adb shell am start -n "$PKG/$ACTIVITY"

Write-Host "==> Done."
