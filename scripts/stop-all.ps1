$ErrorActionPreference = "Continue"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$PidFiles = @()
$PidFiles += Get-ChildItem -LiteralPath (Join-Path $RepoRoot "backend\logs") -Filter "*.pid" -ErrorAction SilentlyContinue
$PidFiles += Get-ChildItem -LiteralPath (Join-Path $RepoRoot "logs") -Filter "*.pid" -ErrorAction SilentlyContinue

if (-not $PidFiles -or $PidFiles.Count -eq 0) {
    Write-Host "No pid files found. If services were started in visible windows, close those windows manually."
    exit 0
}

foreach ($pidFile in $PidFiles) {
    $rawPid = Get-Content -LiteralPath $pidFile.FullName -ErrorAction SilentlyContinue | Select-Object -First 1
    $processId = 0
    if ([int]::TryParse($rawPid, [ref]$processId)) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping PID=$processId from $($pidFile.Name)"
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        }
    }
    Remove-Item -LiteralPath $pidFile.FullName -Force -ErrorAction SilentlyContinue
}

Write-Host "Stop command finished."
