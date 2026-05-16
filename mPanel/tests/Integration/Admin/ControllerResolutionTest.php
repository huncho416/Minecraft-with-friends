<?php

namespace Tests\Integration\Admin;

use App\Services\Activity\ActivityLogService;
use App\Services\Nodes\NodeCreationService;
use Tests\Integration\IntegrationTestCase;

class ControllerResolutionTest extends IntegrationTestCase
{
    public function test_activity_log_service_resolves()
    {
        $service = $this->app->make(ActivityLogService::class);
        $this->assertNotNull($service);
    }

    public function test_node_creation_service_resolves()
    {
        $service = $this->app->make(NodeCreationService::class);
        $this->assertNotNull($service);
    }
}
