@echo off
echo ==========================================
echo    Baseline Profile Generator
echo ==========================================
echo.
echo Step 1: Stopping Gradle daemons...
call gradlew.bat --stop

echo.
echo Step 2: Checking for connected devices...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" devices

echo.
echo Step 3: Make sure you have a device connected, then press any key to continue...
pause

echo.
echo Step 4: Running baseline profile generation on connected device...
echo This may take a few minutes...
call gradlew.bat :baselineprofile:connectedNonMinifiedReleaseAndroidTest

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Baseline profile generation failed!
    echo Make sure:
    echo   1. A physical device is connected via USB
    echo   2. USB debugging is enabled
    echo   3. The device is unlocked
    pause
    exit /b 1
)

echo.
echo Step 5: Copying baseline profile to source...
call gradlew.bat :app:copyReleaseBaselineProfileIntoSrc

echo.
echo Step 6: Building release APK with baseline profile...
call gradlew.bat :app:assembleRelease

echo.
echo ==========================================
echo    DONE! Baseline profile generated.
echo ==========================================
echo.
echo The release APK is at:
echo   app\build\outputs\apk\release\app-release.apk
echo.
echo Or build AAB for Play Store:
echo   gradlew.bat :app:bundleRelease
echo.
pause

