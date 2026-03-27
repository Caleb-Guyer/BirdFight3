[CmdletBinding()]
param(
    [switch]$NoAudioAudit
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot

function Get-JavaMajor {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaExe
    )

    if (-not (Test-Path $JavaExe)) {
        return $null
    }

    $previousErrorPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $versionOutput = & $JavaExe -version 2>&1 | Out-String
    } finally {
        $ErrorActionPreference = $previousErrorPreference
    }

    if ($LASTEXITCODE -ne 0) {
        return $null
    }

    if ($versionOutput -match '"(?<version>\d+(?:\.\d+){0,2}(?:_[0-9]+)?)') {
        $versionText = $matches.version
        if ($versionText.StartsWith("1.")) {
            return [int]($versionText.Split(".")[1])
        }
        return [int](($versionText -split "[._]")[0])
    }

    return $null
}

function Get-JavaHomeCandidates {
    $roots = @(
        (Join-Path $HOME ".jdks"),
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium"
    )

    $candidates = @()
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) {
            continue
        }

        foreach ($dir in Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue) {
            $javaExe = Join-Path $dir.FullName "bin\java.exe"
            $major = Get-JavaMajor -JavaExe $javaExe
            if ($major -ge 21) {
                $candidates += [PSCustomObject]@{
                    JavaHome = $dir.FullName
                    Major = $major
                }
            }
        }
    }

    return $candidates | Sort-Object `
        @{ Expression = "Major"; Descending = $true }, `
        @{ Expression = "JavaHome"; Descending = $true }
}

function Resolve-JavaHome {
    if ($env:JAVA_HOME) {
        $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
        if ((Get-JavaMajor -JavaExe $javaExe) -ge 21) {
            return $env:JAVA_HOME
        }
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $candidateHome = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
        $javaExe = Join-Path $candidateHome "bin\java.exe"
        if ((Get-JavaMajor -JavaExe $javaExe) -ge 21) {
            return $candidateHome
        }
    }

    $candidates = @(Get-JavaHomeCandidates)
    if ($candidates.Count -gt 0) {
        return $candidates[0].JavaHome
    }

    return $null
}

$resolvedJavaHome = Resolve-JavaHome
if (-not $resolvedJavaHome) {
    throw "No JDK 21+ installation was found. Set JAVA_HOME before running tests."
}

$env:JAVA_HOME = $resolvedJavaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$javaMajor = Get-JavaMajor -JavaExe (Join-Path $env:JAVA_HOME "bin\java.exe")
if ($javaMajor -lt 21) {
    throw "JAVA_HOME points to JDK $javaMajor. Bird Fight 3 requires JDK 21 or newer."
}

Write-Host "Using JAVA_HOME=$env:JAVA_HOME"

& (Join-Path $repoRoot "mvnw.cmd") -B -ntp test
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not $NoAudioAudit) {
    & (Join-Path $scriptRoot "audit-sounds.ps1") -OutputPath (Join-Path $repoRoot "audit\audio-audit.md")
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
