$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

Write-Host 'Stopping containers (and removing volumes)...'
docker compose down -v
