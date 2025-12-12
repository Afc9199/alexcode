#!/bin/bash

echo "========================================"
echo "  TheLorry Employee Management System"
echo "  Ngrok Tunnel Starter"
echo "========================================"
echo ""

# Check if ngrok is installed
if ! command -v ngrok &> /dev/null; then
    echo "[ERROR] ngrok is not installed or not in PATH"
    echo ""
    echo "Please install ngrok:"
    echo "1. Download from https://ngrok.com/download"
    echo "2. Extract ngrok to a folder in your PATH"
    echo "3. Or install via package manager:"
    echo "   - macOS: brew install ngrok/ngrok/ngrok"
    echo "   - Linux: Download from ngrok.com"
    echo ""
    exit 1
fi

echo "[INFO] Starting ngrok tunnel on port 8081..."
echo "[INFO] Make sure your Spring Boot app is running on port 8081"
echo ""
echo "Press Ctrl+C to stop ngrok"
echo ""

ngrok http 8081

