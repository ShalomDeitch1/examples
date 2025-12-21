$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

Write-Host 'Starting Redis + Postgres (primary + replica)...'
docker compose up -d

Write-Host 'Waiting for Postgres primary to accept connections...'
while ($true) {
  try {
    docker exec optimized-postgres-primary pg_isready -U local -d local_delivery | Out-Null
    break
  } catch {
    Start-Sleep -Seconds 1
  }
}

Write-Host 'Waiting for Redis...'
while ($true) {
  $pong = ''
  try {
    $pong = docker exec optimized-redis redis-cli PING 2>$null
  } catch {
    $pong = ''
  }
  if ($pong -match 'PONG') { break }
  Start-Sleep -Seconds 1
}

Write-Host 'Infra is up:'
docker compose ps
