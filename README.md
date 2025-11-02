# TreeVault

Simple guide to run the app. No fluff.

## Quick Start

**Prerequisites:**
- Docker Desktop
- Docker Compose

**Setup Steps:**

1. **Create environment file:**
   
   Copy `.env.example` to `.env` in the project root:
   ```bash
   cp .env.example .env
   ```
   
   Or on Windows PowerShell:
   ```powershell
   Copy-Item .env.example .env
   ```
   
   The default values work fine. Edit `.env` only if you want custom database credentials.

2. **Start all services:**
   ```bash
   docker-compose up -d
   ```
   
   **What happens on startup:**
   - PostgreSQL database starts with an empty database
   - Backend waits for database to be ready (health check)
   - Flyway automatically runs all migrations to create tables and schema
   - Frontend starts after backend is healthy

3. **Access the application:**
   - **Frontend:** http://localhost:3000
   - **API Base:** http://localhost:8080/api/v1
   - **API Docs:** http://localhost:8080/swagger-ui/index.html
   - **Health Check:** http://localhost:8080/actuator/health

## Managing the Application

**View logs:**
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

**Stop services:**
```bash
docker-compose down
```

**Clean everything (including database data):**
```bash
docker-compose down -v
```

**Rebuild after code changes:**
```bash
docker-compose up -d --build
```

## Run Tests

**Backend tests:**
```bash
cd treevault-backend

# Unit tests (fast, no Docker needed)
mvn clean test

# Integration/E2E tests (requires Docker + Testcontainers)
mvn clean verify
```

**Frontend tests:**
```bash
cd treevault-frontend
npm test -- --run
```

## Troubleshooting

**Frontend shows 404 errors:**
- The frontend is configured to call `http://localhost:8080/api/v1`
- Check if backend is running: `docker-compose logs backend`

**Backend fails to start:**
- Check if `.env` file exists with DB credentials
- View backend logs: `docker-compose logs backend`
- Verify Postgres is healthy: `docker-compose ps`

**Ports already in use:**
- Check what's using ports 3000, 8080, or 5432
- Stop conflicting services or change ports in `docker-compose.yml`

**Database issues:**
- Clean and restart: `docker-compose down -v && docker-compose up -d`
- This removes all data and recreates the database from scratch
