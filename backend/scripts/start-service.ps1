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
    gateway  = "ququ-gateway\target\ququ-gateway-1.0.0-SNAPSHOT.jar"
    user     = "ququ-user-service\target\ququ-user-service-1.0.0-SNAPSHOT.jar"
    activity = "ququ-activity-service\target\ququ-activity-service-1.0.0-SNAPSHOT.jar"
    social   = "ququ-social-service\target\ququ-social-service-1.0.0-SNAPSHOT.jar"
    im       = "ququ-im-service\target\ququ-im-service-1.0.0-SNAPSHOT.jar"
    admin    = "ququ-admin-service\target\ququ-admin-service-1.0.0-SNAPSHOT.jar"
    ai       = "ququ-ai-service\target\ququ-ai-service-1.0.0-SNAPSHOT.jar"
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
