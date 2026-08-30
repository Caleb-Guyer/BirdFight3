<#
.SYNOPSIS
    Builds a self-contained Windows package of Bird Fight 3 with jpackage.

.DESCRIPTION
    Produces a double-clickable game that bundles its own trimmed Java runtime,
    so players do not need Java installed.

    Default output is a portable app-image folder plus a shareable zip:
        target\installer\out\Bird Fight 3\Bird Fight 3.exe
        target\BirdFight3-<version>-win.zip

    Pass -Type msi (or exe) for a real installer with Start Menu entry and
    shortcut; those types require the WiX Toolset (https://wixtoolset.org)
    on PATH, which jpackage uses to build Windows installers.

    How the packaging works: the JavaFX jars are true Java modules and get
    jlinked into the bundled runtime; hid4java and JNA are automatic modules
    (jlink can't consume those), so they ride the classpath next to the game
    jar. The Launcher class boots JavaFX from that split world.

.PARAMETER Type
    app-image (default, no extra tooling needed), msi, or exe.

.PARAMETER RunTests
    Run the full test suite during the Maven build (skipped by default).

.PARAMETER AppVersion
    Version stamped on the package. jpackage forbids -SNAPSHOT suffixes.

.EXAMPLE
    .\build-installer.ps1
    .\build-installer.ps1 -Type msi -AppVersion 1.2.0
#>
param(
    [ValidateSet("app-image", "msi", "exe")]
    [string]$Type = "app-image",
    [switch]$RunTests,
    [string]$AppVersion = ""
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# --- keep Maven, jpackage, and the release tag on one version ---
[xml]$pom = Get-Content "pom.xml"
$projectVersion = [string]$pom.project.version
$artifactId = [string]$pom.project.artifactId
$distName = "$artifactId-$projectVersion"
if ([string]::IsNullOrWhiteSpace($AppVersion)) {
    $AppVersion = $projectVersion
} elseif ($AppVersion -ne $projectVersion) {
    throw "AppVersion $AppVersion does not match pom.xml version $projectVersion."
}
if ($AppVersion -match "-SNAPSHOT$") {
    throw "jpackage requires a release version; pom.xml currently contains $AppVersion."
}

# --- locate a JDK that ships jpackage ---
$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome "bin\jpackage.exe"))) {
    $javaHome = $null
    if (Test-Path "$env:USERPROFILE\.jdks") {
        foreach ($candidate in Get-ChildItem "$env:USERPROFILE\.jdks" -Directory | Sort-Object Name -Descending) {
            if (Test-Path (Join-Path $candidate.FullName "bin\jpackage.exe")) {
                $javaHome = $candidate.FullName
                break
            }
        }
    }
}
if (-not $javaHome) {
    throw "No JDK with jpackage found. Set JAVA_HOME to a JDK 21+ install."
}
$env:JAVA_HOME = $javaHome
$jpackage = Join-Path $javaHome "bin\jpackage.exe"
Write-Host "Using JDK: $javaHome"

# Maven's old clean plugin is unreliable when removing a previous Windows
# app-image. Clear that known staging directory with native PowerShell first.
$staging = "target\installer"
if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }

# --- build the game ---
$mvnArgs = @("-q")
if (-not $RunTests) { $mvnArgs += "-DskipTests" }
$mvnArgs += @("clean", "package")
& .\mvnw.cmd @mvnArgs
if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }

# --- stage jpackage inputs: JavaFX modules apart from classpath jars ---
$libPath = "target\$distName-dist\$distName\lib"
$libDir = Get-Item $libPath -ErrorAction SilentlyContinue
if (-not $libDir) { throw "Distribution lib folder not found: $libPath" }

$appDir = New-Item -ItemType Directory -Path "$staging\app" -Force
$moduleDir = New-Item -ItemType Directory -Path "$staging\modules" -Force

$mainJar = $null
foreach ($jar in Get-ChildItem $libDir.FullName -Filter *.jar) {
    if ($jar.Name -like "javafx-*-win.jar") {
        # Real JavaFX modules (classes + Windows natives): jlinked into the runtime.
        Copy-Item $jar.FullName $moduleDir.FullName
    } elseif ($jar.Name -like "javafx-*.jar") {
        # Empty non-classifier JavaFX jars: skip entirely.
    } else {
        # Game jar + automatic modules (hid4java, jna): classpath.
        Copy-Item $jar.FullName $appDir.FullName
        if ($jar.Name -eq "$distName.jar") { $mainJar = $jar.Name }
    }
}
if (-not $mainJar) { throw "Main game jar not found in $($libDir.FullName)." }
Write-Host "Main jar: $mainJar"

# --- run jpackage ---
$outDir = "$staging\out"
$jpackageArgs = @(
    "--type", $Type,
    "--name", "Bird Fight 3",
    "--app-version", $AppVersion,
    "--vendor", "Caleb Guyer",
    "--input", $appDir.FullName,
    "--main-jar", $mainJar,
    "--main-class", "com.example.birdgame3.Launcher",
    "--module-path", $moduleDir.FullName,
    "--add-modules", "javafx.controls,javafx.media,java.se,jdk.unsupported",
    "--java-options", "--enable-native-access=javafx.media,javafx.graphics,ALL-UNNAMED",
    "--dest", $outDir
)
if ($Type -ne "app-image") {
    $jpackageArgs += @("--win-menu", "--win-shortcut", "--win-dir-chooser")
}
& $jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed (for msi/exe, is the WiX Toolset installed?)." }

# --- package the portable build for sharing ---
if ($Type -eq "app-image") {
    $image = Join-Path $outDir "Bird Fight 3"
    $zip = "target\BirdFight3-$AppVersion-win.zip"
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Compress-Archive -Path $image -DestinationPath $zip
    Write-Host ""
    Write-Host "Portable build : $image"
    Write-Host "Shareable zip  : $zip"
    Write-Host "Launch         : `"$image\Bird Fight 3.exe`""
} else {
    Write-Host ""
    Write-Host "Installer written to $outDir"
}
