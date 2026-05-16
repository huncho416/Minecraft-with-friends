<?php

namespace Tests\Unit\Http\Middleware\Api\Daemon;

use App\Exceptions\Repository\RecordNotFoundException;
use App\Http\Middleware\Api\Daemon\DaemonAuthenticate;
use App\Models\Node;
use App\Repositories\Eloquent\NodeRepository;
use Illuminate\Contracts\Encryption\Encrypter;
use Mockery as m;
use Mockery\MockInterface;
use PHPUnit\Framework\Attributes\DataProvider;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;
use Symfony\Component\HttpKernel\Exception\HttpException;
use Tests\Unit\Http\Middleware\MiddlewareTestCase;

class DaemonAuthenticateTest extends MiddlewareTestCase
{
    private MockInterface $encrypter;

    private MockInterface $repository;

    /**
     * Setup tests.
     */
    protected function setUp(): void
    {
        parent::setUp();

        $this->encrypter = m::mock(Encrypter::class);
        $this->repository = m::mock(NodeRepository::class);
    }

    /**
     * Test that if we are accessing the daemon.configuration route this middleware is not
     * applied in order to allow an unauthenticated request to use a token to grab data.
     */
    public function test_response_should_continue_if_route_is_exempted()
    {
        $this->request->expects('route->getName')->withNoArgs()->andReturn('daemon.configuration');

        $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
    }

    /**
     * Test that not passing in the bearer token will result in a HTTP/401 error with the
     * proper response headers.
     */
    public function test_response_should_fail_if_no_token_is_provided()
    {
        $this->request->expects('route->getName')->withNoArgs()->andReturn('random.route');
        $this->request->expects('bearerToken')->withNoArgs()->andReturnNull();

        try {
            $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
        } catch (HttpException $exception) {
            $this->assertEquals(401, $exception->getStatusCode(), 'Assert that a status code of 401 is returned.');
            $this->assertTrue(is_array($exception->getHeaders()), 'Assert that an array of headers is returned.');
            $this->assertArrayHasKey('WWW-Authenticate', $exception->getHeaders(), 'Assert exception headers contains WWW-Authenticate.');
            $this->assertEquals('Bearer', $exception->getHeaders()['WWW-Authenticate']);
        }
    }

    /**
     * Test that passing in an invalid node daemon secret will result in a bad request
     * exception being returned.
     */
    #[DataProvider('badTokenDataProvider')]
    public function test_response_should_fail_if_token_format_is_incorrect(string $token)
    {
        $this->expectException(BadRequestHttpException::class);

        $this->request->expects('route->getName')->withNoArgs()->andReturn('random.route');
        $this->request->expects('bearerToken')->withNoArgs()->andReturn($token);

        $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
    }

    /**
     * Test that an access denied error is returned if the node is valid but the token
     * provided is not valid.
     */
    public function test_response_should_fail_if_token_is_not_valid()
    {
        $this->expectException(AccessDeniedHttpException::class);

        /** @var Node $model */
        $model = Node::factory()->make();

        $this->request->expects('route->getName')->withNoArgs()->andReturn('random.route');
        $this->request->expects('bearerToken')->withNoArgs()->andReturn($model->daemon_token_id.'.random_string_123');

        $this->repository->expects('findFirstWhere')->with(['daemon_token_id' => $model->daemon_token_id])->andReturn($model);
        $this->encrypter->expects('decrypt')->with($model->daemon_token)->andReturns(decrypt($model->daemon_token));

        $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
    }

    /**
     * Test that an access denied exception is returned if the node is not found using
     * the token ID provided.
     */
    public function test_response_should_fail_if_node_is_not_found()
    {
        $this->expectException(AccessDeniedHttpException::class);

        $this->request->expects('route->getName')->withNoArgs()->andReturn('random.route');
        $this->request->expects('bearerToken')->withNoArgs()->andReturn('abcd1234.random_string_123');

        $this->repository->expects('findFirstWhere')->with(['daemon_token_id' => 'abcd1234'])->andThrow(RecordNotFoundException::class);

        $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
    }

    /**
     * Test a successful middleware process.
     */
    public function test_successful_middleware_process()
    {
        /** @var Node $model */
        $model = Node::factory()->make();

        $this->request->expects('route->getName')->withNoArgs()->andReturn('random.route');
        $this->request->expects('bearerToken')->withNoArgs()->andReturn($model->daemon_token_id.'.'.decrypt($model->daemon_token));

        $this->repository->expects('findFirstWhere')->with(['daemon_token_id' => $model->daemon_token_id])->andReturn($model);
        $this->encrypter->expects('decrypt')->with($model->daemon_token)->andReturns(decrypt($model->daemon_token));

        $this->getMiddleware()->handle($this->request, $this->getClosureAssertions());
        $this->assertRequestHasAttribute('node');
        $this->assertRequestAttributeEquals($model, 'node');
    }

    /**
     * Provides different tokens that should trigger a bad request exception due to
     * their formatting.
     *
     * @return array|string[][]
     */
    public static function badTokenDataProvider(): array
    {
        return [
            ['foo'],
            ['foobar'],
            ['foo-bar'],
            ['foo.bar.baz'],
            ['.foo'],
            ['foo.'],
            ['foo..bar'],
        ];
    }

    /**
     * Return an instance of the middleware using mocked dependencies.
     */
    private function getMiddleware(): DaemonAuthenticate
    {
        return new DaemonAuthenticate($this->encrypter, $this->repository);
    }
}
