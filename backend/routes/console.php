<?php

use Illuminate\Support\Facades\Artisan;

Artisan::command('calcuapp:status', function () {
    $this->info('Calcuapp SaaS Platform Engine Active.');
})->purpose('Display Calcuapp system status');
