# Resto SaaS Platform Monorepo

Resto is an enterprise-grade multi-tenant Point of Sale (POS), Inventory Management, Double-Entry Financial Accounting, and Sales Management SaaS platform.

## Monorepo Architecture

```text
/
├── android/          # Native Android Application (Kotlin, Jetpack Compose M3, Room, DataStore)
├── backend/          # Laravel 11 REST API Backend & Double-Entry Accounting Engine
├── web/              # React Native Web / Next.js Admin & POS Dashboard
├── ios/              # Native iOS Swift / SwiftUI Client
├── infrastructure/   # Docker, Kubernetes, Terraform, and CI/CD Pipeline configurations
├── docs/             # Technical specifications & API documentation
├── scripts/          # Automation & deployment scripts
└── tests/            # End-to-end and integration test suites
```

## Documentation

- [PROJECT_SPEC.md](./PROJECT_SPEC.md) - High-level project specifications, feature requirements, and SaaS tenant architecture.
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture, data flow, and cross-platform communication.
- [AGENTS.md](./AGENTS.md) - AI agent delegation guidelines and workflow directives.
- [DATABASE.md](./DATABASE.md) - PostgreSQL database schema, migrations, and transactional integrity design.
- [API.md](./API.md) - REST API specs, OpenAPI schema, and authentication flow.
- [SECURITY.md](./SECURITY.md) - Multi-tenant data isolation, JWT authentication, and security standards.
- [TESTING.md](./TESTING.md) - Test strategy for unit, integration, and UI automation across platforms.

## Docker Development Environment (P11)

The project can be started with one command:

```bash
docker compose up -d --build
```

This starts:

- `nginx` on `http://localhost:8000`
- `app` Laravel PHP-FPM
- `postgres` PostgreSQL 16
- `redis` Redis 7
- `queue` Laravel queue worker
- `scheduler` Laravel scheduler
- `business-web` on `http://localhost:3000`
- `platform-admin` on `http://localhost:3001`

### First-time setup

```bash
cp backend/.env.example backend/.env
# Edit backend/.env if you want non-default DB/Redis credentials.

docker compose up -d --build

docker compose exec app php artisan key:generate
docker compose exec app php artisan migrate
```

### Common commands

```bash
docker compose ps
docker compose logs -f
docker compose logs -f app
docker compose logs -f nginx
docker compose logs -f queue
docker compose logs -f scheduler

docker compose down                 # stop and remove containers/networks (keeps DB data)
docker compose down -v             # also remove volumes (DESTRUCTIVE: deletes DB + Redis data)

docker compose exec app php artisan migrate
docker compose exec app php artisan db:seed
docker compose exec app php artisan test
docker compose exec app php artisan route:list
docker compose exec app php artisan schedule:list
```

### Web development

```bash
npm run build          # builds business-web + platform-admin into backend/public/build
```

For local development without Docker, the two apps can also run standalone:

```bash
npm run start:business   # http://localhost:3000
npm run start:admin      # http://localhost:3001
```

When running inside Docker, `nginx` serves the built assets from `backend/public/build` so both apps are available through the same host port as the API.

### Android

Android is not containerized in this environment. It connects to the Dockerized API using the host Docker gateway address.

- Emulator default: `http://10.0.2.2:8000/api/v1/`
- Physical device / Docker host network: `http://10.0.0.2:8000/api/v1/` (or your Docker host IP)

Do not use `localhost` from Android; it resolves to the device itself, not the Docker host.

### API health

```bash
curl -i http://localhost:8000/api/v1/health
```

Expected when everything is healthy:

```json
{
  "status": "healthy",
  "services": {
    "database": "ok",
    "redis": "ok"
  },
  "timestamp": "<iso8601>"
}
```

### Notes / caveats

- PostgreSQL and Redis are exposed on the host only on loopback by default. That is intentional for development.
- Secrets and host-specific customizations belong in `backend/.env`, which is gitignored. `backend/.env.example` is the reproducible template.
- `docker compose down -v` deletes persisted database and Redis data. This is expected behavior for a full reset, not a bug.

## Getting Started

### Android App
```bash
cd android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
