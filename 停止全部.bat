@echo off
title TianGong ERP - Stop All

echo Stopping backend(48080) and frontend(5173)...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":48080 :5173"') do (
  taskkill /f /pid %%p >nul 2>&1
)

echo Stopping database containers (data kept in Docker volumes)...
cd /d "%~dp0tiangong-erp"
docker compose stop

echo.
echo All stopped. Start again: 1.start-db  2.start-backend  3.start-frontend
pause
