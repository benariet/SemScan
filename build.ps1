# === Build and install APK to all connected devices ===
# Simple script - just run: .\build.ps1

$Wrapper = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { ".\gradlew" }

# 1) Build APK
Write-Host "Building APK..." -ForegroundColor Green
& $Wrapper assembleDebug -x lintDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }

# 2) Find built APKs
$apks = Get-ChildItem -Recurse "build\outputs\apk" -Filter "*-debug.apk" | Sort-Object LastWriteTime -Descending
if (-not $apks) { throw "No APKs found after build" }
Write-Host "Found APK: $($apks[0].Name)" -ForegroundColor Green

# 3) Devices
$devs = (adb devices) | Select-String "`tdevice$" | ForEach-Object { $_.Line.Split("`t")[0] }
if (-not $devs) { throw "No devices found. Enable USB debugging." }
Write-Host "Installing to: $($devs -join ', ')" -ForegroundColor Green

# 4) Install
foreach ($d in $devs) {
  Write-Host "→ Installing on $d ..." -ForegroundColor Yellow
  adb -s $d install -r -d "$($apks[0].FullName)" | Out-Null
}
Write-Host "✅ Done!" -ForegroundColor Green
