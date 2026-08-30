# Bird Fight 3

Bird Fight 3 is a feature-complete JavaFX platform fighter with a 22-bird
roster, authored single-player campaigns, local and direct-connect multiplayer,
and code-drawn vector presentation.

## Project status

**Feature complete and in maintenance mode.** Version 1.5.0 is the definitive
content release. New work is expected to be focused bug fixes and explicitly
chosen improvements rather than an open-ended roadmap. The issue tracker stays
open, and future maintenance releases may still be published.

Download the latest portable Windows package from
[GitHub Releases](https://github.com/Caleb-Guyer/BirdFight3/releases). Extract
`BirdFight3-<version>-win.zip` and run **Bird Fight 3.exe**. The portable package
contains its own Java runtime.

## Highlights

- **22 distinct fighters** with authored normals, directional specials,
  ultimates, skins, frame data, matchup identities, and CPU behavior.
- **The Still Sky**, a 40-mission campaign with three difficulties, objective
  missions, recruitable allies, boss phases, cutscenes, and credits.
- **22 Classic routes**, each with authored encounters, an objective round,
  bosses, rewards, and an ending.
- **Smash and Stamina battles**, a full rules editor, local multiplayer, CPU
  opponents, Tournament, Squad Strike, Boss Rush, and Training Academy.
- **21 main stages and 26 stage variants**, including campaign, Classic, and
  boss arenas with hazards and traversal mechanics.
- **Progression and collection** through profiles, achievements, Bird Coins,
  reward packs, the Shop, fighter records, match history, cutscene galleries,
  music, and unlock guides.
- **Self-contained replays** that preserve the seed, rules, roster, stage, and
  per-tick inputs for deterministic playback.
- **LAN and direct internet play** using deterministic lockstep simulation.
- Keyboard, Xbox-compatible gamepad, and configurable Wii Remote support.
- Procedural sound effects, public-domain music, vector bird art, optional
  sprite-sheet overrides, packaged auto-update, and save-safe upgrades.

## Playing

Controls are configurable and shown in the in-game move guide and settings.
Keyboard and controller prompts adapt to the active input device.

### Internet multiplayer

Internet play is direct peer-to-peer host/join for up to four players. It has
no central account, matchmaking, NAT traversal, or relay service.

To host:

1. Open **NETWORK PLAY → INTERNET PLAY → HOST INTERNET**.
2. Allow Bird Fight 3 through the operating-system firewall.
3. Forward the selected **TCP** port (default `28999`) to the host computer.
4. Share the public IP address or DNS name and port with invited players.

To join, enter `host:port`, such as `games.example.com:28999`. Everyone should
run the same game version. The host supplies the authoritative rules and active
`bird-stats.properties` tuning snapshot for the match. Participants can see one
another's IP addresses, so connect only with people you trust. Wired, nearby
connections provide the best lockstep response.

## Development

The project is a Java 21 Maven application. Windows packaging requires a JDK
that includes `jpackage`; MSI or EXE installers also require the WiX Toolset.

```powershell
# Run the complete regular test suite and audio audit
.\scripts\test.cmd

# Run from source
.\mvnw.cmd javafx:run

# Build and verify the jar-based distribution
.\mvnw.cmd package
.\scripts\verify-dist.cmd

# Build the self-contained Windows app image and portable zip
.\build-installer.ps1 -RunTests
```

The Maven package is written to `target/BirdGame3-1.5.0-dist.zip`. The portable
Windows package is written to `target/BirdFight3-1.5.0-win.zip`.

If Maven reports that `JAVA_HOME` is missing or selects Java 8, set `JAVA_HOME`
to a JDK 21 or newer. `scripts/test.cmd` and `build-installer.ps1` can also find
compatible JDK installations in common Windows locations.

### Balance and regression labs

```powershell
# Full roster AI-vs-AI audit
.\mvnw.cmd test -Dtest=BalanceLabRun

# All authored Classic routes
.\mvnw.cmd test -Dtest=ClassicBalanceLabRun -DclassicBird=ALL -DclassicMatches=64

# All 40 Still Sky missions on Easy, Normal, and Hard
.\mvnw.cmd test -Dtest=AdventureBalanceLabRun -DadventureMatches=24
```

The lab reports in `audit/` are playtest leads rather than automatic balance
orders. Objective results also measure CPU navigation.

## Repository guide

- `src/main/java/com/example/birdgame3/` — game, simulation, UI, networking,
  progression, campaign, and fighter systems
- `src/main/resources/` — story text, stage previews, music, sound effects, and
  legacy image assets
- `src/test/java/com/example/birdgame3/` — gameplay, determinism, persistence,
  transport, UI, visual-policy, and content regression tests
- `sprites/` — optional sprite-sheet pipeline and the Pigeon example
- `scripts/` — tests, distribution verification, visual tools, and audio audit
- `docs/` — release notes, asset records, and implementation documentation
- `audit/` — generated balance and audio reports

Developers and coding assistants should read `AGENTS.md` before changing
simulation, replay, networking, save, updater, or packaging code.

## Known boundaries

- Internet play requires direct reachability and manual port forwarding.
- High-latency play uses lockstep rather than rollback.
- Most fighters use code-drawn vector art; real per-fighter sprite art remains
  optional.
- The large `BirdGame3.java` and `Bird.java` classes are known technical debt,
  not a release blocker.

## Contributing and maintenance

Bug reports and deliberately scoped improvements are welcome through
[GitHub Issues](https://github.com/Caleb-Guyer/BirdFight3/issues). See
`CONTRIBUTING.md` and `SECURITY.md` for reporting guidance. Maintenance work
should preserve deterministic simulation, save compatibility, and replay/LAN
protocol invariants.

## Credits and license

Bird Fight 3 is released under the MIT License. Music and image attribution is
recorded in `CREDITS-AUDIO.md` and `CREDITS-IMAGES.md`. Bundled music is public
domain or CC0 as documented; bundled sound effects are original procedural
assets.
