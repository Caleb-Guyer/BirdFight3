[CmdletBinding()]
param(
    [string]$SoundRoot = "",
    [string]$OutputPath = "",
    [int]$ScanBytes = 262144,
    [switch]$FailOnFindings
)

$ErrorActionPreference = "Stop"
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot

if (-not $SoundRoot) {
    $SoundRoot = Join-Path $repoRoot "src\main\resources\sounds"
}

function Get-AsciiStrings {
    param(
        [Parameter(Mandatory = $true)]
        [byte[]]$Bytes,
        [int]$MinLength = 6
    )

    $builder = New-Object System.Text.StringBuilder
    $results = New-Object System.Collections.Generic.List[string]

    foreach ($byte in $Bytes) {
        if ($byte -ge 32 -and $byte -le 126) {
            [void]$builder.Append([char]$byte)
            continue
        }

        if ($builder.Length -ge $MinLength) {
            $results.Add($builder.ToString())
        }
        $builder.Clear() | Out-Null
    }

    if ($builder.Length -ge $MinLength) {
        $results.Add($builder.ToString())
    }

    return $results
}

function Read-ScanBytes {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [int]$MaxBytes = 262144
    )

    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $buffer = New-Object byte[] ([Math]::Min($stream.Length, $MaxBytes))
        $read = $stream.Read($buffer, 0, $buffer.Length)
        if ($read -lt $buffer.Length) {
            return $buffer[0..($read - 1)]
        }
        return $buffer
    } finally {
        $stream.Dispose()
    }
}

if (-not (Test-Path $SoundRoot)) {
    throw "Sound directory not found: $SoundRoot"
}

$domainPattern = '(?i)\b(?:[a-z0-9-]{2,}\.)+(?:com|org|net|io|gg|tv|fm|co|us|dev|app|me|edu|gov)\b'
$files = Get-ChildItem -Path $SoundRoot -File | Sort-Object Name
$findings = @()
$totalBytes = 0L

foreach ($file in $files) {
    $totalBytes += $file.Length
    $bytes = Read-ScanBytes -Path $file.FullName -MaxBytes $ScanBytes
    $strings = Get-AsciiStrings -Bytes $bytes
    $domains = $strings |
        Select-String -Pattern $domainPattern -AllMatches |
        ForEach-Object { $_.Matches.Value.ToLowerInvariant() } |
        Sort-Object -Unique

    if ($domains.Count -eq 0) {
        continue
    }

    $findings += [PSCustomObject]@{
        Name = $file.Name
        SizeKB = [math]::Round($file.Length / 1KB, 1)
        Domains = ($domains -join ", ")
    }
}

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Audio Audit")
$report.Add("")
$report.Add("- Scan date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
$report.Add('- Sound directory: `' + $SoundRoot + '`')
$report.Add("- Files scanned: $($files.Count)")
$report.Add("- Bytes scanned per file: $ScanBytes")
$report.Add("- Total size: $([math]::Round($totalBytes / 1MB, 2)) MB")
$report.Add("- Files with embedded hostnames: $($findings.Count)")
$report.Add("")
$report.Add("This audit looks for embedded hostname-like strings inside bundled audio files. Hostname hits are not proof of ownership, but they are a release blocker until the asset source and license are verified.")
$report.Add("")

if ($findings.Count -eq 0) {
    $report.Add("No embedded hostnames were detected.")
} else {
    $report.Add("| File | Size KB | Embedded hostnames |")
    $report.Add("| --- | ---: | --- |")
    foreach ($finding in $findings) {
        $report.Add("| $($finding.Name) | $($finding.SizeKB) | $($finding.Domains) |")
    }
    $report.Add("")
    $report.Add("Recommended next steps:")
    $report.Add("1. Replace each flagged file with an asset that has a recorded source and license.")
    $report.Add("2. Add an asset manifest that maps every shipped sound to its source, license, and modification status.")
    $report.Add("3. Re-run this audit before any public release.")
}

$reportText = ($report -join [Environment]::NewLine) + [Environment]::NewLine

if ($OutputPath) {
    $outputDirectory = Split-Path -Parent $OutputPath
    if ($outputDirectory) {
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    }
    Set-Content -Path $OutputPath -Value $reportText -Encoding UTF8
    Write-Host "Audio audit written to $OutputPath"
}

Write-Host $reportText

if ($FailOnFindings -and $findings.Count -gt 0) {
    Write-Error "Bundled audio audit found $($findings.Count) file(s) with embedded hostnames."
}
