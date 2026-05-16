<?php

namespace Tests\Unit\Services\Helpers;

use App\Services\Helpers\GeoLocaleService;
use Tests\TestCase;

class GeoLocaleServiceTest extends TestCase
{
    private GeoLocaleService $service;

    private array $availableLocales;

    protected function setUp(): void
    {
        parent::setUp();
        $this->service = new GeoLocaleService();
        $this->availableLocales = ['ar', 'de', 'en', 'es', 'fr', 'hi', 'id', 'kn', 'pt', 'sv', 'tr', 'zh'];
    }

    public function test_it_resolves_english_for_us()
    {
        $this->assertEquals('en', $this->service->resolveLocale('US', $this->availableLocales));
    }

    public function test_it_resolves_english_for_gb()
    {
        $this->assertEquals('en', $this->service->resolveLocale('GB', $this->availableLocales));
    }

    public function test_it_resolves_german_for_de()
    {
        $this->assertEquals('de', $this->service->resolveLocale('DE', $this->availableLocales));
    }

    public function test_it_resolves_german_for_austria()
    {
        $this->assertEquals('de', $this->service->resolveLocale('AT', $this->availableLocales));
    }

    public function test_it_resolves_french_for_fr()
    {
        $this->assertEquals('fr', $this->service->resolveLocale('FR', $this->availableLocales));
    }

    public function test_it_resolves_spanish_for_es()
    {
        $this->assertEquals('es', $this->service->resolveLocale('ES', $this->availableLocales));
    }

    public function test_it_resolves_spanish_for_mexico()
    {
        $this->assertEquals('es', $this->service->resolveLocale('MX', $this->availableLocales));
    }

    public function test_it_resolves_portuguese_for_brazil()
    {
        $this->assertEquals('pt', $this->service->resolveLocale('BR', $this->availableLocales));
    }

    public function test_it_resolves_portuguese_for_portugal()
    {
        $this->assertEquals('pt', $this->service->resolveLocale('PT', $this->availableLocales));
    }

    public function test_it_resolves_arabic_for_saudi_arabia()
    {
        $this->assertEquals('ar', $this->service->resolveLocale('SA', $this->availableLocales));
    }

    public function test_it_resolves_arabic_for_egypt()
    {
        $this->assertEquals('ar', $this->service->resolveLocale('EG', $this->availableLocales));
    }

    public function test_it_resolves_hindi_for_india()
    {
        $this->assertEquals('hi', $this->service->resolveLocale('IN', $this->availableLocales));
    }

    public function test_it_resolves_indonesian_for_indonesia()
    {
        $this->assertEquals('id', $this->service->resolveLocale('ID', $this->availableLocales));
    }

    public function test_it_resolves_swedish_for_sweden()
    {
        $this->assertEquals('sv', $this->service->resolveLocale('SE', $this->availableLocales));
    }

    public function test_it_resolves_turkish_for_turkey()
    {
        $this->assertEquals('tr', $this->service->resolveLocale('TR', $this->availableLocales));
    }

    public function test_it_resolves_chinese_for_china()
    {
        $this->assertEquals('zh', $this->service->resolveLocale('CN', $this->availableLocales));
    }

    public function test_it_resolves_chinese_for_taiwan()
    {
        $this->assertEquals('zh', $this->service->resolveLocale('TW', $this->availableLocales));
    }

    public function test_it_returns_null_for_unmapped_country()
    {
        $this->assertNull($this->service->resolveLocale('XX', $this->availableLocales));
    }

    public function test_it_returns_null_when_mapped_locale_is_not_available()
    {
        $limitedLocales = ['en', 'fr'];
        $this->assertNull($this->service->resolveLocale('DE', $limitedLocales));
    }

    public function test_it_is_case_insensitive_for_country_codes()
    {
        $this->assertEquals('fr', $this->service->resolveLocale('fr', $this->availableLocales));
        $this->assertEquals('de', $this->service->resolveLocale('de', $this->availableLocales));
    }

    public function test_it_returns_null_for_empty_available_locales()
    {
        $this->assertNull($this->service->resolveLocale('US', []));
    }
}
