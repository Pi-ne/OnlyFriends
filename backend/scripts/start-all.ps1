param(
    [switch]$WithAi,
    [switch]$Background
)

$ErrorActionPreference = "Stop"

$BackendRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $BackendRoot

. .\scripts\set-local-env.ps1

docker compose up -d redis
docker compose up -d --force-recreate mysql

if (-not (Test-Path "onlyfriends-gateway\target\onlyfriends-gateway-1.0.0-SNAPSHOT.jar")) {
    mvn -DskipTests package
}

$services = @("user", "activity", "social", "im", "admin", "gateway")
if ($WithAi) {
    $services += "ai"
}

$LogRoot = Join-Path $BackendRoot "logs"
if (-not (Test-Path $LogRoot)) {
    New-Item -ItemType Directory -Path $LogRoot | Out-Null
}

foreach ($service in $services) {
    $serviceScript = Join-Path $BackendRoot "scripts\start-service.ps1"

    if ($Background) {
        $stdout = Join-Path $LogRoot "$service.out.log"
        $stderr = Join-Path $LogRoot "$service.err.log"
        $pidFile = Join-Path $LogRoot "$service.pid"

        $process = Start-Process powershell -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $stdout `
            -RedirectStandardError $stderr `
            -ArgumentList @(
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", "`"$serviceScript`"",
                "-Service", $service
            )

        Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ASCII
        Write-Host "Started $service in background. PID=$($process.Id), log=$stdout"
    } else {
        Start-Process powershell -ArgumentList @(
            "-NoExit",
            "-ExecutionPolicy", "Bypass",
            "-File", "`"$serviceScript`"",
            "-Service", $service
        )
    }
    Start-Sleep -Seconds 2
}

if ($Background) {
    Write-Host "Started backend services in background."
    Write-Host "Logs: $LogRoot"
} else {
    Write-Host "Started backend services in separate PowerShell windows."
}
Write-Host "Gateway: http://localhost:8080"
