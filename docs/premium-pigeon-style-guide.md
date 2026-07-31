# Premium Pigeon — Visual Benchmark

This document defines the first production-quality visual target for Bird Fight 3.
Pigeon is the benchmark character: later fighter art should match its readability,
animation clarity, and finish without copying its silhouette.

## Creative direction

- **Tone:** competitive, energetic, and heroic; never cute, plush, or slapstick.
- **Medium:** hand-painted 2D animation with crisp graphic edges and restrained texture.
- **Readability:** the silhouette and current action must remain clear at an 80 px in-game height.
- **Shape language:** compact street fighter — broad chest, low center of gravity, swept wings,
  short orange-red feet, and a sharp forward-facing head.
- **Identity:** blue-gray body, charcoal flight feathers, pale gray face, iridescent teal-to-violet
  neck, amber eye, and a small dark beak. Pigeon must not wear clothing or carry a weapon.
- **Lighting:** fixed upper-left key light with a narrow cool rim; never bake stage-colored light
  into the character.
- **Outline:** dark blue-charcoal outer contour; interior feather separations use thinner,
  lower-contrast lines.

## Palette

| Role | Color |
| --- | --- |
| Outer contour | `#17202D` |
| Main body | `#71839A` |
| Shadow feathers | `#344257` |
| Face highlight | `#B9C4D0` |
| Neck teal | `#1FA6A0` |
| Neck violet | `#7050A8` |
| Eye | `#F3B13F` |
| Beak | `#252D38` |
| Feet | `#C95C46` |
| Primary VFX | `#70D7FF` |
| Royal/ultimate accent | `#F3C94C` |

## Animation language

- Every action follows **anticipation → contact → recovery**.
- Preserve the body volume and markings in every frame.
- Idle motion is controlled breathing and a vigilant head shift, not bouncing.
- Running is a low, forceful sprint with a stable head and driving wings.
- Flap frames emphasize lift by opening the primary feathers into one readable fan.
- The basic attack is a committed forward wing/elbow strike with one unmistakable contact frame.
- Hitstun bends the silhouette away from the impact; do not use stars or comedy symbols.
- Shield is a braced defensive pose; the game draws the energy shield separately.
- Dodge compresses into a fast aerodynamic slip.
- KO is a readable loss of control, ending in a stable non-looping pose.

## Sprite-sheet contract

- Authoring/hot-reload file: `sprites/pigeon.png`; Maven also bundles this pair under `/sprites`
  so packaged builds receive the production art while external files remain overrideable.
- Grid: 4 columns × 9 rows
- Frame: 160 × 160 px
- Canvas: 640 × 1440 px
- Background: transparent
- Art faces right and stays fully inside each frame.
- Row order: `idle`, `run`, `flap`, `fall`, `attack`, `hitstun`, `shield`, `dodge`, `ko`.
- Contact points should remain stable: lowest painted pixel at y=154 with a transparent bottom
  margin; body center stays near x=80. Airborne poses may shift vertically to preserve silhouette.

## Review gate

The benchmark is accepted only when:

1. Every frame reads clearly against bright and dark stages.
2. No pose changes the bird's identity, proportions, palette, or facing direction.
3. The feet do not visibly slide during idle or run playback.
4. Attack anticipation, contact, and recovery are distinguishable without VFX.
5. Alpha edges are clean at 100% and 50% scale.
6. Training hot-reload reports the Pigeon sheet as loaded.
7. Gameplay remains deterministic; animation clocks consume simulation ticks only.

## Live visual QA result

- Battlefield training verified the sprite against a bright stage with both facings.
- Ground contact and mirroring passed; combat presentation scale is 1.30 so the painted
  silhouette matches the visual weight of the vector roster despite transparent frame padding.
- Run uses 4 ticks per frame (about 67 ms) and reads as a complete four-pose cycle.
- Attack uses 1 tick per frame (about 17 ms), allowing all four authored poses to play
  during the short live normal-attack state before the last pose holds or returns to idle.
- Roster previews normalize that world scale independently, remove horizontal bias, and lift
  the feet-anchored frame 6.5% so the visible bird is centered in character tiles.
- Feather Burst, Street Rush, Fire-Escape Flutter, Drop Peck, blocked attacks, and Rooftop
  Coronation now use distinct presentation audio and the shared directional impact system.
- Live Feather Burst contact verified the four-damage hit spark, feather trail, hitstop, shake,
  and target hitstun sequence without changing move damage or launch values.

## Production prompt

Use case: stylized-concept<br>
Asset type: production 2D fighting-game sprite atlas<br>
Primary request: one consistent heroic urban pigeon performing nine animation sequences<br>
Style/medium: hand-painted 2D game animation, crisp graphic contour, restrained feather texture<br>
Composition: exact 4-column by 9-row atlas; one 160 px square frame per cell; art faces right<br>
Constraints: identical character design and scale in all 36 frames; no text, labels, grid lines,
weapons, clothing, cast shadows, scenery, logos, or watermarks
