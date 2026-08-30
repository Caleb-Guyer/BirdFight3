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
    'music-escape.mp3',
    'music-farewell.mp3',
    'sfx-boom.wav',
    'sfx-whoosh.wav',
    'sfx-impact-heavy.wav',
    'sfx-shatter.wav',
    'sfx-bigwave.wav',
    'sfx-thwump.wav',
    'sfx-rebirth-nova.wav',
    'sfx-fallwhistle.wav',
    'sfx-fighter-ready.wav',
    'sfx-impact-medium.wav',
    'sfx-impact-light.wav',
    'sfx-hit.wav',
    'sfx-swing-heavy.wav',
    'sfx-swing-light.wav',
    'sfx-shield-block.wav',
    'sfx-shield-parry.wav',
    'sfx-shield-break.wav',
    'sfx-launch-tail.wav'
)

$ffmpegArguments = @('-hide_banner', '-loglevel', 'warning', '-i', $silentVideo)
foreach ($audioInput in $audioInputs) {
    $ffmpegArguments += @('-i', (Join-Path $sounds $audioInput))
}

# The story cut starts with the campaign's farewell score, breaks into the
# faster Chase cue for the gameplay showcase (its first strong beat lands at
# 27.000), then returns to an independently decoded passage of the farewell
# theme for Eagle's final choice. Fades stay before adelay: applying them after
# a delay would place their timestamps in the wrong part of the stream and can
# silently erase the outro.
$filterGraph = @'
[1:a]atrim=start=0:end=27.0,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=1.6,afade=t=out:st=23.7:d=3.3,volume=0.54[sad];
[2:a]atrim=start=0:end=53.17,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=0.20,afade=t=out:st=50.17:d=3.0,volume=0.64,adelay=26750|26750[battle];
[3:a]atrim=start=45.0:end=63.08,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=1.2,afade=t=out:st=16.5:d=1.58,volume=0.68,adelay=79920|79920[farewell];
[4:a]asplit=5[boom0][boom1][boom2][boom3][boom4];
[boom0]adelay=12000|12000,volume=0.72[b0];
[boom1]adelay=27000|27000,volume=0.90[b1];
[boom2]adelay=56400|56400,volume=0.90[b2];
[boom3]adelay=79920|79920,volume=0.96[b3];
[boom4]adelay=90800|90800,volume=0.92[b4];
[5:a]asplit=8[whoosh0][whoosh1][whoosh2][whoosh3][whoosh4][whoosh5][whoosh6][whoosh7];
[whoosh0]adelay=27040|27040,volume=0.48[w0];
[whoosh1]adelay=32920|32920,volume=0.50[w1];
[whoosh2]adelay=38800|38800,volume=0.50[w2];
[whoosh3]adelay=44680|44680,volume=0.52[w3];
[whoosh4]adelay=56440|56440,volume=0.52[w4];
[whoosh5]adelay=62320|62320,volume=0.54[w5];
[whoosh6]adelay=68200|68200,volume=0.56[w6];
[whoosh7]adelay=74080|74080,volume=0.60[w7];
[6:a]asplit=8[impact0][impact1][impact2][impact3][impact4][impact5][impact6][impact7];
[impact0]adelay=31140|31140,volume=0.60[i0];
[impact1]adelay=37020|37020,volume=0.62[i1];
[impact2]adelay=42900|42900,volume=0.62[i2];
[impact3]adelay=48780|48780,volume=0.66[i3];
[impact4]adelay=60540|60540,volume=0.66[i4];
[impact5]adelay=66420|66420,volume=0.68[i5];
[impact6]adelay=72300|72300,volume=0.72[i6];
[impact7]adelay=78180|78180,volume=0.78[i7];
[7:a]asplit=2[shatter0][shatter1];
[shatter0]adelay=12000|12000,volume=0.72[s0];
[shatter1]adelay=80000|80000,volume=0.64[s1];
[8:a]asplit=2[bigwave0][bigwave1];
[bigwave0]adelay=68160|68160,volume=0.72[g0];
[bigwave1]adelay=79920|79920,volume=0.82[g1];
[9:a]asplit=3[thwump0][thwump1][thwump2];
[thwump0]adelay=51500|51500,volume=0.62[t0];
[thwump1]adelay=53300|53300,volume=0.68[t1];
[thwump2]adelay=55100|55100,volume=0.76[t2];
[10:a]adelay=68160|68160,volume=0.70[nova];
[11:a]adelay=89150|89150,volume=0.34[fall];
[12:a]adelay=90600|90600,volume=0.64[ready];
[13:a]asplit=8[medium0][medium1][medium2][medium3][medium4][medium5][medium6][medium7];
[medium0]adelay=27700|27700,volume=0.56[m0];
[medium1]adelay=33580|33580,volume=0.58[m1];
[medium2]adelay=39460|39460,volume=0.58[m2];
[medium3]adelay=45340|45340,volume=0.60[m3];
[medium4]adelay=57100|57100,volume=0.60[m4];
[medium5]adelay=62980|62980,volume=0.62[m5];
[medium6]adelay=68860|68860,volume=0.66[m6];
[medium7]adelay=74740|74740,volume=0.70[m7];
[14:a]asplit=8[light0][light1][light2][light3][light4][light5][light6][light7];
[light0]adelay=28580|28580,volume=0.42[l0];
[light1]adelay=34460|34460,volume=0.44[l1];
[light2]adelay=40340|40340,volume=0.44[l2];
[light3]adelay=46220|46220,volume=0.46[l3];
[light4]adelay=57980|57980,volume=0.46[l4];
[light5]adelay=63860|63860,volume=0.48[l5];
[light6]adelay=69740|69740,volume=0.50[l6];
[light7]adelay=75620|75620,volume=0.54[l7];
[15:a]asplit=8[hit0][hit1][hit2][hit3][hit4][hit5][hit6][hit7];
[hit0]adelay=31700|31700,volume=0.42[h0];
[hit1]adelay=37580|37580,volume=0.44[h1];
[hit2]adelay=43460|43460,volume=0.44[h2];
[hit3]adelay=49340|49340,volume=0.46[h3];
[hit4]adelay=61100|61100,volume=0.46[h4];
[hit5]adelay=66980|66980,volume=0.48[h5];
[hit6]adelay=72860|72860,volume=0.50[h6];
[hit7]adelay=78740|78740,volume=0.54[h7];
[16:a]asplit=8[swingheavy0][swingheavy1][swingheavy2][swingheavy3][swingheavy4][swingheavy5][swingheavy6][swingheavy7];
[swingheavy0]adelay=30440|30440,volume=0.54[sh0];
[swingheavy1]adelay=36320|36320,volume=0.56[sh1];
[swingheavy2]adelay=42200|42200,volume=0.56[sh2];
[swingheavy3]adelay=48080|48080,volume=0.58[sh3];
[swingheavy4]adelay=59840|59840,volume=0.58[sh4];
[swingheavy5]adelay=65720|65720,volume=0.60[sh5];
[swingheavy6]adelay=71600|71600,volume=0.64[sh6];
[swingheavy7]adelay=77480|77480,volume=0.68[sh7];
[17:a]asplit=8[swinglight0][swinglight1][swinglight2][swinglight3][swinglight4][swinglight5][swinglight6][swinglight7];
[swinglight0]adelay=27550|27550,volume=0.48[sl0];
[swinglight1]adelay=33430|33430,volume=0.50[sl1];
[swinglight2]adelay=39310|39310,volume=0.50[sl2];
[swinglight3]adelay=45190|45190,volume=0.52[sl3];
[swinglight4]adelay=56950|56950,volume=0.52[sl4];
[swinglight5]adelay=62830|62830,volume=0.54[sl5];
[swinglight6]adelay=68710|68710,volume=0.58[sl6];
[swinglight7]adelay=74590|74590,volume=0.62[sl7];
[18:a]asplit=4[block0][block1][block2][block3];
[block0]adelay=31950|31950,volume=0.50[bl0];
[block1]adelay=43700|43700,volume=0.52[bl1];
[block2]adelay=61950|61950,volume=0.54[bl2];
[block3]adelay=72850|72850,volume=0.58[bl3];
[19:a]asplit=2[parry0][parry1];
[parry0]adelay=36950|36950,volume=0.56[p0];
[parry1]adelay=66800|66800,volume=0.60[p1];
[20:a]adelay=75100|75100,volume=0.62[shieldbreak];
[21:a]asplit=4[launch0][launch1][launch2][launch3];
[launch0]adelay=49300|49300,volume=0.56[la0];
[launch1]adelay=67300|67300,volume=0.60[la1];
[launch2]adelay=75900|75900,volume=0.66[la2];
[launch3]adelay=78800|78800,volume=0.72[la3];
[sad][battle][farewell][b0][b1][b2][b3][b4][w0][w1][w2][w3][w4][w5][w6][w7][i0][i1][i2][i3][i4][i5][i6][i7][s0][s1][g0][g1][t0][t1][t2][nova][fall][ready][m0][m1][m2][m3][m4][m5][m6][m7][l0][l1][l2][l3][l4][l5][l6][l7][h0][h1][h2][h3][h4][h5][h6][h7][sh0][sh1][sh2][sh3][sh4][sh5][sh6][sh7][sl0][sl1][sl2][sl3][sl4][sl5][sl6][sl7][bl0][bl1][bl2][bl3][p0][p1][shieldbreak][la0][la1][la2][la3]amix=inputs=85:normalize=0:dropout_transition=0,apad=pad_dur=99,aformat=sample_rates=48000:channel_layouts=stereo,loudnorm=I=-14:TP=-1.0:LRA=11,volume=-1.5dB,alimiter=limit=0.749894:attack=5:release=80:level=0,atrim=duration=98.0,asetpts=N/SR/TB[aout]
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
