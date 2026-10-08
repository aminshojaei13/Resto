<?php

use Laravel\Sanctum\Sanctum;

return [

    'stateful' => explode(',', (string) env('SANCTUM_STATEFUL_DOMAINS', sprintf(
        '%s%s',
        'localhost,localhost:3000,localhost:3001,127.0.0.1,127.0.0.1:3000,127.0.0.1:3001,::1',
        Sanctum::currentApplicationUrlWithPort(),
    ))),

    'guard' => ['web'],

    /*
    |--------------------------------------------------------------------------
    | Expiration
    |--------------------------------------------------------------------------
    |
    | Personal access tokens are short lived. Expired tokens stop working
    | instead of accumulating forever on a shared device.
    |
    */

    'expiration' => (int) env('AUTH_TOKEN_EXPIRATION_MINUTES', 60 * 12),

    'token_prefix' => env('SANCTUM_TOKEN_PREFIX', 'resto_'),

    'middleware' => [
        'encrypt_cookies' => Illuminate\Cookie\Middleware\EncryptCookies::class,
        'validate_csrf_token' => Illuminate\Foundation\Http\Middleware\ValidateCsrfToken::class,
        'verified' => Illuminate\Auth\Middleware\EnsureEmailIsVerified::class,
    ],

];
