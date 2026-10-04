<?php

namespace Tests;

use Illuminate\Foundation\Testing\TestCase as BaseTestCase;

abstract class TestCase extends BaseTestCase
{
    use CreatesApplication;

    /**
     * The auth guard memoises the user it resolved for the life of the test
     * application, so several requests inside one test would otherwise all see
     * the first resolved user. Flushing the guards makes each simulated request
     * authenticate from scratch, exactly as a real request would.
     */
    protected function withFreshAuth(): static
    {
        $this->app['auth']->forgetGuards();

        return $this;
    }
}
