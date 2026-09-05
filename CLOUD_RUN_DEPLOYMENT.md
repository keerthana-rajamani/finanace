# Cloud Run & Cloud Deployment Guide

This document provides complete instructions for deploying the Personal Finance and Budget Management Application to **Google Cloud Run** and containerized cloud environments, per SRS Section 2.4 and 3.6.5.

---

## ⚡ Active Live Deployment (Instant Access)

The application is currently running live via Cloudflare edge tunnel:

| Service | Live URL | Details |
| :--- | :--- | :--- |
| **Live Full-Stack App** | [https://baseline-diary-big-eddie.trycloudflare.com](https://baseline-diary-big-eddie.trycloudflare.com) | Unified React SPA + Spring Boot REST API + Live Database |
| **GitHub Pages** | [https://keerthana-rajamani.github.io/finanace/](https://keerthana-rajamani.github.io/finanace/) | Production React client connected to live backend |
| **GitHub Repository** | [https://github.com/keerthana-rajamani/finanace](https://github.com/keerthana-rajamani/finanace) | Source repository on `main` and `gh-pages` |

### 🔑 Test Credentials
- **Email**: `liveuser@example.com`
- **Password**: `Password@123`

---

## 🐳 Option 1: Deploying to Google Cloud Run (Recommended)

Google Cloud Run runs the application container serverlessly with automatic HTTPS, scaling from 0 to multiple instances.

### Prerequisites
1. [Google Cloud SDK (gcloud CLI)](https://cloud.google.com/sdk/docs/install) installed and logged in:
   ```bash
   gcloud auth login
   gcloud config set project YOUR_PROJECT_ID
   ```
2. Enable required Google Cloud APIs:
   ```bash
   gcloud services enable run.googleapis.com cloudbuild.googleapis.com containerregistry.googleapis.com
   ```

### Method A: Single Command Deployment via Cloud Build
Run the provided automated script:
- **Windows**:
  ```cmd
  deploy-cloudrun.bat YOUR_PROJECT_ID us-central1
  ```
- **Linux / macOS**:
  ```bash
  chmod +x deploy-cloudrun.sh
  ./deploy-cloudrun.sh YOUR_PROJECT_ID us-central1
  ```

Or directly execute:
```bash
gcloud builds submit --config cloudbuild.yaml .
```

### Method B: Deploy from Local Docker Build
```bash
# 1. Build container image
docker build -t gcr.io/YOUR_PROJECT_ID/personal-finance-app:latest .

# 2. Push to Google Container Registry (or Artifact Registry)
docker push gcr.io/YOUR_PROJECT_ID/personal-finance-app:latest

# 3. Deploy to Cloud Run
gcloud run deploy personal-finance-app \
  --image gcr.io/YOUR_PROJECT_ID/personal-finance-app:latest \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory 1Gi \
  --cpu 1
```

### Method C: Connecting Google Cloud SQL (MySQL)
To link a managed MySQL instance on GCP:
```bash
gcloud run services update personal-finance-app \
  --region us-central1 \
  --add-cloudsql-instances YOUR_PROJECT_ID:REGION:INSTANCE_NAME \
  --set-env-vars "SPRING_DATASOURCE_URL=jdbc:mysql:///financedb?cloudSqlInstance=YOUR_PROJECT_ID:REGION:INSTANCE_NAME&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false" \
  --set-env-vars "SPRING_DATASOURCE_USERNAME=root,SPRING_DATASOURCE_PASSWORD=YOUR_DB_PASSWORD"
```

---

## ☁️ Option 2: Deploying via RunCloud (RunCloud.io)

If using RunCloud to manage a VPS (DigitalOcean, AWS, Linode, Vultr):
1. **Connect Server**: Connect your Ubuntu 22.04 LTS server in the RunCloud dashboard.
2. **Install Docker via RunCloud**: RunCloud allows deploying Docker containers or native Java/Node apps.
3. **Deploy via Docker Compose**:
   - Upload `docker-compose.yml` and `C:\Finance` repository to `/home/runcloud/webapps/finance`.
   - Run:
     ```bash
     docker compose up -d --build
     ```
4. **Configure Nginx Reverse Proxy**:
   - In RunCloud web application settings, configure reverse proxy targeting port `8080`.
   - Enable free Let's Encrypt SSL with 1-click in RunCloud SSL tab.

---

## 📦 Option 3: Local Docker Compose

To run the complete stack locally with MySQL 8.0 and the unified application:
```bash
docker compose up -d --build
```
- Frontend + Unified Backend: `http://localhost:8080`
- MySQL Database: `localhost:3306` (database: `financedb`)
