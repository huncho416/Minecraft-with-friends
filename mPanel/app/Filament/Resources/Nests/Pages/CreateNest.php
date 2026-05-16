<?php

namespace App\Filament\Resources\Nests\Pages;

use App\Filament\Resources\Nests\NestResource;
use Filament\Resources\Pages\CreateRecord;
use Illuminate\Support\Str;

class CreateNest extends CreateRecord
{
    protected static string $resource = NestResource::class;

    protected function mutateFormDataBeforeCreate(array $data): array
    {
        $data['uuid'] = Str::uuid()->toString();
        $data['author'] = $data['author'] ?? config('panel.service.author');

        return $data;
    }
}
