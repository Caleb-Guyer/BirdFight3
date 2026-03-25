package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractsBoardTest {
    private static final List<ContractsBoard.ContractDefinition> DAILY_POOL = List.of(
            new ContractsBoard.ContractDefinition("daily_matches", ContractsBoard.ContractFrequency.DAILY, ContractsBoard.ContractMetric.MATCHES,
                    "Open Skies", "Play 2 matches.", 2, 100, 10),
            new ContractsBoard.ContractDefinition("daily_damage", ContractsBoard.ContractFrequency.DAILY, ContractsBoard.ContractMetric.DAMAGE,
                    "Heavy Feathers", "Deal 100 damage.", 100, 120, 12),
            new ContractsBoard.ContractDefinition("daily_specials", ContractsBoard.ContractFrequency.DAILY, ContractsBoard.ContractMetric.SPECIALS_USED,
                    "Special Delivery", "Use 4 specials.", 4, 90, 8)
    );

    private static final List<ContractsBoard.ContractDefinition> WEEKLY_POOL = List.of(
            new ContractsBoard.ContractDefinition("weekly_maps", ContractsBoard.ContractFrequency.WEEKLY, ContractsBoard.ContractMetric.UNIQUE_MAP_WINS,
                    "World Tour", "Win on 2 different maps.", 2, 250, 20)
    );

    private Preferences prefs;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("/birdfight3-tests/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
        prefs.flush();
    }

    @Test
    void matchProgressPersistsAcrossSaveAndLoad() {
        LocalDate date = LocalDate.of(2026, 3, 24);
        ContractsBoard board = new ContractsBoard(DAILY_POOL, WEEKLY_POOL);
        board.load(prefs, date);

        ContractsBoard.UpdateResult update = board.applyMatch(new ContractsBoard.MatchStats(
                true,
                BirdGame3.MapType.FOREST,
                1,
                1,
                60,
                1,
                2,
                1
        ));

        assertTrue(update.hasChanges());
        board.save(prefs);

        ContractsBoard restored = new ContractsBoard(DAILY_POOL, WEEKLY_POOL);
        restored.load(prefs, date);
        Map<String, ContractsBoard.ContractView> byId = viewsById(restored.activeContracts());

        assertEquals(1, byId.get("daily_matches").progress());
        assertEquals(60, byId.get("daily_damage").progress());
        assertEquals(2, byId.get("daily_specials").progress());
        assertEquals(1, byId.get("weekly_maps").progress());
    }

    @Test
    void weeklyMapWinsOnlyCountUniqueWinningMaps() {
        LocalDate date = LocalDate.of(2026, 3, 24);
        ContractsBoard board = new ContractsBoard(DAILY_POOL, WEEKLY_POOL);
        board.load(prefs, date);

        ContractsBoard.UpdateResult first = board.applyMatch(new ContractsBoard.MatchStats(
                true,
                BirdGame3.MapType.FOREST,
                1,
                1,
                10,
                0,
                0,
                0
        ));
        assertTrue(first.hasChanges());
        assertEquals(1, viewsById(board.activeContracts()).get("weekly_maps").progress());

        ContractsBoard.UpdateResult second = board.applyMatch(new ContractsBoard.MatchStats(
                true,
                BirdGame3.MapType.FOREST,
                1,
                1,
                10,
                0,
                0,
                0
        ));
        assertFalse(second.completedContracts().stream().anyMatch(reward -> "weekly_maps".equals(reward.contract().id())));
        assertEquals(1, viewsById(board.activeContracts()).get("weekly_maps").progress());

        ContractsBoard.UpdateResult third = board.applyMatch(new ContractsBoard.MatchStats(
                true,
                BirdGame3.MapType.CITY,
                1,
                1,
                10,
                0,
                0,
                0
        ));
        assertTrue(third.completedContracts().stream().anyMatch(reward -> "weekly_maps".equals(reward.contract().id())));
        assertEquals(250, third.coinsAwarded());
        assertEquals(20, third.masteryXpAwarded());
        assertEquals(2, viewsById(board.activeContracts()).get("weekly_maps").progress());
        assertTrue(viewsById(board.activeContracts()).get("weekly_maps").completed());
    }

    private static Map<String, ContractsBoard.ContractView> viewsById(List<ContractsBoard.ContractView> views) {
        return views.stream().collect(Collectors.toMap(ContractsBoard.ContractView::id, Function.identity()));
    }
}
