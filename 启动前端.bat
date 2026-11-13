@echo off
title TianGong ERP - Frontend (:5173)

cd /d "%~dp0tiangong-ui"

if exist "node_modules" goto run
echo First run: installing npm dependencies (1-3 min)...
call npm install --registry=https://registry.npmmirror.com
if errorlevel 1 goto installfail
goto run

:installfail
echo npm install FAILED. Check output above.
pause
exit /b 1

:run
echo Starting frontend dev server...
echo Browser: http://localhost:5173   Login: admin / admin123
echo Close this window to stop the frontend.
echo.
call npm run dev
echo.
echo Frontend stopped.
pause
