$connections = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if (-not $connections) {
    Write-Host "Nada escuchando en el puerto 8080."
    exit 0
}

$pids = $connections.OwningProcess | Sort-Object -Unique
foreach ($pid in $pids) {
    try {
        $proc = Get-Process -Id $pid -ErrorAction Stop
        Write-Host "Deteniendo $($proc.ProcessName) (PID $pid)..."
        Stop-Process -Id $pid -Force
    } catch {
        Write-Host "No se pudo detener PID $pid : $_"
    }
}

Start-Sleep -Seconds 1
Write-Host "Puerto 8080 liberado."
