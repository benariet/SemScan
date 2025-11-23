# Simple Password Security Test
# Shows device list and lets you test specific device

Write-Host "=== Connected Devices ===" -ForegroundColor Cyan
Write-Host ""

# Get list of devices
$devices = adb devices | Select-String -Pattern "device$" | ForEach-Object { ($_ -split "\s+")[0] }

if ($devices.Count -eq 0) {
    Write-Host "❌ No devices connected!" -ForegroundColor Red
    exit
}

# Display devices
for ($i = 0; $i -lt $devices.Count; $i++) {
    Write-Host "[$($i+1)] $($devices[$i])" -ForegroundColor White
}

Write-Host ""
$deviceNum = Read-Host "Enter device number (1-$($devices.Count))"

$index = [int]$deviceNum - 1
if ($index -lt 0 -or $index -ge $devices.Count) {
    Write-Host "❌ Invalid device number!" -ForegroundColor Red
    exit
}

$selectedDevice = $devices[$index]
Write-Host ""
Write-Host "Testing device: $selectedDevice" -ForegroundColor Yellow
Write-Host ""

# Test password storage
Write-Host "Checking if password is stored..." -ForegroundColor Cyan
$result = adb -s $selectedDevice shell run-as org.example.semscan cat /data/data/org.example.semscan/shared_prefs/semscan_prefs.xml 2>$null | Select-String -Pattern "password" -CaseSensitive:$false

if ($result) {
    Write-Host "❌ FAIL: Password found in storage!" -ForegroundColor Red
    $passwordValue = $result -replace '.*<string name="saved_password">(.*)</string>.*', '$1'
    Write-Host "Password: $passwordValue" -ForegroundColor Red
    Write-Host ""
    Write-Host "⚠️  Security fix NOT active!" -ForegroundColor Yellow
    Write-Host "Rebuild and reinstall:" -ForegroundColor Yellow
    Write-Host "  ./gradlew assembleDebug -x lintDebug" -ForegroundColor White
    Write-Host "  adb -s $selectedDevice install -r build/outputs/apk/debug/SemScan-debug.apk" -ForegroundColor White
} else {
    Write-Host "✅ PASS: No password stored" -ForegroundColor Green
    Write-Host "Security fix is working!" -ForegroundColor Green
}


