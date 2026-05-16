<?php

namespace Tests\Integration\Api\Application\Nests;

use App\Contracts\Repository\NestRepositoryInterface;
use App\Models\Egg;
use App\Models\Nest;
use App\Transformers\Api\Application\NestTransformer;
use Illuminate\Database\Eloquent\Collection;
use Illuminate\Http\Response;
use Tests\Integration\Api\Application\ApplicationApiIntegrationTestCase;

class NestControllerTest extends ApplicationApiIntegrationTestCase
{
    private NestRepositoryInterface $repository;

    /**
     * Setup tests.
     */
    protected function setUp(): void
    {
        parent::setUp();

        $this->repository = $this->app->make(NestRepositoryInterface::class);
    }

    /**
     * Test that the expected nests are returned by the request.
     */
    public function test_nest_response()
    {
        Nest::factory()->count(2)->create();

        /** @var Collection<int, Nest> $nests */
        $nests = $this->repository->all();

        $response = $this->getJson('/api/application/nests');
        $response->assertStatus(Response::HTTP_OK);
        $response->assertJsonCount(count($nests), 'data');
        $response->assertJsonStructure([
            'object',
            'data' => [['object', 'attributes' => ['id', 'uuid', 'author', 'name', 'description', 'created_at', 'updated_at']]],
            'meta' => ['pagination' => ['total', 'count', 'per_page', 'current_page', 'total_pages']],
        ]);

        $response->assertJson([
            'object' => 'list',
            'data' => [],
            'meta' => [
                'pagination' => [
                    'total' => count($nests),
                    'count' => count($nests),
                    'per_page' => 50,
                    'current_page' => 1,
                    'total_pages' => 1,
                ],
            ],
        ]);

        foreach ($nests as $nest) {
            $response->assertJsonFragment([
                'object' => 'nest',
                'attributes' => $this->getTransformer(NestTransformer::class)->transform($nest),
            ]);
        }
    }

    /**
     * Test that getting a single nest returns the expected result.
     */
    public function test_single_nest_response()
    {
        $nest = Nest::factory()->create();

        $response = $this->getJson('/api/application/nests/'.$nest->id);
        $response->assertStatus(Response::HTTP_OK);
        $response->assertJsonStructure([
            'object',
            'attributes' => ['id', 'uuid', 'author', 'name', 'description', 'created_at', 'updated_at'],
        ]);

        $response->assertJson([
            'object' => 'nest',
            'attributes' => $this->getTransformer(NestTransformer::class)->transform($nest),
        ]);
    }

    /**
     * Test that including eggs in the response works as expected.
     */
    public function test_single_nest_with_eggs_included()
    {
        $nest = Nest::factory()->create();
        Egg::factory()->count(2)->create([
            'nest_id' => $nest->id,
            'author' => 'authors@reviactyl.app',
            'docker_images' => ['ghcr.io/reviactyl/images:java_21'],
            'config_files' => '[]',
            'config_startup' => '{"done":"Server marked as running"}',
            'config_logs' => '[]',
            'config_stop' => 'end',
        ]);
        $nest->loadMissing('eggs');

        $response = $this->getJson('/api/application/nests/'.$nest->id.'?include=servers,eggs');
        $response->assertStatus(Response::HTTP_OK);
        $response->assertJsonStructure([
            'object',
            'attributes' => [
                'relationships' => [
                    'eggs' => ['object', 'data' => []],
                    'servers' => ['object', 'data' => []],
                ],
            ],
        ]);

        $response->assertJsonCount(count($nest->getRelation('eggs')), 'attributes.relationships.eggs.data');
    }

    /**
     * Test that a missing nest returns a 404 error.
     */
    public function test_get_missing_nest()
    {
        $response = $this->getJson('/api/application/nests/nil');
        $this->assertNotFoundJson($response);
    }

    /**
     * Test that an authentication error occurs if a key does not have permission
     * to access a resource.
     */
    public function test_error_returned_if_no_permission()
    {
        $nest = Nest::factory()->create();
        $this->createNewDefaultApiKey($this->getApiUser(), ['r_nests' => 0]);

        $response = $this->getJson('/api/application/nests/'.$nest->id);
        $this->assertAccessDeniedJson($response);
    }
}
