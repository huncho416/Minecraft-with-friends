<?php

namespace Tests\Unit\Http\Middleware;

use App\Http\Middleware\RedirectIfAuthenticated;
use Illuminate\Auth\AuthManager;
use Illuminate\Http\RedirectResponse;
use Mockery as m;
use Mockery\MockInterface;

class RedirectIfAuthenticatedTest extends MiddlewareTestCase
{
    private MockInterface $authManager;

    /**
     * Setup tests.
     */
    protected function setUp(): void
    {
        parent::setUp();

        $this->authManager = m::mock(AuthManager::class);
    }

    /**
     * Test that an authenticated user is redirected.
     */
    public function test_authenticated_user_is_redirected()
    {
        $this->authManager->shouldReceive('guard')->with(null)->once()->andReturnSelf();
        $this->authManager->shouldReceive('check')->withNoArgs()->once()->andReturn(true);

        $response = $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
        $this->assertInstanceOf(RedirectResponse::class, $response);
        $this->assertEquals(route('index'), $response->getTargetUrl());
    }

    /**
     * Test that a non-authenticated user continues through the middleware.
     */
    public function test_non_authenticated_user_is_not_redirected()
    {
        $this->authManager->shouldReceive('guard')->with(null)->once()->andReturnSelf();
        $this->authManager->shouldReceive('check')->withNoArgs()->once()->andReturn(false);

        $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
    }

    /**
     * Return an instance of the middleware using mocked dependencies.
     */
    private function getMiddleware(): RedirectIfAuthenticated
    {
        return new RedirectIfAuthenticated($this->authManager);
    }
}
