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
    'music-prologue.mp3',
    'music-razorbill-worldseam.mp3',
    'music-vulture-debt-engine.mp3',
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
    'sfx-launch-tail.wav',
    'sfx-attack-clank.wav',
    'sfx-achievement.wav',
    'sfx-sizzle.wav',
    'sfx-click.wav',
    'sfx-thunder-field-recording.wav'
)

$ffmpegArguments = @('-hide_banner', '-loglevel', 'warning', '-i', $silentVideo)
foreach ($audioInput in $audioInputs) {
    $ffmpegArguments += @('-i', (Join-Path $sounds $audioInput))
}

# The first eight seconds contain only four separate passages from a real
# public-domain thunder field recording. Music begins on the full-release hit,
# then hands off to the more aggressive finale score for the back half.
$filterParts = [Collections.Generic.List[string]]::new()
$mixLabels = [Collections.Generic.List[string]]::new()

function Add-DelayedAudioClips {
    param(
        [Collections.Generic.List[string]]$FilterParts,
        [Collections.Generic.List[string]]$MixLabels,
        [int]$InputIndex,
        [string]$Prefix,
        [double[]]$TimesSeconds,
        [double]$Volume
    )
    if ($TimesSeconds.Count -eq 0) { return }
    $source = "[$($InputIndex):a]"
    if ($TimesSeconds.Count -gt 1) {
        $branches = for ($i = 0; $i -lt $TimesSeconds.Count; $i++) { "[$($Prefix)Src$i]" }
        $FilterParts.Add("$source" + "asplit=$($TimesSeconds.Count)" + ($branches -join '')) | Out-Null
    }
    for ($i = 0; $i -lt $TimesSeconds.Count; $i++) {
        $inputLabel = if ($TimesSeconds.Count -gt 1) { "[$($Prefix)Src$i]" } else { $source }
        $outputLabel = "$Prefix$i"
        $delayMs = [int][Math]::Round($TimesSeconds[$i] * 1000.0)
        $FilterParts.Add("$inputLabel" + "adelay=$delayMs|$delayMs,volume=$Volume" + "[$outputLabel]") | Out-Null
        $MixLabels.Add("[$outputLabel]") | Out-Null
    }
}

$filterParts.Add('[2:a]atrim=start=0:end=55.5,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=0.30,afade=t=out:st=52.0:d=3.5,volume=0.50,adelay=8000|8000[release]') | Out-Null
$filterParts.Add('[3:a]atrim=start=0.279:end=49.0,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=0.20,afade=t=out:st=45.5:d=3.2,volume=0.94,adelay=60500|60500[climax]') | Out-Null
$mixLabels.Add('[release]') | Out-Null
$mixLabels.Add('[climax]') | Out-Null

$filterParts.Add('[26:a]asplit=4[thunderSrc0][thunderSrc1][thunderSrc2][thunderSrc3]') | Out-Null
$filterParts.Add('[thunderSrc0]atrim=start=3.45:end=5.45,asetpts=PTS-STARTPTS,afade=t=out:st=1.55:d=0.45,volume=1.08[thunder0]') | Out-Null
$filterParts.Add('[thunderSrc1]atrim=start=6.20:end=8.20,asetpts=PTS-STARTPTS,afade=t=out:st=1.55:d=0.45,volume=1.62,adelay=2000|2000[thunder1]') | Out-Null
$filterParts.Add('[thunderSrc2]atrim=start=12.05:end=14.05,asetpts=PTS-STARTPTS,afade=t=out:st=1.55:d=0.45,volume=2.05,adelay=4000|4000[thunder2]') | Out-Null
$filterParts.Add('[thunderSrc3]atrim=start=15.05:end=17.05,asetpts=PTS-STARTPTS,afade=t=out:st=1.55:d=0.45,volume=2.18,adelay=6000|6000[thunder3]') | Out-Null
$mixLabels.Add('[thunder0]') | Out-Null
$mixLabels.Add('[thunder1]') | Out-Null
$mixLabels.Add('[thunder2]') | Out-Null
$mixLabels.Add('[thunder3]') | Out-Null

$actionCuts = [double[]]@(
    8.000, 10.000, 12.000, 14.000,
    28.500, 30.500, 32.500, 34.500,
    36.500, 37.750, 39.000, 40.250, 41.500, 42.750, 44.000, 45.250,
    46.500, 48.500, 50.500, 52.500,
    54.500, 57.000,
    63.500, 65.625, 67.750, 69.875,
    75.500, 77.625, 79.750, 81.875,
    96.000, 98.667, 101.333
)
$swingTimes = [double[]]($actionCuts | ForEach-Object { $_ + 0.48 })
$impactTimes = [double[]]($actionCuts | ForEach-Object { $_ + 0.66 })
$heavyTimes = [double[]]@(
    for ($i = 0; $i -lt $actionCuts.Count; $i += 2) { $actionCuts[$i] + 1.64 }
)
$hitTimes = [double[]]@(
    for ($i = 1; $i -lt $actionCuts.Count; $i += 2) { $actionCuts[$i] + 1.70 }
)
$cutWhooshes = [double[]]($actionCuts | ForEach-Object { $_ + 0.035 })

Add-DelayedAudioClips $filterParts $mixLabels 5 'whoosh' $cutWhooshes 0.34
Add-DelayedAudioClips $filterParts $mixLabels 17 'swing' $swingTimes 0.43
Add-DelayedAudioClips $filterParts $mixLabels 13 'impact' $impactTimes 0.48
Add-DelayedAudioClips $filterParts $mixLabels 16 'heavy' $heavyTimes 0.40
Add-DelayedAudioClips $filterParts $mixLabels 15 'hit' $hitTimes 0.38

Add-DelayedAudioClips $filterParts $mixLabels 4 'boom' ([double[]]@(8.000, 16.000, 28.500, 36.500, 46.500, 54.500, 63.500, 75.500, 84.000, 96.000, 104.000)) 0.66
Add-DelayedAudioClips $filterParts $mixLabels 7 'shatter' ([double[]]@(28.500, 63.500, 75.500, 101.333)) 0.52
Add-DelayedAudioClips $filterParts $mixLabels 8 'wave' ([double[]]@(16.000, 36.500, 46.500, 59.500, 63.500, 72.000, 75.500, 84.000, 96.000, 104.000)) 0.58
Add-DelayedAudioClips $filterParts $mixLabels 9 'thwump' ([double[]]@(64.208, 68.458, 78.333, 82.583, 88.140, 90.460, 92.780, 99.333)) 0.42
Add-DelayedAudioClips $filterParts $mixLabels 10 'nova' ([double[]]@(12.050, 64.200, 80.200, 101.100)) 0.54
Add-DelayedAudioClips $filterParts $mixLabels 12 'ready' ([double[]]@(8.000, 28.500, 36.500, 46.500, 63.500, 75.500, 84.000, 96.000, 104.000)) 0.48
Add-DelayedAudioClips $filterParts $mixLabels 18 'block' ([double[]]@(10.770, 31.270, 39.770, 49.270, 66.395, 78.395, 98.997)) 0.44
Add-DelayedAudioClips $filterParts $mixLabels 19 'parry' ([double[]]@(12.820, 33.320, 43.570, 51.320, 68.570, 80.570, 101.663)) 0.48
Add-DelayedAudioClips $filterParts $mixLabels 20 'shieldbreak' ([double[]]@(34.920, 45.670, 70.275, 82.275, 102.733)) 0.52
Add-DelayedAudioClips $filterParts $mixLabels 21 'launch' ([double[]]@(15.420, 35.920, 53.920, 58.420, 71.295, 83.295, 103.553)) 0.54
Add-DelayedAudioClips $filterParts $mixLabels 23 'achievement' ([double[]]@(59.500, 104.000)) 0.42
Add-DelayedAudioClips $filterParts $mixLabels 24 'sizzle' ([double[]]@(12.250, 64.400, 80.400, 101.300)) 0.36
Add-DelayedAudioClips $filterParts $mixLabels 25 'click' ([double[]]@(16.000, 36.500, 37.750, 39.000, 40.250, 41.500, 42.750, 44.000, 45.250, 59.500)) 0.38

$massClashes = [double[]]@(84.900, 85.480, 86.060, 86.640, 87.220, 87.800, 88.380, 88.960, 89.540, 90.120, 90.700, 91.280, 91.860, 92.440, 93.020, 93.600, 94.180, 94.760, 95.340)
Add-DelayedAudioClips $filterParts $mixLabels 22 'massClank' $massClashes 0.58
Add-DelayedAudioClips $filterParts $mixLabels 17 'massSwing' ([double[]]($massClashes | ForEach-Object { $_ - 0.12 })) 0.46
Add-DelayedAudioClips $filterParts $mixLabels 13 'massImpact' ([double[]]($massClashes | ForEach-Object { $_ + 0.10 })) 0.52

$filterGraph = ($filterParts -join ";`n") + ";`n" + ($mixLabels -join '') +
    "amix=inputs=$($mixLabels.Count):normalize=0:dropout_transition=0," +
    'apad=pad_dur=110.0,aformat=sample_rates=48000:channel_layouts=stereo,' +
    'loudnorm=I=-14:TP=-1.0:LRA=11,volume=-1.5dB,' +
    'alimiter=limit=0.749894:attack=5:release=80:level=0,' +
    'atrim=duration=109.000,asetpts=N/SR/TB[aout]'

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
    '-metadata', 'comment=Official Bird Fight 3 full release trailer',
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
