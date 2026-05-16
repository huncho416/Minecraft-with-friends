<?php

namespace Tests\Unit\Http\Middleware;

use Tests\Assertions\MiddlewareAttributeAssertionsTrait;
use Tests\TestCase;
use Tests\Traits\Http\MocksMiddlewareClosure;
use Tests\Traits\Http\RequestMockHelpers;

abstract class MiddlewareTestCase extends TestCase
{
    use MiddlewareAttributeAssertionsTrait;
    use MocksMiddlewareClosure;
    use RequestMockHelpers;

    /**
     * Setup tests with a mocked request object and normal attributes.
     */
    protected function setUp(): void
    {
        parent::setUp();

        $this->buildRequestMock();
    }
}
