@echo off
echo ========================================
echo   TheLorry Employee Management System
echo   Ngrok Tunnel Starter
echo ========================================
echo.

REM Check if ngrok is installed
where ngrok >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] ngrok is not installed or not in PATH
    echo.
    echo Please install ngrok:
    echo 1. Download from https://ngrok.com/download
    echo 2. Extract ngrok.exe to a folder in your PATH
    echo 3. Or add ngrok.exe to this directory
    echo.
    pause
    exit /b 1
)

echo [INFO] Starting ngrok tunnel on port 8081...
echo [INFO] Make sure your Spring Boot app is running on port 8081
echo.
echo Press Ctrl+C to stop ngrok
echo.

ngrok http 8081

pause

