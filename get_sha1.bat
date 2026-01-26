@echo off
echo ==========================================
echo    Debug SHA-1 Fingerprint Checker
echo ==========================================
echo.
echo This will show your debug keystore SHA-1 fingerprint.
echo You need to add this to Firebase Console if Google Sign-In isn't working.
echo.

set KEYSTORE=%USERPROFILE%\.android\debug.keystore

if not exist "%KEYSTORE%" (
    echo ERROR: Debug keystore not found at %KEYSTORE%
    echo This is created when you first run an app from Android Studio.
    pause
    exit /b 1
)

echo Debug keystore found at: %KEYSTORE%
echo.
echo Extracting SHA-1 fingerprint...
echo.

keytool -list -v -keystore "%KEYSTORE%" -alias androiddebugkey -storepass android -keypass android 2>nul | findstr "SHA1"

echo.
echo ==========================================
echo    Instructions:
echo ==========================================
echo.
echo 1. Copy the SHA-1 fingerprint shown above
echo 2. Go to Firebase Console (https://console.firebase.google.com)
echo 3. Select your project
echo 4. Go to Project Settings (gear icon)
echo 5. Scroll to "Your apps" section
echo 6. Find your Android app
echo 7. Click "Add fingerprint"
echo 8. Paste the SHA-1 and save
echo 9. Download the updated google-services.json
echo 10. Replace the one in app/ folder
echo 11. Rebuild the app
echo.
echo ==========================================
pause

