# Audio Audit

- Scan date: 2026-03-27 10:46:47 -05:00
- Sound directory: `C:\Users\caleb\IdeaProjects\BirdFight3\src\main\resources\sounds`
- Files scanned: 18
- Bytes scanned per file: 262144
- Total size: 47.36 MB
- Files with embedded hostnames: 13

This audit looks for embedded hostname-like strings inside bundled audio files. Hostname hits are not proof of ownership, but they are a release blocker until the asset source and license are verified.

| File | Size KB | Embedded hostnames |
| --- | ---: | --- |
| bonk.mp3 | 9.6 | 101soundboards.com |
| butter.mp3 | 14.5 | 101soundboards.com |
| buttonclick.mp3 | 16.1 | 101soundboards.com |
| dark-ages-ultimate-battle.mp3 | 14325.1 | 101soundboards.com |
| finalfanfare.mp3 | 102.6 | 101soundboards.com |
| GW2ops.mp3 | 2231.4 | 101soundboards.com |
| hugewave.mp3 | 107.5 | 101soundboards.com |
| jalapeno.mp3 | 29.2 | 101soundboards.com |
| rvthrow.mp3 | 17.7 | 101soundboards.com |
| skycity.mp3 | 14322.3 | 101soundboards.com |
| swing.mp3 | 7.5 | 101soundboards.com |
| vase-breaking.mp3 | 14.9 | 101soundboards.com |
| zombie-falling.mp3 | 21 | 101soundboards.com |

Recommended next steps:
1. Replace each flagged file with an asset that has a recorded source and license.
2. Add an asset manifest that maps every shipped sound to its source, license, and modification status.
3. Re-run this audit before any public release.

