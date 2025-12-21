$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

& (Join-Path $PSScriptRoot 'infra-up.ps1')

Write-Host 'Running unit tests + integration tests (Failsafe)...'
mvn -q verify

Write-Host 'Done.'
