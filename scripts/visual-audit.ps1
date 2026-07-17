[CmdletBinding()]
param(
    [switch]$FailOnFindings
)

$ErrorActionPreference = "Stop"
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot

Push-Location $repoRoot
try {
    $mavenArgs = @("-B", "-ntp", "test", "-Dtest=BirdVisualAuditTest,BirdVisualAuditRun")
    if ($FailOnFindings) {
        $mavenArgs += "-DvisualAudit.failOnFindings=true"
    }
    & (Join-Path $repoRoot "mvnw.cmd") $mavenArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    Write-Host "Visual audit written to $(Join-Path $repoRoot 'audit\visual')"
} finally {
    Pop-Location
}
