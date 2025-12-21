$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

& (Join-Path $PSScriptRoot 'infra-up.ps1')

Write-Host 'Building app...'
mvn -q -DskipTests package

$jar = Get-ChildItem -Path .\target -Filter '*SNAPSHOT.jar' | Select-Object -First 1
if (-not $jar) {
  throw 'Could not find built jar under target/*SNAPSHOT.jar'
}

Write-Host "Starting app: $($jar.FullName)"
$proc = Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar.FullName) -PassThru

try {
  Write-Host 'Waiting for HTTP...'
  $uri = 'http://localhost:8097/items?lat=40.7128&lon=-74.0060'
  while ($true) {
    try {
      Invoke-WebRequest -Uri $uri -UseBasicParsing | Out-Null
      break
    } catch {
      Start-Sleep -Seconds 1
    }
  }

  Write-Host 'GET /items (should populate cache):'
  (Invoke-WebRequest -Uri $uri -UseBasicParsing).Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

  Write-Host 'Create order:'
  $body = @{ customerId = '20000000-0000-0000-0000-000000000001'; lines = @(@{ itemId = '10000000-0000-0000-0000-000000000001'; qty = 1 }) } | ConvertTo-Json -Depth 10
  $order = Invoke-RestMethod -Method Post -Uri 'http://localhost:8097/orders' -ContentType 'application/json' -Body $body
  $orderId = $order.orderId
  Write-Host "Created order: $orderId"

  Write-Host 'Confirm payment:'
  $confirm = Invoke-RestMethod -Method Post -Uri ("http://localhost:8097/orders/$orderId/confirm-payment") -ContentType 'application/json' -Body '{"success": true}'
  $confirm | ConvertTo-Json -Depth 10

  Write-Host 'Redis keys (sample):'
  docker exec optimized-redis redis-cli KEYS 'items:grid:*'
} finally {
  if ($proc -and -not $proc.HasExited) {
    Write-Host "Stopping app (pid=$($proc.Id))"
    Stop-Process -Id $proc.Id -Force
  }
}
