@echo off
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1
for /f "tokens=2" %%p in ('tasklist ^| findstr /i "cmd.exe"') do taskkill /f /pid %%p >nul 2>&1
echo cleaned
