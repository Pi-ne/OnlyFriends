param(
    [switch]$Background,
    [switch]$Restart
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$FrontendRoot = Join-Path $RepoRoot "frontend"
$ServerScript = Join-Path $FrontendRoot "server.js"

if (-not (Test-Path $ServerScript)) {
    Write-Host "Frontend server script not found: $ServerScript"
    exit 1
}

$port = 5173
$existingListener = Get-NetTCPConnection -LocalAddress 127.0.0.1 -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($existingListener) {
    $existingPid = $existingListener.OwningProcess
    if ($Restart) {
        Write-Host "Port $port is already used by PID=$existingPid. Restarting frontend..."
        Stop-Process -Id $existingPid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    } else {
        Write-Host "Frontend already appears to be running on http://127.0.0.1:$port"
        Write-Host "PID=$existingPid"
        Write-Host "Open this URL in your browser, or run this to restart:"
        Write-Host ".\scripts\start-frontend.ps1 -Restart"
        exit 0
    }
}

if ($Background) {
    $LogRoot = Join-Path $RepoRoot "logs"
    if (-not (Test-Path $LogRoot)) {
        New-Item -ItemType Directory -Path $LogRoot | Out-Null
    }

    $stdout = Join-Path $LogRoot "frontend.out.log"
    $stderr = Join-Path $LogRoot "frontend.err.log"
    $pidFile = Join-Path $LogRoot "frontend.pid"

    $process = Start-Process node -WindowStyle Hidden -PassThru `
        -WorkingDirectory $FrontendRoot `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -ArgumentList "server.js"

    Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ASCII
    Write-Host "Started frontend in background. PID=$($process.Id)"
    Write-Host "URL: http://127.0.0.1:5173"
    Write-Host "Logs: $stdout"
} else {
    Set-Location $FrontendRoot
    Write-Host "Starting frontend: http://127.0.0.1:5173"
    node server.js
}
