param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$MysqlContainer = "",
    [string]$MysqlPassword = "onlyfriends_root_password",
    [switch]$SkipUnit,
    [switch]$SkipAi,
    [switch]$SkipSmoke,
    [switch]$SkipAdmin,
    [switch]$UnitOnly
)

$ErrorActionPreference = "Stop"
$BackendRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $BackendRoot

function Resolve-MysqlContainer {
    param([string]$Preferred)
    if (-not [string]::IsNullOrWhiteSpace($Preferred)) {
        return $Preferred
    }
    foreach ($name in @("onlyfriends-mysql", "ququ-mysql")) {
        $exists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $name }
        if ($exists) {
            return $name
        }
    }
    return "onlyfriends-mysql"
}

function Test-GatewayReady {
    param([string]$Url)
    try {
        $response = Invoke-RestMethod -Method Get -Uri "$Url/api/v1/activities?page=1&size=1" -TimeoutSec 5
        return ($response.code -eq 200)
    } catch {
        return $false
    }
}

$results = @()
function Add-Result {
    param([string]$Name, [bool]$Passed, [string]$Detail = "")
    $script:results += [pscustomobject]@{
        Name = $Name
        Passed = $Passed
        Detail = $Detail
    }
}

Write-Host "========================================"
Write-Host " OnlyFriends Full Test Suite"
Write-Host "========================================"
Write-Host ""

if (-not $SkipUnit) {
    Write-Host "[1/4] Java unit tests (mvn test)..."
    mvn test -q
    $unitExit = $LASTEXITCODE
    if ($unitExit -eq 0) {
        Add-Result -Name "Java unit tests" -Passed $true
        Write-Host "  PASS" -ForegroundColor Green
    } else {
        Add-Result -Name "Java unit tests" -Passed $false -Detail "mvn exit code $unitExit"
        Write-Host "  FAIL: mvn exit code $unitExit" -ForegroundColor Red
        if ($UnitOnly) {
            exit 1
        }
    }
    Write-Host ""
}

if ($UnitOnly) {
    Write-Host "Unit-only mode: skipped integration tests."
    exit 0
}

if (-not $SkipAi) {
    Write-Host "[2/4] AI Python tests (pytest)..."
    $aiPythonDir = Join-Path $BackendRoot "onlyfriends-ai-service\python"
    if (Test-Path $aiPythonDir) {
        Push-Location $aiPythonDir
        python -m pytest tests/ -q
        $aiExit = $LASTEXITCODE
        Pop-Location
        if ($aiExit -eq 0) {
            Add-Result -Name "AI Python tests" -Passed $true
            Write-Host "  PASS" -ForegroundColor Green
        } else {
            Add-Result -Name "AI Python tests" -Passed $false -Detail "pytest exit code $aiExit"
            Write-Host "  FAIL: pytest exit code $aiExit" -ForegroundColor Red
        }
    } else {
        Add-Result -Name "AI Python tests" -Passed $false -Detail "directory missing"
        Write-Host "  SKIP: AI Python directory missing" -ForegroundColor Yellow
    }
    Write-Host ""
}

if (-not $SkipSmoke) {
    Write-Host "[3/4] Smoke script validation..."
    $smokeScripts = @(
        "backend-smoke.ps1",
        "user-service\smoke-user-service.ps1",
        "activity-service\smoke-activity-service.ps1",
        "social-service\smoke-social-service.ps1",
        "im-service\smoke-im-service.ps1"
    )
    foreach ($script in $smokeScripts) {
        $path = Join-Path $PSScriptRoot $script
        if (-not (Test-Path $path)) {
            Add-Result -Name "validate $script" -Passed $false -Detail "file missing"
            continue
        }
        $validateExit = 0
        if ($script -eq "backend-smoke.ps1") {
            & $path -ValidateOnly | Out-Null
        } elseif ($script -like "activity-service*") {
            & $path -AccessToken "validate-only" -ValidateOnly | Out-Null
        } else {
            & $path -ValidateOnly | Out-Null
        }
        $validateExit = $LASTEXITCODE
        if ($validateExit -eq 0) {
            Add-Result -Name "validate $script" -Passed $true
        } else {
            Add-Result -Name "validate $script" -Passed $false -Detail "exit code $validateExit"
        }
    }
    Write-Host "  Smoke script validation done"
    Write-Host ""

    Write-Host "[4/4] Full integration smoke (backend-smoke.ps1)..."
    if (-not (Test-GatewayReady -Url $BaseUrl)) {
        Add-Result -Name "integration smoke" -Passed $false -Detail "gateway not ready: $BaseUrl"
        Write-Host "  SKIP: gateway not ready. Run start-all.ps1 first." -ForegroundColor Yellow
    } else {
        $container = Resolve-MysqlContainer -Preferred $MysqlContainer
        $smokeArgs = @{
            BaseUrl = $BaseUrl
            MysqlContainer = $container
            MysqlPassword = $MysqlPassword
        }
        if ($SkipAdmin) {
            $smokeArgs.SkipAdmin = $true
        }
        try {
            & (Join-Path $PSScriptRoot "backend-smoke.ps1") @smokeArgs
            Add-Result -Name "integration smoke" -Passed $true
            Write-Host "  PASS" -ForegroundColor Green
        } catch {
            Add-Result -Name "integration smoke" -Passed $false -Detail $_.Exception.Message
            Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    Write-Host ""
}

Write-Host "========================================"
Write-Host " Test Summary"
Write-Host "========================================"
$passed = @($results | Where-Object { $_.Passed }).Count
$failed = @($results | Where-Object { -not $_.Passed }).Count
foreach ($item in $results) {
    $status = if ($item.Passed) { "PASS" } else { "FAIL" }
    $color = if ($item.Passed) { "Green" } else { "Red" }
    $detail = if ($item.Detail) { " - $($item.Detail)" } else { "" }
    Write-Host ("  [{0}] {1}{2}" -f $status, $item.Name, $detail) -ForegroundColor $color
}
Write-Host ""
Write-Host ("Total: {0} passed, {1} failed" -f $passed, $failed)

if ($failed -gt 0) {
    exit 1
}
