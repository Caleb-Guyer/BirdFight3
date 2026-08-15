# BirdFight3 — AI Assistant Context

Read this before making changes. It captures architecture, hard-won invariants,
and project state that are not obvious from the code.

## What this is

A JavaFX 2D platform fighter (Smash-style) with 21 playable birds, built by a
solo developer (Caleb) with AI pair-programming. Java 21, Maven, single module.
Everything is code-drawn vector art on Canvas unless sprite sheets are provided
(see Sprite pipeline). ~40k-line `BirdGame3.java` god class + 1.1MB `Bird.java`
are known debt — work within them; don't attempt a grand refactor.

## Build / test / run

- Tests: `.\mvnw.cmd test` (CI runs them on every push — see CI section)
- Run from source: `.\mvnw.cmd javafx:run`
- Package for players: `.\build-installer.ps1 [-AppVersion X.Y.Z] [-Type msi]`
  → jpackage app-image + zip in `target\`. MSI needs the WiX Toolset.
- Balance lab (headless AI-vs-AI, ~7m): `.\mvnw.cmd test -Dtest=BalanceLabRun`
  → writes `audit/balance-report.md`
- Classic encounter lab (headless, ~12s for one route):
  `.\mvnw.cmd test -Dtest=ClassicBalanceLabRun -DclassicBird=ROADRUNNER -DclassicMatches=64`
  → writes `audit/classic-balance-report.md`; use `-DclassicBird=ALL` for every route

## THE DETERMINISM CONTRACT (most important thing in this file)

The simulation is a pure function of (match seed, per-tick inputs). Replays,
LAN lockstep netcode, and the balance lab all depend on this. Breaking it
causes silent desyncs. Rules:

1. **Sim randomness only via `SimRng`** (single seeded stream, reseeded per
   match in `startMatch`). `game.random` and `Bird.random` alias it.
2. **Render/audio/UI randomness must NEVER touch SimRng** — use
   `Math.random()`, `renderRandom`, or `audioRandom`. Draw-path code consuming
   SimRng desyncs at high refresh rates.
3. **No wall-clock reads in sim logic** — use `simTick` (60Hz fixed tick
   counter). `System.nanoTime()` is allowed only for render/perf/UI.
4. **Setup randomness** (random bird picks, map generation) uses seed-derived
   `Random` instances, NOT SimRng, so draw counts can't shift the stream.
5. Fixed timestep 60Hz with render interpolation on top; hitstop consumes
   ticks without advancing `simTick`; slow-mo scales wall-clock only.
6. Never stack time-scaling mechanisms (hitstop × slow-mo = perceived hang).

## Key systems and where they live

- **Game loop**: `BirdGame3.gameTick()` — accumulator, hitstop, lockstep gate,
  replay capture/inject, then per-tick sim body. Render interpolation:
  `snapshotRenderPositions`/`applyRenderInterpolation` (prev/curr lerp with
  teleport snap).
- **Replays**: `MatchReplay` (seed + config + per-tick input masks + dash
  taps), `ReplayStore` (gzip binary in `replays/`, keeps 30), browser via
  MATCH HISTORY → REPLAYS. Playback is self-contained (restores roster/map)
  and suppresses all progression side effects.
- **Lockstep netcode**: `LockstepSession` + `LanProtocol` v46. All machines run
  the full sim; host relays per-tick input bundles; 4-tick LAN input delay; state
  hashes exchanged every 120 ticks, desync → kill feed warning. During
  lockstep the sim reads ONLY `lanActionPressed` (bundle-applied) — live local
  arrays are gated out of `isActionPressed`. Legacy snapshot path is dormant
  behind `lockstepSession == null`. Direction edges register dash taps only
  from `applyLockstepBundle`; applying live host edges bypasses input delay and
  desynchronizes the host. Client keyboard and controller each own a
  mask; the sent mask is their union (a sync clobbering the other source
  froze P2 once — commit 7274ebf). Network matches reuse the standard cinematic
  victory/results screen; only the host-controlled lobby/exit actions differ.
- **Internet multiplayer**: direct TCP host/join reuses lockstep. Internet uses
  an 8-tick input buffer negotiated in `MSG_START`. Hosts choose a port (default
  28999) and must forward it through their router; joins accept DNS names,
  IPv4, and IPv6 endpoints. There is no central matchmaking, NAT traversal, or
  relay service. Internet hosting disables the companion feed. The handshake
  must complete before a player slot is exposed. `MSG_START` includes a
  host-authoritative `NetworkSimulationConfig` snapshot because updater-preserved
  tuning files can differ between PCs; clients restore their local tuning when
  the network session ends.
- **Tuning file**: `bird-stats.properties` (working dir). Per-bird power/jump/
  speed/flyUpForce + damageDealtMult/damageTakenMult/cooldownRate/ultimateRate
  + global.gravity/startingHealth. Loaded at startup and by the balance lab;
  F12 in Training writes the template and hot-reloads. Multipliers apply at
  choke points: `receiveScaledDamage` (ALL damage), the single cooldown
  decrement site, `gainUltimate`. Owned Vulture crow damage and launch inherit
  the owner's damageDealtMult, while Null Rock's boss flock remains independent.
  Reload upgrades the exact legacy shipped Goose/Bat/Vulture multiplier presets
  in memory; if any value in one bird's preset differs, the entire preset is
  treated as customized and left alone.
- **Developer profiles**: the `FEATHERDEV` settings code is a permanent
  all-content entitlement. Profile load reapplies `unlockEverythingForDeveloperProfile`
  so developer saves created on older versions automatically receive newly added
  birds, skins, maps, modes, progression unlocks, and the complete Still Sky
  mission/cutscene gallery without opening packs.
- **Save safety**: trailer launches use isolated Java Preferences and must never
  read or write the live profile. Normal shutdown persistence is gated until a
  profile has loaded successfully. A rolling backup is created before each normal
  startup load; preserve all three safeguards when adding headless/export modes.
- **Sprite pipeline**: `sprites/<bird>.png` + `.properties` replaces a bird's
  vector body (effects still draw). Per-skin variants: `<bird>-<suffix>` where
  suffix matches the skin key case/underscore-insensitively. One animation per
  row, art faces right, only `idle` required, fallback chains. Frame clocks on
  sim ticks. F12 hot-reloads. No real art exists yet — only the template.
- **Auto-update**: `GameUpdater` checks GitHub releases/latest on launch of
  packaged builds, downloads the `-win.zip` asset, swaps via a DETACHED
  helper (`cmd /c start` — the jpackage launcher's job object kills normal
  children) using ROBOCOPY (Copy-Item can't merge into populated trees), then
  relaunches. `/XF bird-stats.properties /XD sprites` protects player files.
- **Desktop shortcut**: created on first packaged launch via the shell's real
  Desktop path ([Environment]::GetFolderPath — Caleb's desktop is
  OneDrive-redirected; never use user.home/Desktop).
- **Balance lab**: `BalanceLab` + harness bridge (`harnessPrepareMatch`/
  `harnessTick` in BirdGame3, `headlessHarnessMode` gate in
  MatchController.triggerMatchEnd). No JavaFX toolkit, no UI, no progression.
- **Classic balance lab**: `ClassicBalanceLab` runs authored route encounters
  headlessly with their real teams, map variants, mutators, difficulty, stocks,
  timers, and route mechanics. It reports each combat round independently;
  objective rounds are excluded by default because their result primarily
  measures AI navigation. Later Roadrunner rounds preload prior Redline Bolts.
- **Null Rock cooldowns**: player-controlled Null Rock uses four independent
  directional reuse timers; CPU Null Rock deliberately retains one shared,
  difficulty-scaled boss timer so higher difficulties increase cadence safely.

## Releases

Tag `v<version>` on GitHub with asset `BirdFight3-<version>-win.zip` — every
installed copy offers the update on next launch. Flow: `.\build-installer.ps1
-AppVersion X.Y.Z -RunTests`, create the release (tag `vX.Y.Z`), attach the
zip. The Release workflow (on tag push) also runs checks and attaches the
jar-based dist zip. Latest shipped: v1.2.2. No gh CLI on this machine —
releases have been done via curl + the git credential helper token.

## CI (already exists — don't recreate)

`.github/workflows/ci.yml` (push/PR) and `release.yml` (tags): tests, package,
dist verification, and an AUDIO AUDIT (`scripts/audit-sounds.ps1`) that FAILS
the build if bundled audio contains embedded hostname strings. Gotcha: mp3s
from the wild carry Adobe XMP boilerplate (ns.adobe.com/w3.org) in ID3 tags —
STRIP ID3v2/v1 headers from any new audio before committing.

## Audio

All music is public domain (FreePD via archive.org mirror
`allfreepdmusicbykuronekony4n`); all SFX are procedurally synthesized
originals. Mapping in `CREDITS-AUDIO.md`. The owner rejected cute/silly
tracks — keep music serious/intense. SFX get pitch/volume variation via
`playManagedSfxVaried` (presentation-only `audioRandom`); match music ducks
under KO slow-mo.

## Balance state (2026-08-15, see audit/balance-report.md)

AI-vs-AI results — treat as "where to look," not verdicts (the AI can't pilot
technical kits like Razorbill/Charles):
- The latest 11,088-match audit completed with only 1 draw. Results range from
  Rooster at 32.0% to Raven at 74.5%. Combined with the
  owner's hands-on fighter passes, the roster remains an accepted playable
  baseline, though not a claim of tournament-perfect balance.
- Titmouse's campaign-era 1.45 dealt / 0.68 taken preset produced an 84.6%
  outlier. Restoring 1.10 dealt / 0.92 taken while preserving its 1.40 cooldown
  and 1.25 ultimate cadence brought the full audit to 64.0%. Exact old shipped
  presets migrate in memory; any customized value preserves the whole preset.
- Goose scored 51.5% in the fresh full audit; its honk launch-hierarchy focused
  run scored 51.2%. Honk has charge commitment, sharp distance falloff,
  velocity caps, and shared damage-scaled launch; legacy whole-kit penalties
  were eased to 0.68 dealt / 1.35 taken / 0.70 cooldown / 0.62 ultimate.
- Roadrunner's obsolete 4 power / 0.78 dealt / 1.45 taken / 0.65 cooldown /
  0.50 ultimate preset contradicted its momentum payoff. After owner feedback
  that the first pass was still weak, it moved to 7 / 1.00 / 1.08 / 1.05 / 1.00;
  its full-audit result is now 45.8% without becoming a roster outlier. Its
  Classic Round 1 rose from 12.5% to 67.2% by becoming a three-wave miniature
  gauntlet with checkpoint repairs and a second opening stock. Aggressive CPU
  navigation changes previously made it worse, so keep AI stage-routing separate.
- Phoenix's normal-attack correction now scores 38.2% in the full audit. Bat's
  eased legacy penalties score 39.7%; its strong map variance remains a useful
  owner-playtest target rather than an automatic tuning order.
- Vulture's owned crow damage/launch already inherit his outgoing multiplier.
  Its focused pass eased obsolete whole-kit penalties to 0.84 dealt / 1.26 taken /
  0.84 cooldown / 0.78 ultimate, raising its focused result from 25.7% to 45.4%
  and its latest full-audit result is 39.1%; the owner approved the feel pass.
- Global multipliers do NOT fix structural outliers.

## Working with the owner

- He playtests every feel change and reports back plainly ("feels like
  lagging", "still off"). Ship feel changes small and flag them for playtest.
- Keep dramatic effects rare (match-end only); never stack time-freezes.
- One feature per commit; he says "ship vX.Y.Z" when he wants a release.
- Known bug pattern to watch: `Math.clamp(value, variableMin, constMax)`
  throws when variableMin > constMax — one such crash shipped for months in
  shield-block code (fixed b50a149). Use max(current, min(cap, v)) instead.

## Open threads

1. Competitive balance polish — Raven remains AI-high, while Rooster/Turkey are
   the lower end. Roadrunner is now 45.8% in the full audit; its stronger fighter
   pass and rebalanced Classic opening await owner playtesting. The Classic lab
   flags Roadrunner Round 3 as a likely AI-routing failure and the independently
   tested final boss as too hard; verify those with human play before tuning.
2. Real sprite art — pipeline complete and waiting; owner draws.
3. Rollback, matchmaking, and relay infrastructure — direct internet lockstep
   now exists, but no-setup play through CGNAT still needs a hosted relay and
   high-latency competitive play still needs rollback.
4. Per-skin sprite variants are unit-tested but not yet visually verified.
