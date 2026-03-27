# Bird Fight 3

Bird Fight 3 is a JavaFX platform fighter with local play, AI opponents, LAN matches, unlockables, and multiple single-player modes.

## Requirements

- JDK 21 or newer
- `JAVA_HOME` pointing to that JDK
- PowerShell or another shell that can run the Maven wrapper

This project targets Java 21 in `pom.xml`. Maven Enforcer now fails fast on older JDKs. If `.\mvnw` fails with `JAVA_HOME not found` or launches under Java 8, point it at a JDK 21+ install first.

PowerShell example:

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

If IntelliJ installed a JDK for you, a usable path may be under `C:\Users\<you>\.jdks\`.

## Run

```powershell
.\mvnw javafx:run
```

## Test

```powershell
.\mvnw test
```

Or use the repo test script, which will try to find a local JDK 21+ install under common Windows locations before running Maven:

```powershell
.\scripts\test.cmd
```

## CI

GitHub Actions runs the Windows test workflow from `.github/workflows/ci.yml` on every push and pull request.

## Audio Audit

Bundled audio should be treated as release-blocking until its source and license are recorded. Run the audit script to scan shipped sound files for embedded hostnames and write a markdown report:

```powershell
.\scripts\audit-sounds.cmd -OutputPath .\audit\audio-audit.md
```

The latest checked-in audit report lives at `docs/audio-audit.md`.
Track clearance or replacement work in `docs/audio-asset-manifest.csv`.

## Links

- Official Bird Fight 3 Wiki: https://bird-fight-3.fandom.com/wiki/Bird_Fight_3_Wiki

## Wiimote Support

- WiimoteHook: https://epigramx.github.io/WiimoteHook/
- JoyToKey: https://joytokey.net/en/download
- Bird Fight 3 JoyToKey table files: https://drive.google.com/drive/u/0/folders/1c-JLjfshHN9w6SN-8aThY_4pzLLG5hfL
