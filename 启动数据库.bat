@echo off
title TianGong ERP - Database (MySQL + Redis)

echo [1/3] Starting Docker Desktop (skip if already running)...
start "" "E:\Docker\Docker Desktop.exe"

echo [2/3] Waiting for Docker engine (max 60s)...
set /a n=0
:wait
docker info >nul 2>&1
if %errorlevel%==0 goto ready
set /a n+=1
if %n% geq 30 (
  echo Docker engine not ready in 60s. Please open Docker Desktop manually and retry.
  pause
  exit /b 1
)
timeout /t 2 /nobreak >nul
goto wait

:ready
echo [3/3] Starting MySQL and Redis containers...
cd /d "%~dp0tiangong-erp"
docker compose up -d
echo.
echo Database ready: MySQL :3306 / Redis :6379
echo Tip: docker compose ps = status, docker compose stop = stop (data kept)
pause
