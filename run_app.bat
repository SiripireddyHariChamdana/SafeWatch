@echo off
title SafeWatch - Starting...
echo.
echo  ============================================
echo   SafeWatch - Personal Safety Monitor
echo  ============================================
echo.
echo  [*] Starting server, please wait...
echo.

REM Open browser after a short delay (server needs time to start)
start "" cmd /c "timeout /t 3 /nobreak >nul && start http://localhost:8000"

if exist venv\Scripts\python.exe (
    echo  [*] Using Virtual Environment Python...
    venv\Scripts\python.exe run.py
) else (
    echo  [*] Using System Python...
    python run.py
)

pause
