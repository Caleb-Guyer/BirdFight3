# Gameplay polish — 2026-09-08

This pass fixes platform contact and recovery reliability. Fighter tuning stays
unchanged. The normal interface, fighter selector, Replay Studio, and Flock Run
remain intact.

## Changes

- Landing follows the feet across the tick and selects the first crossed top
  surface, including respawn nests. Fast descents cannot skip a thin platform,
  and list order cannot select a lower overlapping platform first.
- Descending from underneath or drifting in after the feet pass a platform lip
  cannot snap the fighter upward. Underside overlap no longer counts as grounded
  or refreshes recovery abilities.
- Walking off edges and jumping upward through platforms remain possible.
  Ground contact continues to refresh normal air resources after a real landing.
- Runtime growth and shrinking keep grounded feet and the horizontal center
  anchored. This covers Titan pickups and mutators, shrink expiry, base-size
  changes, Phoenix rebirth, and Turkey's Classic size effects.

The original collision failures were reproduced before the fix. Follow-up tests
also caught false recovery refreshes and size changes that could put the feet
under the new collision surface; those cases are covered by the completed fix.

## Validation

- `mvnw.cmd -B -ntp package`: 1,603 tests passed, zero failures or errors.
- Thirteen landing regression tests cover the above behavior; the core cases
  exercise Titmouse, Pelican, and Raven through real fighter code.
- Existing determinism, replay, lockstep input, controller mapping, ledge,
  platform-drop, moving-platform, and fighter ability tests passed.
- Distribution launcher verification and the bundled audio audit passed.
- Automated checks use headless simulation. Physical controller feel and human
  recovery play have not been verified in this pass.

## Seeded match comparison

Each run uses 1,008 AI matches: the three focus fighters against all 21 opponents,
two matches per side on Battlefield, Forest, Worldseam, and Cave. Seeds start at
20260908 and use the same iteration order before and after. The cap is 14,400
ticks; the balance harness resolves unfinished matches by stocks and then health.
Win rates exclude draws. This is a focused sample, not the full roster audit or
a human balance verdict.

The final run uses a fresh JVM, as did the baseline. Reproduce it with
`mvnw.cmd test -Dtest=GameplayPolishAuditRun`; the default output is
`target/gameplay-polish-after.md`.

| Fighter | Stage | Before W–L–D | After W–L–D | Before win rate | After win rate |
|---|---|---:|---:|---:|---:|
| Titmouse | Battlefield | 17–67–0 | 11–73–0 | 20.2% | 13.1% |
| Titmouse | Forest | 28–56–0 | 13–71–0 | 33.3% | 15.5% |
| Titmouse | Worldseam | 42–42–0 | 39–45–0 | 50.0% | 46.4% |
| Titmouse | Cave | 14–70–0 | 11–73–0 | 16.7% | 13.1% |
| Pelican | Battlefield | 20–64–0 | 21–63–0 | 23.8% | 25.0% |
| Pelican | Forest | 45–39–0 | 42–42–0 | 53.6% | 50.0% |
| Pelican | Worldseam | 8–76–0 | 6–78–0 | 9.5% | 7.1% |
| Pelican | Cave | 43–41–0 | 47–36–1 | 51.2% | 56.6% |
| Raven | Battlefield | 60–24–0 | 64–20–0 | 71.4% | 76.2% |
| Raven | Forest | 56–28–0 | 59–25–0 | 66.7% | 70.2% |
| Raven | Worldseam | 47–37–0 | 58–26–0 | 56.0% | 69.0% |
| Raven | Cave | 57–27–0 | 52–31–1 | 67.9% | 62.7% |

Across these four stages, Titmouse moved from 30.1% to 22.0%, Pelican from 34.5%
to 34.6%, and Raven from 65.5% to 69.6%. Draws increased from zero to two, both
on Cave. The sample therefore leaves a wider AI balance gap. Titmouse's CPU plan
and Pelican's Worldseam routing remain follow-up leads; verify their human
recoveries and matchups before making global tuning changes. These figures use
four stages and should not replace the existing 21-stage balance report.

## Owner playtest

1. With Titmouse, Pelican, and Raven, recover from each side of Battlefield and
   Worldseam. Check ledge grabs, drift onto platforms, and the next up-special
   after landing. Note the fighter, stage, and input when a recovery feels wrong.
2. Fall quickly onto side platforms, jump through them from underneath, walk off
   their edges, and use down+jump to drop through. Check that neither an underside
   brush nor a near miss grants another recovery.
3. Try the same recoveries with the controller. Check jump, shield, air dodge,
   and direction changes around landing. Automated input tests cannot establish
   how responsive the physical device feels.
4. Pick up Titan while standing on a platform and let a shrink effect expire
   there. The fighter should keep the same support point.

## Compatibility

Landing physics changed, so network protocol 75 and replay simulation revision
14 prevent silent mismatches. Network peers need matching builds. Existing replay
files remain stored, but older recordings require their matching simulation;
the replay binary format and profile/save format have not changed.
