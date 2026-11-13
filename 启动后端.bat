@echo off
title TianGong ERP - Backend (:48080)

cd /d "%~dp0tiangong-erp"

echo Waiting for MySQL (max 60s)...
set /a n=0
:waitdb
docker exec tiangong-mysql mysqladmin ping -uroot -ptiangong123 --silent >nul 2>&1
if %errorlevel%==0 goto dbready
set /a n+=1
if %n% geq 30 goto dbfail
timeout /t 2 /nobreak >nul
goto waitdb

:dbfail
echo MySQL not ready in 60s. Run start-db.bat first.
pause
exit /b 1

:dbready
echo MySQL ready.

if exist "tiangong-server\target\tiangong-server-1.0.0-SNAPSHOT.jar" goto run
echo jar not found. Building first (1-3 min)...
call mvn -B -ntp -DskipTests package
if errorlevel 1 goto buildfail
goto run

:buildfail
echo Build FAILED. Check Maven output above.
pause
exit /b 1

:run
echo Starting backend: http://localhost:48080  (Swagger: /swagger-ui.html)
echo Login: admin / admin123
echo SUCCESS CHECK: wait for "Started TiangongServerApplication". Close this window to stop.
echo.
java -jar tiangong-server\target\tiangong-server-1.0.0-SNAPSHOT.jar
echo.
echo Backend stopped.
pause
