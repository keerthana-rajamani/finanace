#!/usr/bin/env bash
# =========================================================================
# Google Cloud Run Deployment Script for Personal Finance App
# =========================================================================
set -e

PROJECT_ID=${1:-$GCP_PROJECT_ID}
REGION=${2:-${GCP_REGION:-us-central1}}
SERVICE_NAME="personal-finance-app"

if [ -z "$PROJECT_ID" ]; then
    echo "Usage: ./deploy-cloudrun.sh <GCP_PROJECT_ID> [REGION]"
    echo "Example: ./deploy-cloudrun.sh my-finance-project-123 us-central1"
    exit 1
fi

echo "======================================================="
echo " Deploying $SERVICE_NAME to Google Cloud Run"
echo " Project: $PROJECT_ID | Region: $REGION"
echo "======================================================="

# Submit build to Google Cloud Build and deploy
gcloud builds submit --project "$PROJECT_ID" --config cloudbuild.yaml .

# Fetch the deployed public URL
echo "[*] Retrieving public Cloud Run URL..."
URL=$(gcloud run services describe "$SERVICE_NAME" --project "$PROJECT_ID" --region "$REGION" --format="value(status.url)")
echo "======================================================="
echo " Application successfully deployed on Google Cloud Run!"
echo " Public URL: $URL"
echo "======================================================="
