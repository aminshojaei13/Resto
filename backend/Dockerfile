FROM php:8.4-fpm-alpine AS base

# Install system build dependencies and runtime libraries
RUN apk add --no-cache \
    curl \
    git \
    build-base \
    libpng-dev \
    libxml2-dev \
    zip \
    unzip \
    postgresql-dev \
    oniguruma-dev \
    libzip-dev \
    icu-dev \
    $PHPIZE_DEPS

# Install PHP extensions
#
# IMPORTANT: Alpine PHP source packages do not reliably ship
# linux/sock_diag.h, so the "sockets" extension can fail to compile on
# php:*-fpm-alpine. Laravel does not strictly require the sockets extension
# (predis/phpredis works over standard TCP), so we build without it in
# Alpine and only enable it when the headers are present.
RUN set -e \
  && apk add --no-cache linux-headers \
     || true \
  && docker-php-ext-install \
       pdo \
       pdo_pgsql \
       pgsql \
       bcmath \
       mbstring \
       xml \
       opcache \
       pcntl \
       zip \
       intl \
  && if test -f /usr/include/linux/sock_diag.h; then \
       docker-php-ext-install sockets; \
     fi \
  && apk del linux-headers 2>/dev/null || true

# Install Redis PHP extension via PECL
RUN pecl install redis \
    && docker-php-ext-enable redis

# Clean up build dependencies to minimize image size
RUN apk del build-base $PHPIZE_DEPS

# Install Composer
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Set working directory
WORKDIR /var/www/html

# Copy application files
COPY . /var/www/html

# Ensure entrypoint script is executable
RUN chmod +x /var/www/html/docker/entrypoint.sh

# Set directory permissions for Laravel
RUN chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache

# Expose port 9000 for FastCGI
EXPOSE 9000

ENTRYPOINT ["/var/www/html/docker/entrypoint.sh"]
CMD ["php-fpm"]

FROM base AS development

# In development we keep the repository mounted from the host via Compose.
# No extra build steps required here.

FROM base AS production

# For production builds, dependencies should already be installed in the image
# or via a prior build step. This stage exists so Compose can select a stable
# labeled target explicitly.
RUN true
