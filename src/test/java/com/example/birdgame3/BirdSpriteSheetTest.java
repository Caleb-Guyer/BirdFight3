package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdSpriteSheetTest {

    private record PigeonVariant(String suffix, String skinKey) {
    }

    private static Properties minimalProps() {
        Properties props = new Properties();
        props.setProperty("frameWidth", "80");
        props.setProperty("frameHeight", "80");
        props.setProperty("idle.row", "0");
        props.setProperty("idle.frames", "4");
        props.setProperty("idle.ticksPerFrame", "8");
        return props;
    }

    @Test
    void loopingAnimationWrapsAndNonLoopingClamps() {
        BirdSpriteSheet.Animation looping = new BirdSpriteSheet.Animation(0, 4, 8, true);
        assertEquals(0, looping.frameIndexAt(0));
        assertEquals(0, looping.frameIndexAt(7));
        assertEquals(1, looping.frameIndexAt(8));
        assertEquals(3, looping.frameIndexAt(31));
        assertEquals(0, looping.frameIndexAt(32), "Looping animation should wrap to frame 0.");

        BirdSpriteSheet.Animation oneShot = new BirdSpriteSheet.Animation(0, 4, 8, false);
        assertEquals(3, oneShot.frameIndexAt(31));
        assertEquals(3, oneShot.frameIndexAt(500), "One-shot animation should hold its last frame.");
    }

    @Test
    void singleFrameAnimationAlwaysShowsFrameZero() {
        BirdSpriteSheet.Animation still = new BirdSpriteSheet.Animation(2, 1, 8, true);
        assertEquals(0, still.frameIndexAt(0));
        assertEquals(0, still.frameIndexAt(1000));
    }

    @Test
    void parsesMetadataAndAppliesFallbackChain() {
        Properties props = minimalProps();
        props.setProperty("flap.row", "1");
        props.setProperty("flap.frames", "2");

        BirdSpriteSheet sheet = BirdSpriteSheet.fromProperties(null, props);

        assertNotNull(sheet);
        assertEquals(80, sheet.frameWidth);
        assertEquals(1.0, sheet.scale);
        assertEquals(0, sheet.animationFor("idle").row());
        assertEquals(1, sheet.animationFor("flap").row());
        assertEquals(1, sheet.animationFor("fall").row(), "Missing 'fall' should fall back to 'flap'.");
        assertEquals(1, sheet.animationFor("dodge").row(), "Missing 'dodge' should fall back through 'flap'.");
        assertEquals(0, sheet.animationFor("attack").row(), "Missing 'attack' should fall back to 'idle'.");
        assertEquals(0, sheet.animationFor("run").row(), "Missing 'run' should fall back to 'idle'.");
    }

    @Test
    void skinSuffixMatchingIsForgivingAboutCaseAndUnderscores() {
        assertTrue(BirdSpriteLibrary.skinSuffixMatches(
                BirdSpriteLibrary.normalizeSkinToken("noir"),
                BirdSpriteLibrary.normalizeSkinToken("NOIR_PIGEON_SKIN")));
        assertTrue(BirdSpriteLibrary.skinSuffixMatches(
                BirdSpriteLibrary.normalizeSkinToken("sky_king"),
                BirdSpriteLibrary.normalizeSkinToken("SKY_KING_EAGLE")));
        assertTrue(BirdSpriteLibrary.skinSuffixMatches(
                BirdSpriteLibrary.normalizeSkinToken("NOIR-PIGEON-SKIN"),
                BirdSpriteLibrary.normalizeSkinToken("NOIR_PIGEON_SKIN")));
        assertFalse(BirdSpriteLibrary.skinSuffixMatches(
                BirdSpriteLibrary.normalizeSkinToken("classic"),
                BirdSpriteLibrary.normalizeSkinToken("NOIR_PIGEON_SKIN")));
        assertFalse(BirdSpriteLibrary.skinSuffixMatches(
                BirdSpriteLibrary.normalizeSkinToken(""),
                BirdSpriteLibrary.normalizeSkinToken("NOIR_PIGEON_SKIN")),
                "An empty suffix must never match.");
    }

    @Test
    void rejectsSheetsMissingRequiredMetadata() {
        Properties noFrameSize = new Properties();
        noFrameSize.setProperty("idle.row", "0");
        assertNull(BirdSpriteSheet.fromProperties(null, noFrameSize));

        Properties noIdle = new Properties();
        noIdle.setProperty("frameWidth", "80");
        noIdle.setProperty("frameHeight", "80");
        noIdle.setProperty("attack.row", "0");
        assertNull(BirdSpriteSheet.fromProperties(null, noIdle),
                "A sheet without an idle animation is unusable.");
    }

    @Test
    void bundledPigeonAtlasMatchesItsAnimationMetadata() throws Exception {
        try (InputStream imageIn = BirdSpriteLibrary.class.getResourceAsStream("/sprites/pigeon.png");
             InputStream propsIn = BirdSpriteLibrary.class.getResourceAsStream("/sprites/pigeon.properties")) {
            assertNotNull(imageIn, "The production Pigeon atlas must be packaged with the game.");
            assertNotNull(propsIn, "The production Pigeon metadata must be packaged with the game.");

            BufferedImage image = ImageIO.read(imageIn);
            assertNotNull(image);
            assertEquals(640, image.getWidth());
            assertEquals(1440, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha(), "The production atlas must retain transparency.");

            Properties props = new Properties();
            props.load(propsIn);
            assertEquals(1.30, Double.parseDouble(props.getProperty("scale")), 0.0001,
                    "Pigeon's padded art needs a larger in-world presentation scale.");
            assertEquals("160", props.getProperty("frameWidth"));
            assertEquals("160", props.getProperty("frameHeight"));
            for (int row = 0; row < BirdSpriteSheet.STATE_NAMES.size(); row++) {
                String state = BirdSpriteSheet.STATE_NAMES.get(row);
                assertEquals(Integer.toString(row), props.getProperty(state + ".row"));
                assertEquals("4", props.getProperty(state + ".frames"));
            }
            assertEquals("1", props.getProperty("attack.ticksPerFrame"),
                    "The four attack poses must fit Pigeon's live normal-attack state window.");
        }
    }

    @Test
    void bundledPigeonSkinAtlasesAreDistinctCompleteAndMatchTheirSkinKeys() throws Exception {
        List<PigeonVariant> variants = List.of(
                new PigeonVariant("city_pigeon", "CITY_PIGEON"),
                new PigeonVariant("noir_pigeon", "NOIR_PIGEON"),
                new PigeonVariant("freeman_pigeon", "FREEMAN_PIGEON"),
                new PigeonVariant("beacon_pigeon", "BEACON_PIGEON"),
                new PigeonVariant("storm_pigeon", "STORM_PIGEON")
        );
        Set<String> colorSignatures = new HashSet<>();

        assertEquals(variants.stream().map(PigeonVariant::suffix).toList(),
                BirdSpriteLibrary.bundledVariantSuffixesFor(BirdGame3.BirdType.PIGEON),
                "Every packaged Pigeon skin atlas must be present in the bundled variant index");

        for (PigeonVariant variant : variants) {
            String stem = "pigeon-" + variant.suffix();
            try (InputStream imageIn = BirdSpriteLibrary.class.getResourceAsStream("/sprites/" + stem + ".png");
                 InputStream propsIn = BirdSpriteLibrary.class.getResourceAsStream("/sprites/" + stem + ".properties")) {
                assertNotNull(imageIn, stem + " must be packaged with the game");
                assertNotNull(propsIn, stem + " metadata must be packaged with the game");

                BufferedImage image = ImageIO.read(imageIn);
                assertNotNull(image);
                assertEquals(640, image.getWidth());
                assertEquals(1440, image.getHeight());
                assertTrue(image.getColorModel().hasAlpha(), stem + " must retain transparency");
                assertEquals(0, (image.getRGB(0, 0) >>> 24) & 0xFF,
                        stem + " must have a transparent outer canvas");

                long red = 0;
                long green = 0;
                long blue = 0;
                long opaque = 0;
                for (int row = 0; row < BirdSpriteSheet.STATE_NAMES.size(); row++) {
                    int visibleInRow = 0;
                    for (int y = row * 160; y < (row + 1) * 160; y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            int argb = image.getRGB(x, y);
                            int alpha = (argb >>> 24) & 0xFF;
                            if (alpha > 16) {
                                visibleInRow++;
                            }
                            if (alpha > 128) {
                                red += (argb >>> 16) & 0xFF;
                                green += (argb >>> 8) & 0xFF;
                                blue += argb & 0xFF;
                                opaque++;
                            }
                        }
                    }
                    assertTrue(visibleInRow > 3_000,
                            stem + " is missing visible art in animation row " + row);
                }
                assertTrue(opaque > 0);
                String signature = (red / opaque) + ":" + (green / opaque) + ":" + (blue / opaque);
                assertTrue(colorSignatures.add(signature), stem + " must have a distinct skin palette");

                Properties props = new Properties();
                props.load(propsIn);
                assertEquals("160", props.getProperty("frameWidth"));
                assertEquals("160", props.getProperty("frameHeight"));
                assertTrue(BirdSpriteLibrary.skinSuffixMatches(
                                BirdSpriteLibrary.normalizeSkinToken(variant.suffix()),
                                BirdSpriteLibrary.normalizeSkinToken(variant.skinKey())),
                        stem + " filename must match its runtime skin key");
            }
        }
    }

    @Test
    void runtimeSelectsEachPigeonSkinAtlasInsteadOfFallingBackToNormalPigeon() {
        BirdSpriteLibrary.reload();
        BirdSpriteSheet base = BirdSpriteLibrary.sheetFor(BirdGame3.BirdType.PIGEON);
        assertNotNull(base);

        Set<BirdSpriteSheet> selectedVariants = new HashSet<>();
        for (String skinKey : List.of(
                "CITY_PIGEON",
                "NOIR_PIGEON",
                "FREEMAN_PIGEON",
                "BEACON_PIGEON",
                "STORM_PIGEON")) {
            BirdSpriteSheet variant = BirdSpriteLibrary.sheetFor(BirdGame3.BirdType.PIGEON, skinKey);
            assertNotNull(variant, skinKey + " must resolve to a sprite atlas");
            assertNotSame(base, variant, skinKey + " must not fall back to normal Pigeon");
            assertTrue(selectedVariants.add(variant), skinKey + " must resolve to a unique atlas");
        }
    }
}
