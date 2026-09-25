# Calcuapp Docker Development Environment

This guide covers setting up, managing, and working with the Docker-based development environment for the Calcuapp backend and services.

---

## 1. Prerequisites

Before starting, ensure you have the following installed:
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) (v20.10.0 or later)
* [Docker Compose](https://docs.docker.com/compose/) (v2.0.0 or later)

---

## 2. Starting the Environment

1. Copy the example environment file if you haven't already:
   ```bash
   cp backend/.env.example backend/.env
   ```

2. Generate an application key if needed:
   ```bash
   cd backend && php artisan key:generate
   ```

3. Build and start the containers in detached mode:
   ```bash
   docker compose up -d --build
   ```

---

## 3. Stopping the Environment

To stop the running containers without removing volumes or state:
```bash
docker compose stop
```

To stop and remove containers and the application network:
```bash
docker compose down
```

---

## 4. Viewing Logs

To view logs for all services in real time:
```bash
docker compose logs -f
```

To view logs for a specific service (e.g., `app` or `nginx`):
```bash
docker compose logs -f app
docker compose logs -f nginx
```

---

## 5. Entering the App Container

To open a shell session inside the primary application (`app`) container:
```bash
docker compose exec app sh
```

---

## 6. Running Migrations

To run database migrations against the PostgreSQL container:
```bash
docker compose exec app php artisan migrate
```

---

## 7. Running Seeders

To populate the database with seed data:
```bash
docker compose exec app php artisan db:seed
```

---

## 8. Running Tests

To run the PHPUnit test suite inside the container:
```bash
docker compose exec app php artisan test
```

---

## 9. Running Queue Worker

To run the queue worker:
```bash
docker compose exec app php artisan queue:work
```
*(Alternatively, you can uncomment the `queue` service section in `docker-compose.yml` to run a dedicated worker container.)*

---

## 10. Running Scheduler

To run the task scheduler:
```bash
docker compose exec app php artisan schedule:work
```
*(Alternatively, you can uncomment the `scheduler` service section in `docker-compose.yml` to run a dedicated, isolated scheduler container.)*

---

## 11. Database Connection Information

* **Database Engine:** PostgreSQL 16
* **Host (inside Docker network):** `postgres`
* **Host (from localhost / host machine):** `localhost` or `127.0.0.1`
* **Port:** `5432`
* **Database Name:** `calcuapp` (configurable in `.env` via `DB_DATABASE`)
* **Username:** `postgres` (configurable in `.env` via `DB_USERNAME`)
* **Password:** `secret` (configurable in `.env` via `DB_PASSWORD`)

---

## 12. Redis Connection Information

* **Service Name (inside Docker network):** `redis`
* **Host (from localhost / host machine):** `127.0.0.1`
* **Port:** `6379`
* **Client:** `phpredis` (PHP native extension)

---

## 13. Troubleshooting

### Permission Issues on `storage` or `bootstrap/cache`
If Laravel reports permission denied errors for writing logs or cache:
```bash
docker compose exec app chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache
docker compose exec app chmod -R 775 /var/www/html/storage /var/www/html/bootstrap/cache
```

### Database Connection Refused
Ensure that the `postgres` container's healthcheck passes by running:
```bash
docker compose ps
```
If PostgreSQL is unhealthy or restarting, check `docker compose logs postgres`.

### Cache / Config Stale Issues
If configuration changes do not take effect immediately:
```bash
docker compose exec app php artisan config:clear
docker compose exec app php artisan cache:clear
```

---

## 14. Resetting the Development Database and Volumes (DESTRUCTIVE)

> **WARNING:** The following command will permanently delete all persisted database records and Redis state.

To remove all containers, networks, and persistent volume data:
```bash
docker compose down -v
```

After resetting, bring up the stack and re-run migrations and seeders:
```bash
docker compose up -d --build
docker compose exec app php artisan migrate --seed
```
