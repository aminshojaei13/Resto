FROM php:8.3-fpm-alpine

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
RUN docker-php-ext-install \
    pdo \
    pdo_pgsql \
    pgsql \
    bcmath \
    mbstring \
    xml \
    opcache \
    pcntl \
    sockets \
    zip \
    intl

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
