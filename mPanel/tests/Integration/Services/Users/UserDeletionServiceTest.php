<?php

namespace Tests\Integration\Services\Users;

use App\Exceptions\DisplayException;
use App\Models\User;
use App\Services\Users\UserDeletionService;
use Tests\Integration\IntegrationTestCase;

class UserDeletionServiceTest extends IntegrationTestCase
{
    public function test_exception_returned_if_user_assigned_to_servers(): void
    {
        $server = $this->createServerModel();

        $this->expectException(DisplayException::class);
        $this->expectExceptionMessage(__('admin/user.exceptions.user_has_servers'));

        $this->app->make(UserDeletionService::class)->handle($server->user);

        $this->assertModelExists($server->user);
    }

    public function test_user_is_deleted(): void
    {
        $user = User::factory()->create();

        $this->app->make(UserDeletionService::class)->handle($user);

        $this->assertModelMissing($user);
    }
}
