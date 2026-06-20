Set-Location $PSScriptRoot\..\frontend
if (-not (Test-Path node_modules)) {
    Write-Host "Instalando dependencias..."
    npm install
}
Write-Host "Frontend -> API http://localhost:8080"
npm run dev
