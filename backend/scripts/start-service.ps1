param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("gateway", "user", "activity", "social", "im", "admin", "ai")]
    [string]$Service
)

$ErrorActionPreference = "Stop"

$BackendRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $BackendRoot

. .\scripts\set-local-env.ps1

$JarMap = @{
    gateway  = "onlyfriends-gateway\target\onlyfriends-gateway-1.0.0-SNAPSHOT.jar"
    user     = "onlyfriends-user-service\target\onlyfriends-user-service-1.0.0-SNAPSHOT.jar"
    activity = "onlyfriends-activity-service\target\onlyfriends-activity-service-1.0.0-SNAPSHOT.jar"
    social   = "onlyfriends-social-service\target\onlyfriends-social-service-1.0.0-SNAPSHOT.jar"
    im       = "onlyfriends-im-service\target\onlyfriends-im-service-1.0.0-SNAPSHOT.jar"
    admin    = "onlyfriends-admin-service\target\onlyfriends-admin-service-1.0.0-SNAPSHOT.jar"
    ai       = "onlyfriends-ai-service\target\onlyfriends-ai-service-1.0.0-SNAPSHOT.jar"
}

$JarPath = $JarMap[$Service]
if (-not (Test-Path $JarPath)) {
    Write-Host "Jar not found: $JarPath"
    Write-Host "Run this first: mvn -DskipTests package"
    exit 1
}

$JavaExe = $null
if ($env:JAVA_HOME) {
    $CandidateJava = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $CandidateJava) {
        $JavaExe = $CandidateJava
    }
}

if (-not $JavaExe) {
    $JavaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($JavaCommand) {
        $JavaExe = $JavaCommand.Source
    }
}

if (-not $JavaExe) {
    Write-Host "Java runtime not found. Set JAVA_HOME to JDK 17+ or add java.exe to PATH."
    exit 1
}

Write-Host "Starting $Service service..."
Write-Host "Using Java: $JavaExe"
& $JavaExe -jar $JarPath
