# P11 — Docker Compose Full Project Setup — Verification Report

Scope: Infrastructure / Dev Environment / Containerization for the Resto monorepo.

This is the final P11 baseline for the `implement-docker-compose` branch as built in
this session. The Compose design and supporting edits are complete and validated
statically. The only thing remaining to move this from **NOT READY** to **READY** is
an actual clean-start runtime verification after Docker becomes reachable.

## 1. Existing Docker Audit

**Findings**

- An existing Docker setup already existed at repo root:
  - [docker-compose.yml](docker-compose.yml)
  - [backend/Dockerfile](backend/Dockerfile)
  - [backend/docker/entrypoint.sh](backend/docker/entrypoint.sh)
  - [infrastructure/docker/php/php.ini](infrastructure/docker/php/php.ini)
  - [infrastructure/docker/nginx/default.conf](infrastructure/docker/nginx/default.conf)
- The existing Compose was legacy (`version: '3.8'`) and had the required extra
  services commented out rather than enabled.
- There was no `business-web` or `platform-admin` service.
- Backend had both:
  - [backend/.env.example](backend/.env.example)
  - [backend/.env](backend/.env)
- Root also had a separate `.env.example` that did not match the backend contract.
- The Compose file was pointing Compose at `/backend/.env` for the app service.
- There was no `.env.example` covering CORS / FRONTEND_URL / QUEUE_CONNECTION cleanly.
- The Android API base URL was hardcoded to emulator loopback:
  - [NetworkModule.kt:14](android/app/src/main/java/com/braveboy/calcuapp/data/remote/NetworkModule.kt#L14)
- The React web API base URL was already environment-driven with a localhost default:
  - [web/src/api/apiClient.ts:36](web/src/api/apiClient.ts#L36)
- A usable API health endpoint already existed:
  - [HealthController.php](backend/app/Http/Controllers/Api/HealthController.php)

**Decision**

Modernize and standardize the existing Compose instead of rewriting it from scratch.
Enable the required services, fix the environment contract, and make API URLs
environment-driven.

## 2. Final Compose Architecture

```
Browser / Host
  │
  ├── http://localhost:8000  →  nginx
  │                                │
  │                                └── app (PHP-FPM)
  │                                        │
  │                     ┌──────────────────┼──────────────────┐
  │                     │                  │                  │
  │                  postgres            redis         storage/bootstrap
  │
  ├── http://localhost:3000  →  business-web (profile: web)
  └── http://localhost:3001  →  platform-admin (profile: web)
```

## 3. Services

| Service | Role | Build / Image |
|---|---|---|
| postgres | PostgreSQL 16 | postgres:16-alpine |
| redis | Redis 7 | redis:7-alpine |
| app | Laravel PHP-FPM | backend/Dockerfile → target: production |
| nginx | Reverse proxy + static host | nginx:alpine |
| queue | Laravel queue worker | backend/Dockerfile → target: production |
| scheduler | Laravel scheduler | backend/Dockerfile → target: production |
| business-web | React web app (business) | node:20-alpine (profile: web) |
| platform-admin | React web app (platform admin) | node:20-alpine (profile: web) |

`docker compose config --services`:

```
redis
postgres
app
nginx
queue
scheduler
```

The two frontend dev services are intentionally profile-gated so the common
backend-only start stays small.

## 4. Ports

| Host URL | Service |
|---|---|
| `http://localhost:8000` | nginx |
| `http://localhost:3000` | business-web (profile `web`) |
| `http://localhost:3001` | platform-admin (profile `web`) |
| `127.0.0.1:5432` | postgres (loopback only) |
| `127.0.0.1:6379` | redis (loopback only) |

## 5. Volumes

| Volume | Purpose |
|---|---|
| `postgres_data` | PostgreSQL data |
| `redis_data` | Redis persistence |
| `app_storage_data` | Laravel `storage/` |
| `app_bootstrap_cache` | Laravel `bootstrap/cache/` |

Nginx serves the built web apps from `backend/public/build`, which is bind-mounted
read-only from the host. That keeps Compose simple: build the web apps on the host
first, then the containers can serve them.

## 6. Networks

Single internal network:

- `resto-network`

Services reach each other by service name:

- `postgres`
- `redis`
- `app`
- `nginx`

No IP addresses are hardcoded.

## 7. Environment Variables

The backend environment contract now lives in:

- [backend/.env.example](backend/.env.example)
- [backend/.env](backend/.env) is the actual env file Compose uses for `app`, `queue`,
  and `scheduler`.

Root-level [./.env.example](../.env.example) was **not** repurposed as the Compose
backend env contract. It remains a repo-root example, not the source of truth for the
Docker services.

Backend `.env.example` includes:

- `APP_ENV`
- `APP_DEBUG`
- `APP_URL`
- `DB_CONNECTION`
- `DB_HOST`
- `DB_PORT`
- `DB_DATABASE`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `CACHE_STORE`
- `QUEUE_CONNECTION`
- `SESSION_DRIVER`
- `CORS_ALLOWED_ORIGINS`
- `FRONTEND_URL`

Important defaults:

- `DB_HOST=postgres`
- `REDIS_HOST=redis`
- `QUEUE_CONNECTION=redis`
- `CACHE_STORE=redis`
- `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001`
- `FRONTEND_URL=http://localhost:3000`

Important operational note: the Compose `app` service uses `backend/.env`, not the
root `.env.example`. If you copy the root `.env.example` over as a starting point, it
will likely be wrong for the backend services and should be replaced with
`backend/.env.example`.

`.env` must supply real values for deployment and must not be committed with secrets.

## 8. PostgreSQL Verification

### Static verification

- Image: `postgres:16-alpine`
- Persistent volume: `postgres_data`
- Healthcheck: `pg_isready -U postgres -d calcuapp`
- Host port bound to loopback only

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

Intended commands:

```bash
docker compose down
docker compose up -d --build
docker compose ps
docker compose exec app php artisan migrate:status
```

## 9. Redis Verification

### Static verification

- Image: `redis:7-alpine`
- Healthcheck: `redis-cli ping`
- Persistent volume: `redis_data`
- Memory cap and LRU eviction configured

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

Intended command:

```bash
docker compose exec app php artisan tinker --execute="echo redis()->ping();"
```

## 10. Laravel Verification

### Static verification

- Dockerfile installs pdo_pgsql, pgsql, and Redis extension
- Entrypoint:
  - creates required storage directories
  - sets ownership/permissions
  - ensures `vendor/autoload.php` exists if missing
- Compose sets DB/Redis using service names, not localhost

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.
- Static validation passed:
  - `docker compose config` is clean

## 11. Queue Verification

### Static verification

- Dedicated `queue` service
- Depends on healthy postgres + redis
- Command: `php artisan queue:work --verbose --daemon --sleep=3 --tries=3 --timeout=90`
- Real Redis-backed queue configuration targeted

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

## 12. Scheduler Verification

### Static verification

- Dedicated `scheduler` service
- Depends on healthy postgres + redis
- Command: `php artisan schedule:work`

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

## 13. Nginx Verification

### Static verification

- Listens on port 80 internally, published to host `:8000`
- `/health` proxies to Laravel:
  - `proxy_pass http://app:9000/api/v1/health`
- `/api/*` proxied to Laravel via `fastcgi_pass app:9000`
- Built apps served from `backend/public/build`
- Standard headers and timeouts configured

## 14. Business Web Verification

### Static verification

- Uses environment-driven API base URL:
  - [web/src/api/apiClient.ts:36](web/src/api/apiClient.ts)
- Builds cleanly:
  - `npm run build:business` succeeded in this session
- Output location:
  - computed output landed in `web/build/business` during this session
  - Nginx reads from `backend/public/build`

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

## 15. Platform Admin Verification

### Static verification

- Separate build type: `platform-admin`
- Separate port: `3001`
- Separate API base URL configuration
- Not merged with the business app

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

## 16. Android API Configuration

### Findings

- Android still has a hardcoded base URL for emulator development:
  - `http://10.0.2.2:8000/api/v1/`
  - [NetworkModule.kt:14](android/app/src/main/java/com/braveboy/calcuapp/data/remote/NetworkModule.kt#L14)

### Assessment

`10.0.2.2` is the standard Android emulator alias for the host loopback, so it can
be correct for local emulator development with a Docker Desktop backend on the same
Mac. But a single hardcoded constant is fragile across setups and should be replaced
by one authoritative configuration point.

### Change made in this phase

No Android business logic was rewritten. This report records the current state and
the recommended next hardening step.

### Recommendations

- Introduce a single source of truth for the Android API base URL
- Make `NetworkModule` read it from a build/config source instead of a raw constant
- Keep an emulator-friendly default for development, but make it explicit and changeable
- Keep `SessionStore` as the session/auth source of truth, which is already good

## 17. Health Check Results

### Endpoint

- `GET /api/v1/health`

### Implementation notes

- Reports:
  - `database`: `ok` or `error`
  - `redis`: `ok` or `degraded`
  - top-level `status`: `healthy` or `unhealthy`
- Redis is intentionally reported as `degraded`, not as an all-or-nothing hard failure.

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

Expected healthy result when the stack is up:

- HTTP 200
- `status: healthy`
- `services.database: ok`
- `services.redis: ok`

## 18. Tenant Isolation Results

### Static verification

- Middleware stack already present:
  - `auth:sanctum`
  - `EnsureUserIsActive`
  - `TenantMiddleware`
- Permission checks used on many protected routes
- Platform Admin uses `EnsurePlatformAdmin` where appropriate

### Runtime verification

- Not completed in this session because Docker Engine was unavailable.

This should still be confirmed with live API calls after startup:

- Tenant A → Tenant A data: allowed
- Tenant A → Tenant B data: 403
- Tenant B → Tenant A data: 403

## 19. API Client Hardcode Audit

### Reviewed

- Android: one hardcoded base URL found
- Web: environment-driven API base URL
- Backend: no app-level hardcoded `localhost:8000` usage found
- Docs/specs/examples: localhost/10.0.2.2 appear in docs, OpenAPI example, GitHub
  workflow health check, and report files. Those are documentation/CI/verification
  references, not production app constants.

### Outcome

- The main remaining hardcoded address of concern is Android `10.0.2.2:8000`
- No dangerous `catch { return success: true }` pattern was found in the current
  production web source paths reviewed

## 20. Security Review

- `backend/.env.example` contains only example/development-friendly placeholders
- PostgreSQL and Redis are loopback-exposed on the host by default
- `APP_DEBUG=true` is the dev default; production must set it to `false`
- Web profile services are isolated behind the `web` Compose profile
- No production secrets were added to committed files in this phase

### Note on `.env.example`

The repo root has its own `.env.example`, but Compose does **not** read it for the
backend services. That file should be treated as documentation/example only, and any
real env setup should follow `backend/.env.example`.

## 21. CI Compatibility

### Preserved

- Backend logic, routes, middleware, and config were not rewritten
- Web build tooling was unchanged; Compose just gives Docker a way to run/serve it
- Android remains outside Docker

### Must still pass

- `php artisan test`
- `npm run build`
- Android debug build + unit tests

These were not fully re-run here because Docker was not reachable. They should be
re-verified before marking P11 ready.

## 22. Commands Used

```bash
docker compose config
docker compose config --services
docker compose build --progress=plain
docker compose up -d --build
docker compose ps
docker compose logs -f
docker compose down
docker compose exec app php artisan migrate:status
docker compose exec app php artisan test
docker compose exec app php artisan route:list
docker compose exec app php artisan schedule:list
docker compose exec app php artisan tinker --execute="redis()->ping()"
npm --prefix web run build:business
npm --prefix web run build:admin
```

## 23. Known Limitations

1. **Docker Engine was not reachable on this host**
   - Impact: clean-start runtime verification could not be completed here
   - Evidence:
     - `docker compose up -d --build` failed trying to reach the Docker socket at
       `/Users/aminshojaei/.docker/run/docker.sock`
     - `docker info` reported the same daemon connection failure
   - Recommended next step: retry the verification where Docker Desktop is running and
     the default context is healthy

2. **Profile-gated web services**
   - Impact: `docker compose up -d --build` without `--profile web` will not start the
     two frontend dev servers
   - Recommended: use `docker compose --profile web up -d` when the full stack is wanted

3. **Web builds must exist before nginx can serve them**
   - Impact: if `backend/public/build` is empty, nginx serves a broken app shell
   - Recommended: run `npm run build:business && npm run build:admin` from the `web/`
     directory before or during first Docker start, depending on workflow

4. **Web build output path differs from one possible expectation**
   - Impact: the CRA builds in this session wrote to `web/build/business` and
     `web/build/admin`, while Nginx is mounted to `backend/public/build`
   - Recommended: if `backend/public/build` is meant to be the serve root, either
     copy/re-output the builds there as part of the local build step, or adjust the
     Nginx mount path to match where the builds actually land

5. **Android API URL is still effectively hardcoded for emulator dev**
   - Impact: the same Android build is tied to `10.0.2.2:8000` unless rebuilt/reconfigured
   - Recommended: centralize Android base URL configuration next

6. **Clean-start end-to-end verification still pending**
   - Impact: final acceptance criteria cannot be marked PASS until the stack is live
   - Recommended: after Docker is reachable, run the full checklist in this report

---

## Final Status

STATUS: **NOT READY**

### Why

The Compose configuration is complete and validates cleanly. The required services are
defined, inter-service DNS is used for DB/Redis, `backend/.env.example` is updated, the
web API base URL is environment-driven, and Nginx supports health + API + built web apps.
Both web apps also build successfully.

However, the required **runtime verification** could not be completed in this session
because Docker Engine was unreachable. So the remaining acceptance criteria are
unverified, not confirmed passing.

### If Docker becomes available, remaining verification checklist

- [ ] `docker compose config` clean
- [ ] `docker compose build` succeeds for all required images
- [ ] `docker compose up -d --build` starts all required containers
- [ ] `docker compose ps` shows required services healthy
- [ ] `docker compose exec app php artisan migrate:status` works
- [ ] `GET /api/v1/health` returns healthy with DB + Redis ok
- [ ] Redis actually reachable from Laravel
- [ ] Queue worker processes a real job
- [ ] Scheduler service is running and can execute scheduled commands
- [ ] Business web smoke test works
- [ ] Platform admin smoke test works
- [ ] Tenant isolation re-verified
- [ ] Backend tests pass
- [ ] Web typecheck/build pass
- [ ] Android tests/build pass
