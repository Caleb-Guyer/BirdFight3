[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot
$pomPath = Join-Path $repoRoot "pom.xml"

if (-not (Test-Path $pomPath)) {
    throw "pom.xml not found: $pomPath"
}

[xml]$pom = Get-Content $pomPath
$namespace = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
$namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")

$artifactId = $pom.SelectSingleNode("/m:project/m:artifactId", $namespace).InnerText
$version = $pom.SelectSingleNode("/m:project/m:version", $namespace).InnerText
$distName = "$artifactId-$version"
$distRoot = Join-Path $repoRoot "target\$distName-dist"
$appRoot = Join-Path $distRoot $distName
$zipPath = Join-Path $repoRoot "target\$distName-dist.zip"
$libRoot = Join-Path $appRoot "lib"

$requiredPaths = @(
    $zipPath,
    (Join-Path $appRoot "README.md"),
    (Join-Path $appRoot "LICENSE"),
    (Join-Path $appRoot "bin\birdfight3.cmd"),
    (Join-Path $appRoot "bin\birdfight3.ps1"),
    (Join-Path $libRoot "$distName.jar")
)

foreach ($path in $requiredPaths) {
    if (-not (Test-Path $path)) {
        throw "Distribution file missing: $path"
    }
}

$mainJarPath = Join-Path $libRoot "$distName.jar"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jarArchive = [System.IO.Compression.ZipFile]::OpenRead($mainJarPath)
try {
    $manifestEntry = $jarArchive.GetEntry("META-INF/MANIFEST.MF")
    if (-not $manifestEntry) {
        throw "Distribution jar is missing META-INF/MANIFEST.MF: $mainJarPath"
    }
    $reader = New-Object System.IO.StreamReader($manifestEntry.Open())
    try {
        $manifest = $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
} finally {
    $jarArchive.Dispose()
}

if ($manifest -notmatch "(?m)^Implementation-Version: $([regex]::Escape($version))\r?$") {
    throw "Distribution jar Implementation-Version does not match pom.xml version $version."
}

$runtimeJars = @(Get-ChildItem -Path $libRoot -Filter "*.jar" -File)
if ($runtimeJars.Count -lt 8) {
    throw "Distribution lib directory has too few jars: $($runtimeJars.Count)"
}

$requiredJarPatterns = @(
    "javafx-controls-*-win.jar",
    "javafx-media-*-win.jar",
    "hid4java-*.jar",
    "jna-*.jar"
)

foreach ($pattern in $requiredJarPatterns) {
    if (-not (Get-ChildItem -Path $libRoot -Filter $pattern -File)) {
        throw "Distribution lib directory missing jar pattern: $pattern"
    }
}

$javaExe = "java"
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $candidate) {
        $javaExe = $candidate
    }
}

& $javaExe --enable-native-access=ALL-UNNAMED -cp (Join-Path $libRoot "*") com.example.birdgame3.Launcher --dry-run
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Distribution verified: $zipPath"
