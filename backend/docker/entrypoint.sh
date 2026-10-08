#!/bin/sh
set -e

# Ensure required storage directories exist
mkdir -p /var/www/html/storage/app/public \
         /var/www/html/storage/framework/cache/data \
         /var/www/html/storage/framework/sessions \
         /var/www/html/storage/framework/views \
         /var/www/html/storage/logs \
         /var/www/html/bootstrap/cache

# Set permissions for storage and bootstrap/cache
chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache
chmod -R 775 /var/www/html/storage /var/www/html/bootstrap/cache

# In development with a bind mount, make sure vendor exists so artisan can run.
if [ ! -d /var/www/html/vendor/autoload.php ]; then
    echo "[entrypoint] vendor autoload missing; running composer install."
    composer install --no-interaction --optimize-autoloader --no-scripts || composer install --no-interaction --no-dev --optimize-autoloader --no-scripts
fi

# Execute requested command
exec "$@"
