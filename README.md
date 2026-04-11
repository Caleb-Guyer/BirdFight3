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

## Online Relay Server

Online play now uses a relay server so hosts and guests can connect from different networks without direct LAN visibility.

Run a relay locally for testing:

```powershell
.\mvnw -q -DskipTests compile
java -cp target\classes com.example.birdgame3.OnlineRelayServer
```

To make internet play available to other people, run that relay process on a public machine and point the in-game `HOST ONLINE` / `JOIN ONLINE` screens at that server's hostname or IP.

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

## Links

- Official Bird Fight 3 Wiki: https://bird-fight-3.fandom.com/wiki/Bird_Fight_3_Wiki

## Wii Remote Support

Bird Fight 3 natively supports Wii remotes.
