# Reward and confirmation UI

This pass adapts the owner's reward-screen references to Bird Fight 3's own art.
It does not bundle artwork or logos from the reference game.

## Presentation

- Fighter unlocks use a blue, angled splash with a large bird portrait and a
  "Takes flight!" callout.
- Skin unlocks use a gold showcase with the actual unlocked appearance.
- Stage unlocks use a teal showcase with the current stage capture, shared with
  stage selection and the Featherpedia.
- Achievement rewards use the same presentation, with dedicated coin and
  Classic-continue artwork when appropriate.
- Pack receipts display three illustrated rewards at a time. Additional pulls
  can be paged through without hiding their names or granting them again.
- Modal confirmations use a light content area, strong header, and measured
  button widths. Confirmation dialogs default to No/Cancel; text-entry dialogs
  retain their normal Enter-to-submit behavior.

Entry/exit transitions are finite and use the existing menu animation helpers.
Input prompts follow the active input device. Reveal confirmation waits for a
fresh key press and release, so holding Enter cannot skip the unlock queue.

## Safety

`RewardPresentation` and `ShopPackResult` contain display data, not grant
callbacks. Pack odds, duplicate prevention, coin amounts, unlock ownership, and
achievement rewards are unchanged. Displaying the receipt never grants it again.

Artwork uses the clean cutscene pose and existing portrait framing. The
feet-anchored Stock Photo Turkey skin has an explicit framing correction.
Rendering must not consume `SimRng` or write progression.

## Verification

Regular checks:

```powershell
.\mvnw.cmd test
```

Optional offscreen integration/capture audit:

```powershell
.\mvnw.cmd test -Dtest=RewardUiVisualAuditRun
```

The optional audit does not show a window or launch the game. It uses an isolated
test profile, checks all 71 current bird/skin portraits for clipped artwork,
checks simulation-RNG isolation, renders five reveal types at 1280x720 and
2560x1600, checks complete label text, pack paging, Yes/No button widths, queued
input, and the deferred-entrance/early-exit race. PNGs are written to the ignored
`target/reward-ui-audit/` directory.

The regular regression tests also cover 1024x768 and 1920x1080 layout geometry,
long names, receipt immutability, original fallback coin values, and one-time
unlock grants. On 2026-08-25, all 1,451 regular tests and the optional offscreen
audit passed. These checks do not replace hands-on controller or display testing.
