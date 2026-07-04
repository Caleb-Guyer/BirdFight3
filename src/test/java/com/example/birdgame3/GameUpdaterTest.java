package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameUpdaterTest {

    @Test
    void versionComparisonIsNumericNotLexicographic() {
        assertTrue(GameUpdater.isNewerVersion("1.10.0", "1.9.0"));
        assertTrue(GameUpdater.isNewerVersion("2.0.0", "1.99.99"));
        assertTrue(GameUpdater.isNewerVersion("1.0.1", "1.0.0"));
        assertTrue(GameUpdater.isNewerVersion("1.1", "1.0.5"));
        assertFalse(GameUpdater.isNewerVersion("1.0.0", "1.0.0"));
        assertFalse(GameUpdater.isNewerVersion("1.0.0", "1.0.1"));
        assertFalse(GameUpdater.isNewerVersion("0.9.9", "1.0.0"));
    }

    @Test
    void malformedVersionSegmentsCountAsZero() {
        assertFalse(GameUpdater.isNewerVersion("abc", "1.0.0"));
        assertTrue(GameUpdater.isNewerVersion("1.0.1", "1.0.x"));
    }

    @Test
    void extractsTagAndWinZipAssetFromReleaseJson() {
        String json = """
                {
                  "tag_name": "v1.2.0",
                  "assets": [
                    {"name": "BirdFight3-1.2.0-mac.dmg",
                     "browser_download_url": "https://github.com/x/y/releases/download/v1.2.0/BirdFight3-1.2.0-mac.dmg"},
                    {"name": "BirdFight3-1.2.0-win.zip",
                     "browser_download_url": "https://github.com/x/y/releases/download/v1.2.0/BirdFight3-1.2.0-win.zip"}
                  ]
                }
                """;
        Pattern tag = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern asset = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]*?-win\\.zip)\"");

        assertEquals("v1.2.0", GameUpdater.firstMatch(tag, json));
        assertEquals("https://github.com/x/y/releases/download/v1.2.0/BirdFight3-1.2.0-win.zip",
                GameUpdater.firstMatch(asset, json));
        assertNull(GameUpdater.firstMatch(asset, "{\"assets\": []}"),
                "A release without a -win.zip asset must not match.");
    }
}
