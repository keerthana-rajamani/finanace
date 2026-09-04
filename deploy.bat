@echo off
echo ===================================================
echo Personal Finance and Budget Management Application
echo Starting Full Application Deployment...
echo ===================================================

echo [1/3] Checking MySQL Service...
sc query MySQL80 | findstr "RUNNING" >nul
if %ERRORLEVEL% NEQ 0 (
    echo Starting MySQL80 service...
    net start MySQL80
)

echo [2/3] Starting Spring Boot Backend on Port 8080...
start "Finance Backend" /D "%~dp0springapp" java -jar target\springapp-0.0.1-SNAPSHOT.jar

echo [3/3] Starting React Frontend on Port 8081...
start "Finance Frontend" /D "%~dp0reactapp" npm start

echo.
echo ===================================================
echo Deployment successfully initiated!
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:8081
echo ===================================================
