[CmdletBinding()]
param(
    [string]$OutputPath,
    [string]$FfmpegPath,
    [ValidateRange(24, 60)]
    [int]$FrameRate = 60,
    [switch]$SkipRender,
    [switch]$KeepIntermediate
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $projectRoot "release-media\BirdFight3-Full-Release-Trailer-1080p$FrameRate.mp4"
}
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)
$outputDirectory = Split-Path -Parent $OutputPath
$outputStem = [System.IO.Path]::GetFileNameWithoutExtension($OutputPath)
$silentVideo = Join-Path $outputDirectory ($outputStem + '.silent.mp4')
$encodeLog = $silentVideo + '.encode.log'

function Resolve-FfmpegExecutable {
    param([string]$RequestedPath)

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        $resolved = Resolve-Path -LiteralPath $RequestedPath -ErrorAction Stop
        return $resolved.Path
    }

    $onPath = Get-Command 'ffmpeg.exe' -ErrorAction SilentlyContinue
    if ($null -ne $onPath) {
        return $onPath.Source
    }

    $bundled = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'target\tools\ffmpeg') `
        -Recurse -File -Filter 'ffmpeg.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $bundled) {
        return $bundled.FullName
    }

    throw 'FFmpeg was not found. Pass -FfmpegPath or place a Windows build under target\tools\ffmpeg.'
}

function Invoke-CheckedProcess {
    param(
        [Parameter(Mandatory)]
        [string]$Executable,
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [string]$FailureMessage = 'External process failed.'
    )

    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FailureMessage Exit code: $LASTEXITCODE"
    }
}

function Resolve-JavaHome {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME) -and
        (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe') -PathType Leaf)) {
        return $env:JAVA_HOME
    }

    $candidates = @()
    $jdkRoots = @((Join-Path $env:USERPROFILE '.jdks'), 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium')
    foreach ($root in $jdkRoots) {
        if (Test-Path -LiteralPath $root -PathType Container) {
            $candidates += Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue
        }
    }
    if (Test-Path -LiteralPath 'C:\Program Files\JetBrains' -PathType Container) {
        $candidates += Get-ChildItem -LiteralPath 'C:\Program Files\JetBrains' -Directory -ErrorAction SilentlyContinue |
            ForEach-Object { Get-Item -LiteralPath (Join-Path $_.FullName 'jbr') -ErrorAction SilentlyContinue }
    }

    $javaHome = $candidates |
        Where-Object { $null -ne $_ -and (Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') -PathType Leaf) } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $javaHome) {
        throw 'A JDK 21 or newer was not found. Set JAVA_HOME before exporting the trailer.'
    }
    return $javaHome.FullName
}

$ffmpeg = Resolve-FfmpegExecutable -RequestedPath $FfmpegPath
$ffprobe = Join-Path (Split-Path -Parent $ffmpeg) 'ffprobe.exe'
if (-not (Test-Path -LiteralPath $ffprobe -PathType Leaf)) {
    throw "ffprobe.exe was not found beside ffmpeg.exe: $ffprobe"
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

if (-not $SkipRender) {
    $previousJavaHome = $env:JAVA_HOME
    $previousFfmpeg = $env:BIRDFIGHT3_TRAILER_FFMPEG
    $previousExport = $env:BIRDFIGHT3_OFFICIAL_TRAILER_EXPORT
    $previousFps = $env:BIRDFIGHT3_TRAILER_EXPORT_FPS
    try {
        $env:JAVA_HOME = Resolve-JavaHome
        $env:BIRDFIGHT3_TRAILER_FFMPEG = $ffmpeg
        $env:BIRDFIGHT3_OFFICIAL_TRAILER_EXPORT = $silentVideo
        $env:BIRDFIGHT3_TRAILER_EXPORT_FPS = $FrameRate.ToString([Globalization.CultureInfo]::InvariantCulture)
        Push-Location $projectRoot
        try {
            Invoke-CheckedProcess -Executable (Join-Path $projectRoot 'mvnw.cmd') `
                -Arguments @('-q', '-DskipTests', 'javafx:run') `
                -FailureMessage "The standalone trailer renderer failed. Inspect $encodeLog."
        } finally {
            Pop-Location
        }
    } finally {
        $env:JAVA_HOME = $previousJavaHome
        $env:BIRDFIGHT3_TRAILER_FFMPEG = $previousFfmpeg
        $env:BIRDFIGHT3_OFFICIAL_TRAILER_EXPORT = $previousExport
        $env:BIRDFIGHT3_TRAILER_EXPORT_FPS = $previousFps
    }
}

if (-not (Test-Path -LiteralPath $silentVideo -PathType Leaf)) {
    throw "The silent trailer master was not created: $silentVideo"
}

$sounds = Join-Path $projectRoot 'src\main\resources\sounds'
$audioInputs = @(
    'music-farewell.mp3',
    'music-grinch-bellkeeper.mp3',
    'music-farewell.mp3',
    'sfx-boom.wav',
    'sfx-whoosh.wav',
    'sfx-impact-heavy.wav',
    'sfx-shatter.wav',
    'sfx-bigwave.wav',
    'sfx-thwump.wav',
    'sfx-rebirth-nova.wav',
    'sfx-fallwhistle.wav',
    'sfx-fighter-ready.wav'
)

$ffmpegArguments = @('-hide_banner', '-loglevel', 'warning', '-i', $silentVideo)
foreach ($audioInput in $audioInputs) {
    $ffmpegArguments += @('-i', (Join-Path $sounds $audioInput))
}

# The story cut starts with the campaign's farewell score, moves onto a measured
# 150 BPM battle cue at 27.825 seconds (its first beat lands at 28.000), then
# returns to an independently decoded passage of the farewell theme for Eagle's
# final choice. Keeping the outro on its own input prevents an early-ended split
# branch from leaving the final title card silent.
$filterGraph = @'
[1:a]atrim=start=0:end=30.0,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=1.6,afade=t=out:st=25.8:d=4.2,volume=0.54[sad];
[2:a]atrim=start=0:end=55.2,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=0.35,afade=t=out:st=52.975:d=2.225,volume=0.62,adelay=27825|27825[battle];
[3:a]atrim=start=45.0:end=62.2,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=1.2,afade=t=out:st=15.6:d=1.6,volume=0.68,adelay=80800|80800[farewell];
[4:a]asplit=5[boom0][boom1][boom2][boom3][boom4];
[boom0]adelay=12000|12000,volume=0.72[b0];
[boom1]adelay=28000|28000,volume=0.92[b1];
[boom2]adelay=54000|54000,volume=0.96[b2];
[boom3]adelay=70000|70000,volume=1.00[b3];
[boom4]adelay=90800|90800,volume=0.92[b4];
[5:a]asplit=9[whoosh0][whoosh1][whoosh2][whoosh3][whoosh4][whoosh5][whoosh6][whoosh7][whoosh8];
[whoosh0]adelay=27880|27880,volume=0.62[w0];
[whoosh1]adelay=31100|31100,volume=0.70[w1];
[whoosh2]adelay=34300|34300,volume=0.70[w2];
[whoosh3]adelay=53900|53900,volume=0.72[w3];
[whoosh4]adelay=56300|56300,volume=0.72[w4];
[whoosh5]adelay=58700|58700,volume=0.74[w5];
[whoosh6]adelay=69900|69900,volume=0.80[w6];
[whoosh7]adelay=73100|73100,volume=0.80[w7];
[whoosh8]adelay=79500|79500,volume=0.86[w8];
[6:a]asplit=11[impact0][impact1][impact2][impact3][impact4][impact5][impact6][impact7][impact8][impact9][impact10];
[impact0]adelay=28000|28000,volume=0.86[i0];
[impact1]adelay=31200|31200,volume=0.90[i1];
[impact2]adelay=34400|34400,volume=0.90[i2];
[impact3]adelay=54000|54000,volume=0.90[i3];
[impact4]adelay=56400|56400,volume=0.88[i4];
[impact5]adelay=58800|58800,volume=0.90[i5];
[impact6]adelay=61200|61200,volume=0.94[i6];
[impact7]adelay=70000|70000,volume=0.94[i7];
[impact8]adelay=73200|73200,volume=0.96[i8];
[impact9]adelay=76400|76400,volume=1.00[i9];
[impact10]adelay=79600|79600,volume=1.00[i10];
[7:a]asplit=2[shatter0][shatter1];
[shatter0]adelay=12000|12000,volume=0.72[s0];
[shatter1]adelay=82800|82800,volume=0.64[s1];
[8:a]asplit=2[bigwave0][bigwave1];
[bigwave0]adelay=63600|63600,volume=0.78[g0];
[bigwave1]adelay=70000|70000,volume=0.88[g1];
[9:a]asplit=4[thwump0][thwump1][thwump2][thwump3];
[thwump0]adelay=46000|46000,volume=0.70[t0];
[thwump1]adelay=48000|48000,volume=0.72[t1];
[thwump2]adelay=50000|50000,volume=0.76[t2];
[thwump3]adelay=52000|52000,volume=0.82[t3];
[10:a]adelay=69600|69600,volume=0.78[nova];
[11:a]adelay=89150|89150,volume=0.34[fall];
[12:a]adelay=90600|90600,volume=0.64[ready];
[sad][battle][farewell][b0][b1][b2][b3][b4][w0][w1][w2][w3][w4][w5][w6][w7][w8][i0][i1][i2][i3][i4][i5][i6][i7][i8][i9][i10][s0][s1][g0][g1][t0][t1][t2][t3][nova][fall][ready]amix=inputs=39:normalize=0:dropout_transition=0,apad=pad_dur=99,aformat=sample_rates=48000:channel_layouts=stereo,loudnorm=I=-14:TP=-1.0:LRA=11,volume=-1.5dB,alimiter=limit=0.749894:attack=5:release=80:level=0,atrim=duration=98.0,asetpts=N/SR/TB[aout]
'@

$ffmpegArguments += @(
    '-filter_complex', $filterGraph,
    '-map', '0:v:0',
    '-map', '[aout]',
    '-c:v', 'copy',
    '-c:a', 'aac',
    '-b:a', '320k',
    '-ar', '48000',
    '-movflags', '+faststart',
    '-metadata', 'title=Bird Fight 3 - Full Release Trailer',
    '-metadata', 'artist=Caleb Guyer',
    '-metadata', 'comment=Official Bird Fight 3 story trailer - The Still Sky',
    '-metadata', 'date=2026',
    '-y',
    $OutputPath
)

Invoke-CheckedProcess -Executable $ffmpeg -Arguments $ffmpegArguments `
    -FailureMessage 'Trailer sound design and final mux failed.'

$probeArguments = @(
    '-v', 'error',
    '-show_entries', 'format=duration,size,bit_rate:stream=index,codec_name,codec_type,width,height,r_frame_rate,pix_fmt,sample_rate,channels',
    '-of', 'json',
    $OutputPath
)
Invoke-CheckedProcess -Executable $ffprobe -Arguments $probeArguments `
    -FailureMessage 'Trailer verification failed.'

if (-not $KeepIntermediate) {
    foreach ($generatedFile in @($silentVideo, $encodeLog)) {
        if (Test-Path -LiteralPath $generatedFile -PathType Leaf) {
            Remove-Item -LiteralPath $generatedFile -Force
        }
    }
}

Write-Host "Upload-ready trailer: $OutputPath"
