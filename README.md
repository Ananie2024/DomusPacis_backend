---
title: Domus Pacis Backend
emoji: 🏨
colorFrom: blue
colorTo: purple
sdk: docker
pinned: false
---
# Domus Pacis Platform

Full-stack hospitality management platform for Domus Pacis, with a Spring Boot API and a Next.js admin/public frontend.

## Stack

- Backend: Java 21, Spring Boot 3, Spring Security, JPA, MySQL, JWT
- Frontend: Next.js 15, React 18, TypeScript, Tailwind CSS, React Query
- Tests: JUnit/Testcontainers for backend integration tests

## Required Environment

Backend runtime variables:

```bash
MYSQLUSER=...
MYSQLPASSWORD=...
DB_HOST=...
DB_PORT=3306
DB_NAME=domuspacis
JWT_SECRET=use-a-long-random-secret-at-least-32-bytes
MAIL_USERNAME=...
MAIL_PASSWORD=...
CORS_ORIGINS=https://your-frontend.example.com
PORT=8080
```

SSL/TLS variables (defaults are production-safe for TiDB Cloud Serverless):

```bash
DB_USE_SSL=true          # Enable TLS (required by TiDB Cloud Serverless)
DB_REQUIRE_SSL=true      # Reject non-TLS connections
DB_SSL_MODE=VERIFY_IDENTITY  # VERIFY_IDENTITY | VERIFY_CA | PREFERRED | REQUIRED
```

For local MySQL development, set `DB_USE_SSL=false` and `DB_REQUIRE_SSL=false`, or use the `local` Spring profile (`application-local.yml`).

Frontend runtime variables:

```bash
NEXT_PUBLIC_API_URL=https://your-backend.example.com/api/v1
```

For local frontend development, omit `NEXT_PUBLIC_API_URL` to proxy to `http://localhost:8080/api/v1`.

## Local Development

Backend:

```bash
./mvnw spring-boot:run
```

Frontend:

```bash
cd domus-pacis-frontend
npm install
npm run dev
```

## Verification

Backend compile:

```bash
./mvnw -DskipTests compile
```

Backend tests require Docker because integration tests use Testcontainers:

```bash
./mvnw test
```

Frontend:

```bash
cd domus-pacis-frontend
npm run type-check
npm run build
npm audit --omit=dev
```

## Deployment Notes

- Do not commit `.env`, local database files, or generated frontend build artifacts.
- Configure explicit `CORS_ORIGINS`; wildcard CORS is not suitable for credentialed API calls.
- Set `JWT_SECRET`, `MYSQLUSER`, and `MYSQLPASSWORD` in the deployment environment. The backend is configured to fail fast if these are missing.
- The Dockerfile builds the backend jar with tests skipped and runs it on the configured `PORT`.