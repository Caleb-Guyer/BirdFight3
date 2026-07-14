package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdSpriteSheetTest {

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
}
