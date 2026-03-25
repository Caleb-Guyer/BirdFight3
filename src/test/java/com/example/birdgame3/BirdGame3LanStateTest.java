package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BirdGame3LanStateTest {
    @Test
    void applyLanStateReusesExistingWorldObjectsWhenCountsStayStable() throws Exception {
        BirdGame3 game = new BirdGame3();

        LanState first = buildState(10, 20, PowerUpType.HEALTH, 120, 180, true);
        applyLanState(game, first);

        PowerUp powerRef = game.powerUps.getFirst();
        NectarNode nectarRef = game.nectarNodes.getFirst();
        SwingingVine vineRef = game.swingingVines.getFirst();
        WindVent ventRef = game.windVents.getFirst();
        CrowMinion crowRef = game.crowMinions.getFirst();
        ChickMinion chickRef = game.chickMinions.getFirst();

        LanState second = buildState(40, 55, PowerUpType.OVERCHARGE, 260, 320, false);
        applyLanState(game, second);

        assertSame(powerRef, game.powerUps.getFirst());
        assertSame(nectarRef, game.nectarNodes.getFirst());
        assertSame(vineRef, game.swingingVines.getFirst());
        assertSame(ventRef, game.windVents.getFirst());
        assertSame(crowRef, game.crowMinions.getFirst());
        assertSame(chickRef, game.chickMinions.getFirst());

        assertEquals(40.0, game.powerUps.getFirst().x);
        assertEquals(55.0, game.powerUps.getFirst().y);
        assertEquals(PowerUpType.OVERCHARGE, game.powerUps.getFirst().type);
        assertEquals(260.0, game.nectarNodes.getFirst().x);
        assertEquals(320.0, game.nectarNodes.getFirst().y);
        assertEquals(false, game.nectarNodes.getFirst().isSpeed);
        assertEquals(41.0, game.crowMinions.getFirst().x);
        assertEquals(56.0, game.crowMinions.getFirst().y);
        assertEquals(7, game.crowMinions.getFirst().age);
        assertEquals(42.0, game.chickMinions.getFirst().x);
        assertEquals(57.0, game.chickMinions.getFirst().y);
        assertEquals(3.5, game.chickMinions.getFirst().vx);
        assertEquals(5, game.chickMinions.getFirst().life);
    }

    @Test
    void applyLanStateReplacesChickWhenVariantChanges() throws Exception {
        BirdGame3 game = new BirdGame3();

        applyLanState(game, buildState(10, 20, PowerUpType.HEALTH, 120, 180, true));
        ChickMinion firstChick = game.chickMinions.getFirst();

        LanState updated = buildState(10, 20, PowerUpType.HEALTH, 120, 180, true);
        updated.chickMinions.getFirst().variant = 2;
        updated.chickMinions.getFirst().ultimate = false;
        applyLanState(game, updated);

        ChickMinion secondChick = game.chickMinions.getFirst();
        assertEquals(2, secondChick.variant);
        assertEquals(false, secondChick.ultimate);
        org.junit.jupiter.api.Assertions.assertNotSame(firstChick, secondChick);
    }

    private static LanState buildState(double x, double y, PowerUpType powerUpType,
                                       double nectarX, double nectarY, boolean isSpeed) {
        LanState state = new LanState();
        state.activePlayers = 1;

        LanState.PowerUpState power = new LanState.PowerUpState();
        power.x = x;
        power.y = y;
        power.typeOrdinal = powerUpType.ordinal();
        state.powerUps.add(power);

        LanState.NectarNodeState nectar = new LanState.NectarNodeState();
        nectar.x = nectarX;
        nectar.y = nectarY;
        nectar.isSpeed = isSpeed;
        nectar.active = true;
        state.nectarNodes.add(nectar);

        LanState.SwingingVineState vine = new LanState.SwingingVineState();
        vine.baseX = 300;
        vine.baseY = 200;
        vine.length = 160;
        vine.angle = 0.3;
        vine.angularVelocity = 0.02;
        state.swingingVines.add(vine);

        LanState.WindVentState vent = new LanState.WindVentState();
        vent.x = 500;
        vent.y = 600;
        vent.w = 220;
        vent.cooldown = 18;
        state.windVents.add(vent);

        LanState.CrowMinionState crow = new LanState.CrowMinionState();
        crow.x = x + 1;
        crow.y = y + 1;
        crow.age = 7;
        crow.ownerIndex = -1;
        crow.hasCrown = true;
        state.crowMinions.add(crow);

        LanState.ChickMinionState chick = new LanState.ChickMinionState();
        chick.x = x + 2;
        chick.y = y + 2;
        chick.vx = 3.5;
        chick.age = 9;
        chick.ownerIndex = -1;
        chick.variant = 1;
        chick.life = 5;
        chick.ultimate = true;
        state.chickMinions.add(chick);

        return state;
    }

    private static void applyLanState(BirdGame3 game, LanState state) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("applyLanState", LanState.class);
        method.setAccessible(true);
        method.invoke(game, state);
    }
}
