# Bring up the full MythicPvP network on Windows.
# Usage: .\up.ps1            -> full network
#        .\up.ps1 dev        -> dev variant
param(
    [ValidateSet('full','dev')]
    [string]$Variant = 'full'
)

$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$file = if ($Variant -eq 'dev') { 'docker-compose.dev.yml' } else { 'docker-compose.yml' }

Write-Host "[up] starting via $file"
docker compose -f $file up -d --build
docker compose -f $file ps
