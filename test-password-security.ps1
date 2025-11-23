# Password Security Test Script
# Tests connected devices

Write-Host "=== Password Security Test ===" -ForegroundColor Cyan
Write-Host ""

# Get list of devices
$devices = adb devices | Select-String -Pattern "device$" | ForEach-Object { ($_ -split "\s+")[0] }

if ($devices.Count -eq 0) {
    Write-Host "❌ No devices connected!" -ForegroundColor Red
    exit
}

Write-Host "Found $($devices.Count) device(s):" -ForegroundColor Yellow
for ($i = 0; $i -lt $devices.Count; $i++) {
    Write-Host "  [$($i+1)] $($devices[$i])" -ForegroundColor White
}

Write-Host ""
Write-Host "Select option:" -ForegroundColor Cyan
Write-Host "  [A] Test ALL devices" -ForegroundColor White
Write-Host "  [1-$($devices.Count)] Test specific device" -ForegroundColor White
Write-Host "  [Q] Quit" -ForegroundColor White
Write-Host ""
$choice = Read-Host "Enter choice"

if ($choice -eq "Q" -or $choice -eq "q") {
    exit
}

$devicesToTest = @()
if ($choice -eq "A" -or $choice -eq "a") {
    $devicesToTest = $devices
} elseif ([int]::TryParse($choice, [ref]$null)) {
    $index = [int]$choice - 1
    if ($index -ge 0 -and $index -lt $devices.Count) {
        $devicesToTest = @($devices[$index])
    } else {
        Write-Host "❌ Invalid device number!" -ForegroundColor Red
        exit
    }
} else {
    Write-Host "❌ Invalid choice!" -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "Testing device(s)..." -ForegroundColor Cyan
Write-Host ""

foreach ($device in $devicesToTest) {
    Write-Host "=== Device: $device ===" -ForegroundColor Yellow
    
    # Test 1: Check if password is stored
    Write-Host "Test 1: Checking if password is stored..." -ForegroundColor Cyan
    $result = adb -s $device shell run-as org.example.semscan cat /data/data/org.example.semscan/shared_prefs/semscan_prefs.xml 2>$null | Select-String -Pattern "password" -CaseSensitive:$false
    
    if ($result) {
        Write-Host "  ❌ FAIL: Password found in storage!" -ForegroundColor Red
        $passwordValue = $result -replace '.*<string name="saved_password">(.*)</string>.*', '$1'
        Write-Host "  Password value: $passwordValue" -ForegroundColor Red
        Write-Host "  ⚠️  Security fix is NOT active - need to rebuild app!" -ForegroundColor Yellow
    } else {
        Write-Host "  ✅ PASS: No password stored" -ForegroundColor Green
    }
    
    Write-Host ""
}

Write-Host "=== Test Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "If password was found:" -ForegroundColor Yellow
Write-Host "  1. Make sure you're on 'password_security' branch" -ForegroundColor White
Write-Host "  2. Rebuild: ./gradlew assembleDebug -x lintDebug" -ForegroundColor White
Write-Host "  3. Reinstall: adb -s DEVICE_ID install -r build/outputs/apk/debug/SemScan-debug.apk" -ForegroundColor White
Write-Host "  4. Login again and test" -ForegroundColor White

