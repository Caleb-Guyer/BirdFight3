package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdVisualAuditTest {

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node("/birdfight3-tests/visual-audit/" + UUID.randomUUID()));
    }

    @Test
    void auditCatalogIncludesEveryBirdAndEveryFeatherpediaSkinExactlyOnce() {
        List<BirdGame3.VisualAuditSkin> entries = freshGame().visualAuditSkins();
        Set<String> identities = new HashSet<>();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            String identity = entry.bird().name() + ":" + (entry.key() == null ? "BASE" : entry.key());
            assertTrue(identities.add(identity), "Duplicate visual-audit entry: " + identity);
            assertTrue(entry.name() != null && !entry.name().isBlank(), identity + " needs a review label");
        }

        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            assertTrue(identities.contains(type.name() + ":BASE"), type + " base art is missing from the audit");
        }
        assertTrue(entries.size() >= BirdGame3.BirdType.values().length * 2,
                "The audit should cover base birds plus the complete Featherpedia skin catalog");
    }

    @Test
    void auditCoversTheRequestedPresentationAndCombatPoses() {
        assertEquals(List.of("IDLE", "RUN", "FLAP", "ATTACK", "HIT", "KO"),
                java.util.Arrays.stream(Bird.VisualAuditPose.values()).map(Enum::name).toList());
    }
}
