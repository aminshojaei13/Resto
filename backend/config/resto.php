<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Business web front-end origin
    |--------------------------------------------------------------------------
    |
    | Used to build the links inside password-recovery and staff-invitation
    | emails. No technical identifier ever appears in those links beyond the
    | single-use token.
    |
    */

    'frontend_url' => env('FRONTEND_URL', env('APP_URL', 'http://localhost:3000')),

    /*
    |--------------------------------------------------------------------------
    | Session lifetime
    |--------------------------------------------------------------------------
    |
    | Personal access tokens expire after this many minutes. A short value
    | keeps stolen tokens from being useful indefinitely.
    |
    */

    'token_expiration_minutes' => (int) env('AUTH_TOKEN_EXPIRATION_MINUTES', 60 * 12),

    /*
    |--------------------------------------------------------------------------
    | Password recovery delivery
    |--------------------------------------------------------------------------
    |
    | Reset links are only ever delivered by email. The application never
    | returns a reset code, token or link in an HTTP response.
    |
    */

    'password_reset' => [
        'expire_minutes' => (int) env('PASSWORD_RESET_EXPIRE_MINUTES', 60),
        'throttle_seconds' => (int) env('PASSWORD_RESET_THROTTLE_SECONDS', 60),
    ],

    /*
    |--------------------------------------------------------------------------
    | Login throttling
    |--------------------------------------------------------------------------
    |
    | Deliberately generous: the limit exists to stop bulk guessing, not to
    | lock a real cashier out of a busy till. No permanent lockout is used.
    |
    */

    'login' => [
        'max_attempts' => (int) env('LOGIN_MAX_ATTEMPTS', 10),
        'decay_minutes' => (int) env('LOGIN_DECAY_MINUTES', 1),
    ],

    /*
    |--------------------------------------------------------------------------
    | Invitation link lifetime
    |--------------------------------------------------------------------------
    */

    'staff_invitation_hours' => (int) env('STAFF_INVITATION_HOURS', 72),

];
