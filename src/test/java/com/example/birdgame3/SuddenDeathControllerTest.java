package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuddenDeathControllerTest {
    @Test
    void earlySuddenDeathSpawnsOnlyMurderCrows() {
        SuddenDeathController controller = new SuddenDeathController();
        List<CrowMinion> crows = new ArrayList<>();

        controller.start();
        advance(controller, crows, 140);

        assertTrue(crows.size() >= 2);
        assertTrue(crows.stream().allMatch(crow -> crow.effectiveVariant() == CrowMinion.VARIANT_MURDER_CROW));
        assertTrue(crows.stream().allMatch(CrowMinion::isOverflowProtected));
    }

    @Test
    void laterSuddenDeathAddsNullRockVariantsAndFasterWaves() {
        SuddenDeathController controller = new SuddenDeathController();
        List<CrowMinion> crows = new ArrayList<>();

        controller.start();
        advance(controller, crows, 4_000);

        assertTrue(crows.stream().anyMatch(crow -> crow.effectiveVariant() == CrowMinion.VARIANT_GIANT_CROW
                || crow.effectiveVariant() == CrowMinion.VARIANT_RAVEN
                || crow.effectiveVariant() == CrowMinion.VARIANT_VOID_RAVEN));
        double fastestMultiplier = crows.stream().mapToDouble(crow -> crow.speedMultiplier).max().orElse(1.0);
        assertTrue(fastestMultiplier > 1.2);
        long murderCount = crows.stream().filter(crow -> crow.effectiveVariant() == CrowMinion.VARIANT_MURDER_CROW).count();
        assertTrue(murderCount > 0);
    }

    private static void advance(SuddenDeathController controller, List<CrowMinion> crows, int frames) {
        List<PiranhaHazard> piranhas = new ArrayList<>();
        Random random = new Random(7L);
        double shake = 0.0;
        for (int i = 0; i < frames; i++) {
            shake = controller.updateAndSpawn(
                    crows,
                    piranhas,
                    random,
                    shake,
                    false,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }
        assertEquals(15.0, shake, 0.0001);
    }
}
