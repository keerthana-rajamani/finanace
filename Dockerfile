# =========================================================================
# Multi-Stage Dockerfile for Google Cloud Run / Container Platforms
# Personal Finance and Budget Management Application (SRS Compliant)
# =========================================================================

# --- Stage 1: Build React SPA Frontend ---
FROM node:18-alpine AS frontend-build
WORKDIR /app/reactapp
COPY reactapp/package*.json ./
RUN npm install --legacy-peer-deps
COPY reactapp/ ./
RUN npm run build

# --- Stage 2: Build Spring Boot Application with bundled static assets ---
FROM maven:3.9.6-eclipse-temurin-17 AS backend-build
WORKDIR /app/springapp
COPY springapp/pom.xml .
RUN mvn dependency:go-offline -B || true
COPY springapp/src ./src
COPY --from=frontend-build /app/reactapp/build/ ./src/main/resources/static/
RUN mvn clean package -DskipTests

# --- Stage 3: Minimal Production JRE Runtime ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd -m -u 1001 appuser
USER appuser

COPY --from=backend-build /app/springapp/target/springapp-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT} -jar app.jar"]
