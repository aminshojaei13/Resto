<?php

use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return response()->json([
        'platform' => 'Calcuapp SaaS REST API Backend',
        'version' => '1.0.0',
        'status' => 'operational'
    ]);
});
