param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\src\main\resources\sounds')
)

$ErrorActionPreference = 'Stop'
$sampleRate = 22050
$twoPi = [Math]::PI * 2.0

function Add-Oscillator {
    param(
        [double]$Time,
        [double]$Frequency,
        [double]$Gain,
        [ValidateSet('sine', 'triangle', 'organ')][string]$Wave = 'sine'
    )
    $phase = $twoPi * $Frequency * $Time
    switch ($Wave) {
        'triangle' { return $Gain * (2.0 / [Math]::PI) * [Math]::Asin([Math]::Sin($phase)) }
        'organ' { return $Gain * ([Math]::Sin($phase) + 0.34 * [Math]::Sin($phase * 2.0) + 0.16 * [Math]::Sin($phase * 3.0)) }
        default { return $Gain * [Math]::Sin($phase) }
    }
}

function Write-OriginalTrack {
    param(
        [string]$Name,
        [ValidateSet('route', 'maestro', 'ending')][string]$Kind,
        [double]$DurationSeconds
    )
    $path = Join-Path $OutputDirectory $Name
    $sampleCount = [int]($sampleRate * $DurationSeconds)
    $stream = [IO.File]::Open($path, [IO.FileMode]::Create, [IO.FileAccess]::Write)
    $writer = [IO.BinaryWriter]::new($stream)
    try {
        $dataSize = $sampleCount * 2
        $writer.Write([Text.Encoding]::ASCII.GetBytes('RIFF'))
        $writer.Write([int](36 + $dataSize))
        $writer.Write([Text.Encoding]::ASCII.GetBytes('WAVE'))
        $writer.Write([Text.Encoding]::ASCII.GetBytes('fmt '))
        $writer.Write([int]16)
        $writer.Write([int16]1)
        $writer.Write([int16]1)
        $writer.Write([int]$sampleRate)
        $writer.Write([int]($sampleRate * 2))
        $writer.Write([int16]2)
        $writer.Write([int16]16)
        $writer.Write([Text.Encoding]::ASCII.GetBytes('data'))
        $writer.Write([int]$dataSize)

        $routeNotes = @(146.83, 174.61, 220.00, 261.63, 220.00, 196.00, 174.61, 130.81)
        $maestroNotes = @(73.42, 77.78, 110.00, 103.83, 69.30, 92.50, 82.41, 116.54)
        $endingNotes = @(146.83, 174.61, 196.00, 220.00, 261.63, 220.00, 196.00, 174.61)

        for ($i = 0; $i -lt $sampleCount; $i++) {
            $t = $i / [double]$sampleRate
            $fade = [Math]::Min(1.0, $t / 0.8) * [Math]::Min(1.0, ($DurationSeconds - $t) / 1.2)
            $value = 0.0
            if ($Kind -eq 'route') {
                $beat = 0.34
                $step = [int][Math]::Floor($t / $beat)
                $note = $routeNotes[$step % $routeNotes.Count]
                $within = $t % $beat
                $noteEnvelope = [Math]::Exp(-$within * 3.8)
                $value += Add-Oscillator $t $note (0.20 * $noteEnvelope) 'triangle'
                $value += Add-Oscillator $t ($note * 2.0) (0.075 * $noteEnvelope) 'sine'
                $bass = if (([int]($step / 4) % 2) -eq 0) { 73.42 } else { 65.41 }
                $value += Add-Oscillator $t $bass 0.18 'organ'
                $pulse = $t % ($beat * 2.0)
                if ($pulse -lt 0.055) {
                    $value += [Math]::Sin($twoPi * (72.0 - $pulse * 470.0) * $pulse) * 0.22 * [Math]::Exp(-$pulse * 42.0)
                }
            } elseif ($Kind -eq 'maestro') {
                $beat = 0.44
                $step = [int][Math]::Floor($t / $beat)
                $note = $maestroNotes[$step % $maestroNotes.Count]
                $within = $t % $beat
                $strike = [Math]::Exp(-$within * 4.8)
                $value += Add-Oscillator $t $note (0.24 * $strike) 'organ'
                $value += Add-Oscillator $t ($note * 1.4983) (0.105 * $strike) 'triangle'
                $value += Add-Oscillator $t 48.999 0.19 'organ'
                $value += Add-Oscillator $t 51.913 0.08 'sine'
                if (($step % 4) -eq 3 -and $within -lt 0.12) {
                    $value += [Math]::Sin($twoPi * (118.0 - $within * 520.0) * $within) * 0.27 * [Math]::Exp(-$within * 25.0)
                }
            } else {
                $beat = 0.82
                $step = [int][Math]::Floor($t / $beat)
                $note = $endingNotes[$step % $endingNotes.Count]
                $within = $t % $beat
                $phrase = [Math]::Min(1.0, $within / 0.12) * [Math]::Exp(-$within * 1.15)
                $value += Add-Oscillator $t $note (0.18 * $phrase) 'sine'
                $value += Add-Oscillator $t ($note * 2.0) (0.055 * $phrase) 'triangle'
                $root = if (([int]($step / 2) % 2) -eq 0) { 73.42 } else { 87.31 }
                $value += Add-Oscillator $t $root 0.14 'organ'
                $value += Add-Oscillator $t ($root * 1.5) 0.075 'sine'
            }
            $softClipped = [Math]::Tanh($value * 1.35) * 0.78 * [Math]::Max(0.0, $fade)
            $writer.Write([int16]([Math]::Round($softClipped * 32767.0)))
        }
    } finally {
        $writer.Dispose()
        $stream.Dispose()
    }
    Write-Host "Generated $path"
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
Write-OriginalTrack 'music-charles-route.wav' 'route' 24.0
Write-OriginalTrack 'music-charles-maestro.wav' 'maestro' 24.0
Write-OriginalTrack 'music-charles-ending.wav' 'ending' 24.0
