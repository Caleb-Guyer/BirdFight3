param(
    [string]$OutputPath = (Join-Path $PSScriptRoot "..\src\main\resources\sounds\music-defeat.wav")
)

$sampleRate = 22050
$durationSeconds = 13.5
$sampleCount = [int]($sampleRate * $durationSeconds)
$dry = [double[]]::new($sampleCount)
$chordSeconds = 2.7

# A slow D-minor descent with muted bell attacks and a soft bowed pad. This is
# kept deterministic so the committed WAV can always be reproduced exactly.
$chords = @(
    @(146.83, 220.00, 293.66, 349.23),
    @(116.54, 174.61, 233.08, 293.66),
    @(98.00, 146.83, 196.00, 233.08),
    @(110.00, 164.81, 220.00, 277.18),
    @(73.42, 146.83, 220.00, 293.66)
)

for ($i = 0; $i -lt $sampleCount; $i++) {
    $t = $i / [double]$sampleRate
    $chordIndex = [Math]::Min($chords.Count - 1, [int]($t / $chordSeconds))
    $localTime = $t - ($chordIndex * $chordSeconds)
    $attack = [Math]::Min(1.0, $localTime / 0.65)
    $release = if ($localTime -gt 2.0) { [Math]::Max(0.0, (2.7 - $localTime) / 0.7) } else { 1.0 }
    $padEnvelope = $attack * $release
    $sample = 0.0

    foreach ($frequency in $chords[$chordIndex]) {
        $phase = 2.0 * [Math]::PI * $frequency * $t
        $sample += $padEnvelope * (0.095 * [Math]::Sin($phase) + 0.025 * [Math]::Sin(2.0 * $phase))
    }

    $bellEnvelope = [Math]::Exp(-2.35 * $localTime)
    $bellFrequency = $chords[$chordIndex][2]
    $sample += $bellEnvelope * (0.18 * [Math]::Sin(2.0 * [Math]::PI * $bellFrequency * $t))
    $sample += $bellEnvelope * (0.055 * [Math]::Sin(2.0 * [Math]::PI * $bellFrequency * 2.01 * $t))

    $bassFrequency = $chords[$chordIndex][0] / 2.0
    $sample += $padEnvelope * 0.13 * [Math]::Sin(2.0 * [Math]::PI * $bassFrequency * $t)

    $fadeIn = [Math]::Min(1.0, $t / 0.35)
    $fadeOut = [Math]::Min(1.0, ($durationSeconds - $t) / 2.2)
    $dry[$i] = $sample * $fadeIn * [Math]::Max(0.0, $fadeOut)
}

$delayOne = [int](0.19 * $sampleRate)
$delayTwo = [int](0.37 * $sampleRate)
$directory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$stream = [System.IO.File]::Open($OutputPath, [System.IO.FileMode]::Create)
$writer = [System.IO.BinaryWriter]::new($stream)

try {
    $dataBytes = $sampleCount * 2
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

    for ($i = 0; $i -lt $sampleCount; $i++) {
        $mixed = $dry[$i]
        if ($i -ge $delayOne) { $mixed += 0.24 * $dry[$i - $delayOne] }
        if ($i -ge $delayTwo) { $mixed += 0.13 * $dry[$i - $delayTwo] }
        $mixed = [Math]::Max(-0.98, [Math]::Min(0.98, $mixed))
        $writer.Write([int16]([Math]::Round($mixed * 32767.0)))
    }
} finally {
    $writer.Dispose()
    $stream.Dispose()
}

Write-Host "Generated $OutputPath"
