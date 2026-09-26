# Resto SaaS Production Deployment Guide

## 1. Production Architecture Overview

Resto is designed to run behind an HTTPS reverse proxy (Nginx or AWS ALB) forwarding API traffic to PHP-FPM and static web assets to Nginx/CDN.

```text
Internet
   ↓ (Port 443 / HTTPS)
Reverse Proxy / Nginx
   ├── Web Static App (React SPA)
   └── Laravel REST API (PHP 8.2 FPM)
         ├── PostgreSQL 16 (Relational DB & Financial Source of Truth)
         ├── Redis 7 (Cache, Sessions, Rate Limiting, Idempotency)
         ├── Queue Workers (`php artisan queue:work redis`)
         └── Scheduler (`php artisan schedule:run`)
```

---

## 2. Server Environment Requirements

- **Operating System**: Ubuntu 22.04 LTS / Debian 12
- **PHP**: PHP 8.2+ with `pdo_pgsql`, `redis`, `mbstring`, `bcmath`, `curl`, `xml`, `zip`
- **Database**: PostgreSQL 16 (Managed AWS RDS or containerized)
- **Cache/Queue**: Redis 7
- **Web Server**: Nginx 1.24+ with SSL certificate (Let's Encrypt / AWS ACM)
- **Container Runtime**: Docker & Docker Compose (optional for containerized deployments)

---

## 3. Required Environment Variables (`.env`)

```ini
APP_NAME=Resto
APP_ENV=production
APP_DEBUG=false
APP_KEY=base64:GENERATE_UNIQUE_32_BYTE_KEY_HERE
APP_URL=https://app.resto-platform.com

# --- Database (PostgreSQL) ---
DB_CONNECTION=pgsql
DB_HOST=postgres
DB_PORT=5432
DB_DATABASE=resto_production
DB_USERNAME=resto_db_user
DB_PASSWORD=SECURE_DB_PASSWORD_HERE

# --- Redis ---
REDIS_CLIENT=phpredis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=SECURE_REDIS_PASSWORD_HERE

# --- Session & Cache ---
SESSION_DRIVER=database
SESSION_LIFETIME=120
CACHE_STORE=redis
QUEUE_CONNECTION=redis
```

*DANGER: Never set `APP_DEBUG=true` or commit actual production passwords to Git!*

---

## 4. Step-by-Step Deployment Commands

### Step 1: Code Update & Autoload Optimization
```bash
git pull origin main
cd backend
composer install --no-dev --optimize-autoloader
```

### Step 2: Database Migration (Non-Destructive)
```bash
php artisan migrate --force
```

### Step 3: Production Caching
```bash
php artisan config:cache
php artisan route:cache
php artisan view:cache
```

### Step 4: Web Production Build
```bash
cd ../web
npm ci
npm run build
```

---

## 5. Supervisor Queue Worker Configuration

Create `/etc/supervisor/conf.d/resto-worker.conf`:
```ini
[program:resto-worker]
process_name=%(program_name)s_%(process_num)02d
command=php /var/www/html/backend/artisan queue:work redis --sleep=3 --tries=3 --max-time=3600
autostart=true
autorestart=true
stopasgroup=true
killasgroup=true
user=www-data
numprocs=2
redirect_stderr=true
stdout_logfile=/var/www/html/backend/storage/logs/worker.log
stopwaitsecs=3600
```

Reload Supervisor:
```bash
sudo supervisorctl reread
sudo supervisorctl update
sudo supervisorctl start resto-worker:*
```

---

## 6. Nginx Virtual Host Configuration Snippet

```nginx
server {
    listen 80;
    server_name app.resto-platform.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name app.resto-platform.com;

    ssl_certificate /etc/letsencrypt/live/app.resto-platform.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.resto-platform.com/privkey.pem;

    root /var/www/html/web/build;
    index index.html;

    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-XSS-Protection "1; mode=block";
    add_header X-Content-Type-Options "nosniff";

    # REST API Proxy
    location /api/ {
        proxy_pass http://127.0.0.1:8000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 7. Health Check Verification

Test endpoint:
```bash
curl -i https://app.resto-platform.com/api/v1/health
```

Expected HTTP `200 OK`:
```json
{
  "status": "healthy",
  "services": {
    "database": "ok",
    "redis": "ok"
  },
  "timestamp": "2025-01-15T12:00:00Z"
}
```

---

## 8. Safe Rollback Procedure

If a deployment fails:
1. Revert Git tag: `git checkout <previous_stable_tag>`
2. Clear caches: `php artisan config:clear && php artisan route:clear`
3. Re-run caches: `php artisan config:cache && php artisan route:cache`
4. Restart queue workers: `php artisan queue:restart`
