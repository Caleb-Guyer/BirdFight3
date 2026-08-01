[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, ValueFromPipeline = $true)]
    [string[]]$Path
)

$ErrorActionPreference = "Stop"

foreach ($inputPath in $Path) {
    $resolved = Resolve-Path -LiteralPath $inputPath
    [byte[]]$bytes = [System.IO.File]::ReadAllBytes($resolved.Path)
    $start = 0
    $end = $bytes.Length

    if ($bytes.Length -ge 10 -and
            $bytes[0] -eq [byte][char]'I' -and
            $bytes[1] -eq [byte][char]'D' -and
            $bytes[2] -eq [byte][char]'3') {
        $tagSize = (($bytes[6] -band 0x7F) -shl 21) `
                -bor (($bytes[7] -band 0x7F) -shl 14) `
                -bor (($bytes[8] -band 0x7F) -shl 7) `
                -bor ($bytes[9] -band 0x7F)
        $footerSize = if (($bytes[5] -band 0x10) -ne 0) { 10 } else { 0 }
        $start = [Math]::Min($bytes.Length, 10 + $tagSize + $footerSize)
    }

    if ($end - $start -ge 128 -and
            $bytes[$end - 128] -eq [byte][char]'T' -and
            $bytes[$end - 127] -eq [byte][char]'A' -and
            $bytes[$end - 126] -eq [byte][char]'G') {
        $end -= 128
    }

    if ($start -gt 0 -or $end -lt $bytes.Length) {
        [byte[]]$audio = $bytes[$start..($end - 1)]
        [System.IO.File]::WriteAllBytes($resolved.Path, $audio)
        Write-Host "Stripped MP3 metadata: $($resolved.Path)"
    } else {
        Write-Host "No ID3 metadata found: $($resolved.Path)"
    }
}
