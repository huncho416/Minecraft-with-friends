<?php

namespace Tests\Integration\Api\Application\Nests;

use App\Models\Egg;
use App\Models\Nest;
use Illuminate\Http\Response;
use Tests\Integration\Api\Application\ApplicationApiIntegrationTestCase;

class EggControllerTest extends ApplicationApiIntegrationTestCase
{
    private function createEgg(array $overrides = []): Egg
    {
        $nest = Nest::factory()->create();

        return Egg::factory()->create(array_merge([
            'nest_id' => $nest->id,
            'author' => 'authors@reviactyl.app',
            'docker_images' => ['ghcr.io/reviactyl/images:java_21'],
            'config_files' => '[]',
            'config_startup' => '{"done":"Server marked as running"}',
            'config_logs' => '[]',
            'config_stop' => 'end',
        ], $overrides));
    }

    /**
     * Test that all the eggs belonging to a given nest can be returned.
     */
    public function test_list_all_eggs_in_nest()
    {
        $firstEgg = $this->createEgg();
        $this->createEgg(['nest_id' => $firstEgg->nest_id]);
        $eggs = Egg::query()->where('nest_id', $firstEgg->nest_id)->get();

        $response = $this->getJson('/api/application/nests/'.$firstEgg->nest_id.'/eggs');
        $response->assertStatus(Response::HTTP_OK);
        $response->assertJsonCount(count($eggs), 'data');
        $response->assertJsonStructure([
            'object',
            'data' => [
                [
                    'object',
                    'attributes' => [
                        'id', 'uuid', 'nest', 'author', 'description', 'docker_image', 'startup', 'created_at', 'updated_at',
                        'script' => ['privileged', 'install', 'entry', 'container', 'extends'],
                        'config' => [
                            'files' => [],
                            'startup' => ['done'],
                            'stop',
                            'logs' => [],
                            'extends',
                        ],
                    ],
                ],
            ],
        ]);

        foreach (array_get($response->json(), 'data') as $datum) {
            $egg = $eggs->where('id', '=', $datum['attributes']['id'])->first();

            $this->assertNotNull($egg);
            $this->assertSame($egg->id, $datum['attributes']['id']);
            $this->assertSame($egg->name, $datum['attributes']['name']);
            $this->assertSame($egg->author, $datum['attributes']['author']);
        }
    }

    /**
     * Test that a single egg can be returned.
     */
    public function test_return_single_egg()
    {
        $egg = $this->createEgg();

        $response = $this->getJson('/api/application/nests/'.$egg->nest_id.'/eggs/'.$egg->id);
        $response->assertStatus(Response::HTTP_OK);
        $response->assertJsonStructure([
            'object',
            'attributes' => [
                'id', 'uuid', 'nest', 'author', 'description', 'docker_image', 'startup', 'script' => [], 'config' => [], 'created_at', 'updated_at',
            ],
        ]);

        $response->assertJson([
            'object' => 'egg',
            'attributes' => [
                'id' => $egg->id,
                'uuid' => $egg->uuid,
                'nest' => $egg->nest_id,
                'author' => $egg->author,
                'name' => $egg->name,
                'startup' => $egg->startup,
            ],
        ], true);
    }

    /**
     * Test that a single egg and all the defined relationships can be returned.
     */
    public function test_return_single_egg_with_relationships()
    {
        $egg = $this->createEgg();

        $response = $this->getJson('/api/application/nests/'.$egg->nest_id.'/eggs/'.$egg->id.'?include=servers,variables,nest');
        $response->assertStatus(Response::HTTP_OK);
        $response->assertJsonStructure([
            'object',
            'attributes' => [
                'relationships' => [
                    'nest' => ['object', 'attributes'],
                    'servers' => ['object', 'data' => []],
                    'variables' => ['object', 'data' => []],
                ],
            ],
        ]);
    }

    /**
     * Test that a missing egg returns a 404 error.
     */
    public function test_get_missing_egg()
    {
        $egg = $this->createEgg();

        $response = $this->getJson('/api/application/nests/'.$egg->nest_id.'/eggs/nil');
        $this->assertNotFoundJson($response);
    }

    /**
     * Test that an authentication error occurs if a key does not have permission
     * to access a resource.
     */
    public function test_error_returned_if_no_permission()
    {
        $egg = $this->createEgg();
        $this->createNewDefaultApiKey($this->getApiUser(), ['r_eggs' => 0]);

        $response = $this->getJson('/api/application/nests/'.$egg->nest_id.'/eggs');
        $this->assertAccessDeniedJson($response);
    }
}
