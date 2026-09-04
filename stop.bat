@echo off
echo Stopping Finance Application services on ports 8080 and 8081...

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Stopping Backend Process PID %%a
    taskkill /F /PID %%a 2>nul
)

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8081" ^| findstr "LISTENING"') do (
    echo Stopping Frontend Process PID %%a
    taskkill /F /PID %%a 2>nul
)

echo Services stopped.
