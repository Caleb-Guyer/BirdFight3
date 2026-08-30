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
    'music-razorbill-seamreaver.mp3',
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
    'sfx-click.wav'
)

$ffmpegArguments = @('-hide_banner', '-loglevel', 'warning', '-i', $silentVideo)
foreach ($audioInput in $audioInputs) {
    $ffmpegArguments += @('-i', (Join-Path $sounds $audioInput))
}

# Three acts use separate decodes so every transition and the end card retain
# independent music. Fades stay before adelay; delayed timestamps applied to
# afade would otherwise erase late cues. Action cuts are spaced in six-beat
# phrases at 102.5 BPM, then eight-beat phrases at 129 BPM for the climax.
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

$filterParts.Add('[1:a]atrim=start=0:end=20.4,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=1.4,afade=t=out:st=16.7:d=3.7,volume=0.48[story]') | Out-Null
$filterParts.Add('[2:a]atrim=start=0:end=70.50,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=0.20,afade=t=out:st=68.30:d=2.20,volume=0.46,adelay=19940|19940[release]') | Out-Null
$filterParts.Add('[3:a]atrim=start=0:end=64.806,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=0.22,afade=t=out:st=62.40:d=2.406,volume=0.85,adelay=90194|90194[climax]') | Out-Null
$mixLabels.Add('[story]') | Out-Null
$mixLabels.Add('[release]') | Out-Null
$mixLabels.Add('[climax]') | Out-Null

$actionCuts = [double[]]@(
    20.000, 23.512, 27.024, 30.536,
    41.073, 44.585, 48.097, 51.609,
    62.146, 65.658, 69.171, 72.683,
    76.195, 79.707,
    90.244, 93.965, 97.686, 101.407,
    110.709, 114.430, 118.151, 121.872,
    136.756, 140.477, 144.198
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

Add-DelayedAudioClips $filterParts $mixLabels 4 'boom' ([double[]]@(9.50, 20.00, 55.122, 83.220, 90.244, 105.128, 110.709, 125.593, 147.919)) 0.66
Add-DelayedAudioClips $filterParts $mixLabels 7 'shatter' ([double[]]@(9.50, 105.128, 133.035)) 0.52
Add-DelayedAudioClips $filterParts $mixLabels 8 'wave' ([double[]]@(62.146, 90.244, 110.709, 125.593, 147.919)) 0.58
Add-DelayedAudioClips $filterParts $mixLabels 9 'thwump' ([double[]]@(96.20, 102.70, 106.20, 108.70)) 0.42
Add-DelayedAudioClips $filterParts $mixLabels 10 'nova' ([double[]]@(24.05, 111.20, 144.55)) 0.54
Add-DelayedAudioClips $filterParts $mixLabels 11 'fall' ([double[]]@(15.65)) 0.25
Add-DelayedAudioClips $filterParts $mixLabels 12 'ready' ([double[]]@(20.00, 90.244, 147.919)) 0.48
Add-DelayedAudioClips $filterParts $mixLabels 18 'block' ([double[]]@(45.25, 66.35, 80.42, 95.20, 115.25, 141.45)) 0.44
Add-DelayedAudioClips $filterParts $mixLabels 19 'parry' ([double[]]@(27.82, 49.00, 73.10, 99.00, 119.20, 145.00)) 0.48
Add-DelayedAudioClips $filterParts $mixLabels 20 'shieldbreak' ([double[]]@(53.00, 102.62, 122.92)) 0.52
Add-DelayedAudioClips $filterParts $mixLabels 21 'launch' ([double[]]@(31.65, 52.82, 74.85, 81.50, 103.72, 124.48, 145.82)) 0.54
Add-DelayedAudioClips $filterParts $mixLabels 22 'clank' ([double[]]@(91.30, 125.82, 129.55, 133.30)) 0.50
Add-DelayedAudioClips $filterParts $mixLabels 23 'achievement' ([double[]]@(83.22, 148.02)) 0.42
Add-DelayedAudioClips $filterParts $mixLabels 24 'sizzle' ([double[]]@(24.25, 111.42, 144.72)) 0.36
Add-DelayedAudioClips $filterParts $mixLabels 25 'click' ([double[]]@(34.048, 55.122, 83.220)) 0.38

$filterGraph = ($filterParts -join ";`n") + ";`n" + ($mixLabels -join '') +
    "amix=inputs=$($mixLabels.Count):normalize=0:dropout_transition=0," +
    'apad=pad_dur=156,aformat=sample_rates=48000:channel_layouts=stereo,' +
    'loudnorm=I=-14:TP=-1.0:LRA=11,volume=-1.5dB,' +
    'alimiter=limit=0.749894:attack=5:release=80:level=0,' +
    'atrim=duration=155.0,asetpts=N/SR/TB[aout]'

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
