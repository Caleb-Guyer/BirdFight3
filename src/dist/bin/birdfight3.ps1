[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GameArgs
)

$ErrorActionPreference = "Stop"

$appHome = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaExe = "java"
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $candidate) {
        $javaExe = $candidate
    }
}

$classPath = Join-Path $appHome "lib\*"
& $javaExe --enable-native-access=ALL-UNNAMED -cp $classPath com.example.birdgame3.Launcher @GameArgs
exit $LASTEXITCODE
