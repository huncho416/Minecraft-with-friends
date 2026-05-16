<?php

namespace App\Filament\Resources\DatabaseHost\Pages;

use App\Filament\Resources\DatabaseHost\DatabaseHostResource;
use App\Services\Activity\ActivityLogService;
use Filament\Resources\Pages\CreateRecord;
use Illuminate\Database\Eloquent\Model;

class CreateDatabaseHost extends CreateRecord
{
    protected static string $resource = DatabaseHostResource::class;

    protected function handleRecordCreation(array $data): Model
    {
        $record = parent::handleRecordCreation($data);

        /** @var ActivityLogService $logService */
        $logService = app(ActivityLogService::class);
        $logService->subject($record)->event('server:database-host.create')->log();

        return $record;
    }

    protected function getRedirectUrl(): string
    {
        return $this->getResource()::getUrl('index');
    }
}
