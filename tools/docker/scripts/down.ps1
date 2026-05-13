# Tear down the MythicPvP network on Windows.
# Usage: .\down.ps1                -> stop, keep volumes
#        .\down.ps1 dev            -> dev variant
#        .\down.ps1 full -Wipe     -> delete volumes too (destructive)
param(
    [ValidateSet('full','dev')]
    [string]$Variant = 'full',
    [switch]$Wipe
)

$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$file = if ($Variant -eq 'dev') { 'docker-compose.dev.yml' } else { 'docker-compose.yml' }

if ($Wipe) {
    Write-Host "[down] stopping $file and removing volumes (DESTRUCTIVE)"
    docker compose -f $file down -v
} else {
    Write-Host "[down] stopping $file (volumes preserved)"
    docker compose -f $file down
}
