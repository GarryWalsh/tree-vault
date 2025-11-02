# TreeVault

Simple guide to run the app. No fluff.

## 1) Run with Docker (recommended)

Prereqs: Docker Desktop + Docker Compose.

1. In project root, create `.env`:
```env
DB_NAME=treevault
DB_USER=treevault
DB_PASSWORD=treevault
```
2. Start:
```bash
docker-compose up -d
```
3. Open:
- Frontend: http://localhost:3000
- API base: http://localhost:8080/api/v1
- API docs: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health

Stop:
```bash
docker-compose down
```

## 2) Run locally (backend + frontend)

Prereqs: Java 21, Maven, Node 18+, npm.

Database (choose one):
- Docker: `docker run --name treevault-db -e POSTGRES_DB=treevault -e POSTGRES_USER=treevault -e POSTGRES_PASSWORD=treevault -p 5432:5432 -d postgres:16-alpine`
- Or use the `postgres` service from `docker-compose.yml` and keep only DB running.

Backend:
```bash
cd treevault-backend
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/treevault
set SPRING_DATASOURCE_USERNAME=treevault
set SPRING_DATASOURCE_PASSWORD=treevault
mvn spring-boot:run
```

Frontend (new terminal):
```bash
cd treevault-frontend
npm install
# IMPORTANT: frontend adds "/api/v1" itself
set VITE_API_URL=http://localhost:8080
npm run dev
```
Open http://localhost:3000

## 3) Run tests

Backend:
- Unit tests (fast, no Docker needed):
```bash
cd treevault-backend
mvn clean test
```
- Integration/E2E tests (Docker + Testcontainers required):
```bash
cd treevault-backend
mvn clean verify
```

Frontend:
```bash
cd treevault-frontend
npm test -- --run
```

## 4) IntelliJ IDEA run configs

Available under `.idea/runConfigurations`:
- Backend - All Tests (Maven `clean test`)
- Frontend - All Tests (NPM `test -- --run`)
- All Tests (Compound)
- Backend - Run (Maven `spring-boot:run`)
- Frontend - Dev (NPM `run dev`)
- Start App (Compound)

## Troubleshooting (quick)
- Frontend 404s → ensure `VITE_API_URL=http://localhost:8080` (no `/api/v1`).
- Backend fails to start → Postgres running? Env vars correct?
- Ports busy → free 8080/3000/5432 or change in `docker-compose.yml`.
