@echo off
REM =========================================================================
REM Google Cloud Run Deployment Script for Personal Finance App
REM =========================================================================
echo =======================================================
echo   Deploying Personal Finance App to Google Cloud Run
echo =======================================================

IF "%~1"=="" (
    echo Usage: deploy-cloudrun.bat ^<GCP_PROJECT_ID^> [REGION]
    echo Example: deploy-cloudrun.bat my-finance-project-123 us-central1
    exit /b 1
)

set PROJECT_ID=%~1
set REGION=%~2
IF "%REGION%"=="" set REGION=us-central1
set SERVICE_NAME=personal-finance-app

echo [*] Target Project : %PROJECT_ID%
echo [*] Target Region  : %REGION%
echo [*] Service Name   : %SERVICE_NAME%

echo [*] Submitting build to Google Cloud Build...
call gcloud builds submit --project %PROJECT_ID% --config cloudbuild.yaml .
IF %ERRORLEVEL% NEQ 0 (
    echo [!] Cloud Build failed. Please ensure gcloud is authenticated.
    exit /b %ERRORLEVEL%
)

echo [*] Cloud Run deployment completed successfully!
echo [*] Retrieving public Cloud Run URL...
call gcloud run services describe %SERVICE_NAME% --project %PROJECT_ID% --region %REGION% --format="value(status.url)"
