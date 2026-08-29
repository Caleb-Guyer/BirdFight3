param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\src\main\resources\sounds")
)

$sampleRate = 44100

function New-SoundBuffer([double]$DurationSeconds) {
    return [double[]]::new([int][Math]::Ceiling($sampleRate * $DurationSeconds))
}

function Add-NoiseBurst {
    param(
        [double[]]$Buffer,
        [int]$Seed,
        [double]$Level,
        [double]$Decay,
        [double]$LowPass,
        [double]$Attack = 0.0015,
        [double]$StartSeconds = 0.0
    )
    $random = [System.Random]::new($Seed)
    $start = [int]($StartSeconds * $sampleRate)
    $smooth = 0.0
    for ($i = $start; $i -lt $Buffer.Length; $i++) {
        $t = ($i - $start) / [double]$sampleRate
        $raw = $random.NextDouble() * 2.0 - 1.0
        $smooth += ($raw - $smooth) * $LowPass
        $envelope = (1.0 - [Math]::Exp(-$t / $Attack)) * [Math]::Exp(-$Decay * $t)
        $Buffer[$i] += $Level * $smooth * $envelope
    }
    return ,$Buffer
}

function Add-WhooshNoise {
    param(
        [double[]]$Buffer,
        [int]$Seed,
        [double]$Level,
        [double]$LowPass,
        [double]$Shape = 1.5
    )
    $random = [System.Random]::new($Seed)
    $smooth = 0.0
    for ($i = 0; $i -lt $Buffer.Length; $i++) {
        $x = $i / [double]([Math]::Max(1, $Buffer.Length - 1))
        $raw = $random.NextDouble() * 2.0 - 1.0
        $smooth += ($raw - $smooth) * $LowPass
        $envelope = [Math]::Pow([Math]::Sin([Math]::PI * $x), $Shape)
        $Buffer[$i] += $Level * $smooth * $envelope
    }
    return ,$Buffer
}

function Add-SineSweep {
    param(
        [double[]]$Buffer,
        [double]$StartFrequency,
        [double]$EndFrequency,
        [double]$Level,
        [double]$Decay,
        [double]$Attack = 0.001,
        [double]$StartSeconds = 0.0
    )
    $start = [int]($StartSeconds * $sampleRate)
    $phase = 0.0
    $duration = [Math]::Max(1, $Buffer.Length - $start)
    $ratio = $EndFrequency / $StartFrequency
    for ($i = $start; $i -lt $Buffer.Length; $i++) {
        $localIndex = $i - $start
        $t = $localIndex / [double]$sampleRate
        $x = $localIndex / [double]$duration
        $frequency = $StartFrequency * [Math]::Pow($ratio, $x)
        $phase += 2.0 * [Math]::PI * $frequency / $sampleRate
        $envelope = (1.0 - [Math]::Exp(-$t / $Attack)) * [Math]::Exp(-$Decay * $t)
        $Buffer[$i] += $Level * [Math]::Sin($phase) * $envelope
    }
    return ,$Buffer
}

function Add-WhooshTone {
    param(
        [double[]]$Buffer,
        [double]$StartFrequency,
        [double]$EndFrequency,
        [double]$Level,
        [double]$Shape = 1.4
    )
    $phase = 0.0
    $ratio = $EndFrequency / $StartFrequency
    for ($i = 0; $i -lt $Buffer.Length; $i++) {
        $x = $i / [double]([Math]::Max(1, $Buffer.Length - 1))
        $frequency = $StartFrequency * [Math]::Pow($ratio, $x)
        $phase += 2.0 * [Math]::PI * $frequency / $sampleRate
        $envelope = [Math]::Pow([Math]::Sin([Math]::PI * $x), $Shape)
        $Buffer[$i] += $Level * [Math]::Sin($phase) * $envelope
    }
    return ,$Buffer
}

function Write-Sound {
    param(
        [string]$Name,
        [double[]]$Buffer,
        [double]$TargetPeak = 0.88
    )
    $mean = 0.0
    foreach ($sample in $Buffer) { $mean += $sample }
    $mean /= [Math]::Max(1, $Buffer.Length)

    $peak = 0.000001
    for ($i = 0; $i -lt $Buffer.Length; $i++) {
        $Buffer[$i] -= $mean
        $peak = [Math]::Max($peak, [Math]::Abs($Buffer[$i]))
    }
    $gain = $TargetPeak / $peak
    $fadeSamples = [int](0.003 * $sampleRate)

    New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
    $path = Join-Path $OutputDirectory $Name
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create)
    $writer = [System.IO.BinaryWriter]::new($stream)
    try {
        $dataBytes = $Buffer.Length * 2
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
        $writer.Write([int](36 + $dataBytes))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
        $writer.Write([int]16)
        $writer.Write([int16]1)
        $writer.Write([int16]1)
        $writer.Write([int]$sampleRate)
        $writer.Write([int]($sampleRate * 2))
        $writer.Write([int16]2)
        $writer.Write([int16]16)
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
        $writer.Write([int]$dataBytes)

        for ($i = 0; $i -lt $Buffer.Length; $i++) {
            $edgeGain = 1.0
            if ($i -lt $fadeSamples) { $edgeGain = $i / [double]$fadeSamples }
            $remaining = $Buffer.Length - 1 - $i
            if ($remaining -lt $fadeSamples) {
                $edgeGain = [Math]::Min($edgeGain, $remaining / [double]$fadeSamples)
            }
            $sample = [Math]::Max(-0.98, [Math]::Min(0.98, $Buffer[$i] * $gain * $edgeGain))
            $writer.Write([int16]([Math]::Round($sample * 32767.0)))
        }
    } finally {
        $writer.Dispose()
        $stream.Dispose()
    }
    Write-Host "Generated $path"
}

# Fast feather/wing cut: bright air, short body, no impact baked into the swing.
$buffer = New-SoundBuffer 0.18
$buffer = Add-WhooshNoise $buffer 1101 0.82 0.48 1.85
$buffer = Add-WhooshTone $buffer 520 1850 0.20 1.65
Write-Sound "sfx-swing-light.wav" $buffer 0.78

# Broad heavy wing/body swing with a low pressure wake.
$buffer = New-SoundBuffer 0.34
$buffer = Add-WhooshNoise $buffer 1102 0.88 0.16 1.35
$buffer = Add-WhooshTone $buffer 190 72 0.32 1.20
$buffer = Add-WhooshTone $buffer 420 160 0.16 1.45
Write-Sound "sfx-swing-heavy.wav" $buffer 0.84

# Small peck/jab contact: tight transient with very little tail.
$buffer = New-SoundBuffer 0.12
$buffer = Add-NoiseBurst $buffer 2101 0.92 42.0 0.78 0.0004
$buffer = Add-SineSweep $buffer 920 210 0.72 34.0 0.0004
Write-Sound "sfx-impact-light.wav" $buffer 0.80

# General body impact: crack plus a round mid-frequency thud.
$buffer = New-SoundBuffer 0.20
$buffer = Add-NoiseBurst $buffer 2102 0.88 26.0 0.50 0.0006
$buffer = Add-SineSweep $buffer 470 82 0.92 20.0 0.0005
$buffer = Add-SineSweep $buffer 1180 360 0.20 30.0 0.0004
Write-Sound "sfx-impact-medium.wav" $buffer 0.88

# Charged/heavy contact: deeper body, controlled crunch, and a longer weight tail.
$buffer = New-SoundBuffer 0.31
$buffer = Add-NoiseBurst $buffer 2103 0.92 15.0 0.22 0.0007
$buffer = Add-NoiseBurst $buffer 2104 0.34 31.0 0.70 0.0003
$buffer = Add-SineSweep $buffer 175 43 1.00 10.5 0.0008
$buffer = Add-SineSweep $buffer 390 76 0.48 15.0 0.0005
Write-Sound "sfx-impact-heavy.wav" $buffer 0.92

# Fast launch tail layered after strong knockback, leaving the impact transient clear.
$buffer = New-SoundBuffer 0.40
$buffer = Add-WhooshNoise $buffer 3101 0.78 0.22 1.55
$buffer = Add-WhooshTone $buffer 260 1180 0.30 1.45
$buffer = Add-SineSweep $buffer 150 62 0.22 13.0 0.0006
Write-Sound "sfx-launch-tail.wav" $buffer 0.76

# Shield contact: hard attack transient against a resonant energy surface.
$buffer = New-SoundBuffer 0.23
$buffer = Add-NoiseBurst $buffer 4101 0.70 34.0 0.74 0.0003
$buffer = Add-SineSweep $buffer 820 690 0.58 16.0 0.0003
$buffer = Add-SineSweep $buffer 1270 1010 0.38 19.0 0.0003
Write-Sound "sfx-shield-block.wav" $buffer 0.82

# Parry: a clean upward confirmation chirp plus glassy harmonics.
$buffer = New-SoundBuffer 0.36
$buffer = Add-NoiseBurst $buffer 4102 0.45 44.0 0.88 0.0002
$buffer = Add-WhooshTone $buffer 720 2380 0.48 1.90
$buffer = Add-SineSweep $buffer 1760 1490 0.34 10.5 0.0003
$buffer = Add-SineSweep $buffer 2630 2240 0.20 13.0 0.0003
Write-Sound "sfx-shield-parry.wav" $buffer 0.86

# Shield break: low rupture followed by staggered bright fragments.
$buffer = New-SoundBuffer 0.52
$buffer = Add-NoiseBurst $buffer 4103 0.95 13.0 0.42 0.0004
$buffer = Add-NoiseBurst $buffer 4104 0.52 24.0 0.82 0.0003 0.035
$buffer = Add-NoiseBurst $buffer 4105 0.38 28.0 0.88 0.0003 0.085
$buffer = Add-SineSweep $buffer 230 48 0.90 8.5 0.0007
$buffer = Add-SineSweep $buffer 1540 760 0.30 9.5 0.0003 0.025
$buffer = Add-SineSweep $buffer 2210 1120 0.24 11.0 0.0003 0.070
Write-Sound "sfx-shield-break.wav" $buffer 0.92

# Attack-on-attack clank: short metal-like disagreement, distinct from shields.
$buffer = New-SoundBuffer 0.26
$buffer = Add-NoiseBurst $buffer 5101 0.62 38.0 0.85 0.0002
$buffer = Add-SineSweep $buffer 640 560 0.52 15.0 0.0002
$buffer = Add-SineSweep $buffer 1040 910 0.42 17.0 0.0002
$buffer = Add-SineSweep $buffer 1680 1440 0.28 20.0 0.0002
Write-Sound "sfx-attack-clank.wav" $buffer 0.86
