@echo off
cd /d "%~dp0tiangong-erp"
set TRACE=%~dp0startup-log.txt
echo t1 >> "%TRACE%"
if not exist "tiangong-server\\target\\tiangong-server-1.0.0-SNAPSHOT.jar" (
  echo t2-missing >> "%TRACE%"
)
echo t3-after-jar-block >> "%TRACE%"
if not exist "..\\tiangong-ui\\node_modules" (
  echo t4-missing >> "%TRACE%"
)
echo t5-done >> "%TRACE%"
