@echo off
echo ========================================
echo   Ngrok Status Checker
echo ========================================
echo.

REM Check if Spring Boot app is running on port 8081
echo [1] Checking if Spring Boot app is running on port 8081...
netstat -ano | findstr :8081 >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Port 8081 is in use - Spring Boot app appears to be running
) else (
    echo [ERROR] Port 8081 is not in use - Spring Boot app is NOT running
    echo.
    echo Please start the Spring Boot application first:
    echo   mvn spring-boot:run
    echo.
)

echo.
echo [2] Checking if ngrok is running...
tasklist | findstr /i "ngrok.exe" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] ngrok.exe process is running
) else (
    echo [ERROR] ngrok.exe is NOT running
    echo.
    echo Please start ngrok:
    echo   ngrok http 8081
    echo   or run: start-ngrok.bat
    echo.
)

echo.
echo [3] Checking ngrok web interface (http://localhost:4040)...
curl -s http://localhost:4040/api/tunnels >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] ngrok web interface is accessible
    echo.
    echo You can check ngrok status at: http://localhost:4040
) else (
    echo [WARNING] Cannot access ngrok web interface
    echo This might mean ngrok is not running or not configured
)

echo.
echo ========================================
echo   Troubleshooting Steps:
echo ========================================
echo 1. Make sure Spring Boot app is running: mvn spring-boot:run
echo 2. Make sure ngrok is running: ngrok http 8081
echo 3. Check ngrok status: http://localhost:4040
echo 4. Restart both if needed
echo.
pause

