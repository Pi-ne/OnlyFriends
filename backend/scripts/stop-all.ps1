param(
    [switch]$All
)

$ErrorActionPreference = "Stop"
$BackendRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LogRoot = Join-Path $BackendRoot "logs"

$services = @("gateway", "user", "activity", "social", "im", "admin", "ai")
$ports = @{
    gateway = 8080
    user = 8081
    activity = 8082
    social = 8083
    im = 8084
    admin = 8085
    ai = 8001
}

foreach ($service in $services) {
    $pidFile = Join-Path $LogRoot "$service.pid"
    if (Test-Path $pidFile) {
        $servicePid = Get-Content $pidFile
        if ($servicePid) {
            Stop-Process -Id $servicePid -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped $service (PID=$servicePid)"
        }
        Remove-Item $pidFile -ErrorAction SilentlyContinue
    }
}

if ($All) {
    foreach ($port in $ports.Values) {
        $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        foreach ($c in $conn) {
            if ($c.OwningProcess -gt 0) {
                Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
                Write-Host "Stopped process on port $port (PID=$($c.OwningProcess))"
            }
        }
    }
    $imAlt = Get-NetTCPConnection -LocalPort 18084 -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $imAlt) {
        if ($c.OwningProcess -gt 0) {
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped IM alt port 18084 (PID=$($c.OwningProcess))"
        }
    }
}

Write-Host "Backend services stopped."
