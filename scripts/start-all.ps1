param(
    [switch]$WithAi,
    [switch]$Background
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$BackendRoot = Join-Path $RepoRoot "backend"

if (-not (Test-Path (Join-Path $BackendRoot "scripts\start-all.ps1"))) {
    Write-Host "Backend start script not found under: $BackendRoot"
    exit 1
}

Set-Location $BackendRoot

$argsToPass = @()
if ($WithAi) {
    $argsToPass += "-WithAi"
}
if ($Background) {
    $argsToPass += "-Background"
}

& .\scripts\start-all.ps1 @argsToPass
