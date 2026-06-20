Set-Location $PSScriptRoot\..

$inUse = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($inUse) {
    Write-Host "El puerto 8080 esta en uso. Deteniendo proceso anterior..."
    & "$PSScriptRoot\stop-backend.ps1"
}

Write-Host ""
Write-Host "Iniciando FMPRO..."
Write-Host "  API:      http://localhost:8080"
Write-Host "  H2:       http://localhost:8080/h2-console"
Write-Host "  Datos:    .\data\fmpro"
Write-Host ""

.\gradlew.bat bootRun
