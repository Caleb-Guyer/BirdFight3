package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, typed definition of the definitive single-player campaign.
 *
 * <p>The content intentionally lives outside {@link BirdGame3}. The game still
 * owns rendering and combat, while this model owns narrative order, authored
 * rosters, objectives, handoffs, and stable save identifiers.
 */
final class StoryCampaign {
    static final int CURRENT_VERSION = 1;

    enum Difficulty {
        EASY("Easy", 3, 0.85, 1.25, true),
        NORMAL("Normal", 5, 1.0, 1.0, false),
        HARD("Hard", 7, 1.10, 0.90, false);

        final String label;
        final int cpuLevel;
        final double enemyHealthScale;
        final double objectiveWindowScale;
        final boolean bonusHealthPickup;

        Difficulty(String label, int cpuLevel, double enemyHealthScale,
                   double objectiveWindowScale, boolean bonusHealthPickup) {
            this.label = label;
            this.cpuLevel = cpuLevel;
            this.enemyHealthScale = enemyHealthScale;
            this.objectiveWindowScale = objectiveWindowScale;
            this.bonusHealthPickup = bonusHealthPickup;
        }

        static Difficulty fromName(String value) {
            if (value != null) {
                try {
                    return valueOf(value);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return NORMAL;
        }
    }

    enum PlayableKind { FORCED, CHOICE, FULL_ROSTER }

    record PlayablePolicy(PlayableKind kind, List<BirdGame3.BirdType> birds) {
        PlayablePolicy {
            kind = Objects.requireNonNull(kind, "kind");
            birds = birds == null ? List.of() : List.copyOf(birds);
            if (kind == PlayableKind.FORCED && birds.size() != 1) {
                throw new IllegalArgumentException("Forced policy needs exactly one bird");
            }
            if (kind == PlayableKind.CHOICE && (birds.size() < 2 || birds.size() > 4)) {
                throw new IllegalArgumentException("Choice policy needs two to four birds");
            }
            if (kind == PlayableKind.FULL_ROSTER && !birds.isEmpty()) {
                throw new IllegalArgumentException("Full-roster policy derives its birds from BirdType");
            }
        }

        static PlayablePolicy forced(BirdGame3.BirdType bird) {
            return new PlayablePolicy(PlayableKind.FORCED, List.of(bird));
        }

        static PlayablePolicy choice(BirdGame3.BirdType... birds) {
            return new PlayablePolicy(PlayableKind.CHOICE, List.of(birds));
        }

        static PlayablePolicy fullRoster() {
            return new PlayablePolicy(PlayableKind.FULL_ROSTER, List.of());
        }

        List<BirdGame3.BirdType> resolvedBirds() {
            return kind == PlayableKind.FULL_ROSTER
                    ? List.of(BirdGame3.BirdType.values())
                    : birds;
        }
    }

    enum ObjectiveType {
        ELIMINATION,
        SURVIVE,
        PROTECT,
        CAPTURE,
        REACH_EXIT,
        HOLD_ZONE,
        GAUNTLET,
        BOSS_PHASES
    }

    record MissionPhase(ObjectiveType objective, String label, int targetTicks,
                        int targetCount, boolean checkpoint) {
        MissionPhase {
            objective = Objects.requireNonNull(objective, "objective");
            label = label == null ? "" : label;
            targetTicks = Math.max(0, targetTicks);
            targetCount = Math.max(0, targetCount);
        }

        static MissionPhase elimination(String label) {
            return new MissionPhase(ObjectiveType.ELIMINATION, label, 0, 0, true);
        }

        static MissionPhase timed(ObjectiveType type, String label, int seconds, int targetCount,
                                  boolean checkpoint) {
            return new MissionPhase(type, label, Math.max(1, seconds) * 60, targetCount, checkpoint);
        }
    }

    record Fighter(BirdGame3.BirdType type, String name, int team, double health,
                   double power, double speed, String skinKey, boolean boss) {
        Fighter {
            type = Objects.requireNonNull(type, "type");
            name = name == null || name.isBlank() ? type.name : name;
            team = Math.max(1, team);
            health = Math.max(1.0, health);
            power = Math.max(0.1, power);
            speed = Math.max(0.1, speed);
        }

        static Fighter ally(BirdGame3.BirdType type, String name) {
            return new Fighter(type, name, 1, 112, 1.08, 1.06, null, false);
        }

        static Fighter enemy(BirdGame3.BirdType type, String name) {
            return new Fighter(type, name, 2, 132, 1.10, 1.06, null, false);
        }

        static Fighter enemy(BirdGame3.BirdType type, String name, String skinKey) {
            return new Fighter(type, name, 2, 132, 1.10, 1.06, skinKey, false);
        }

        static Fighter boss(BirdGame3.BirdType type, String name, double health,
                            double power, double speed, String skinKey) {
            return new Fighter(type, name, 2, health, power, speed, skinKey, true);
        }
    }

    enum ArenaVariant {
        STANDARD,
        STILLNESS,
        CROWN_OCCUPIED,
        EVACUATION,
        CARRION,
        ANCHOR_ASSAULT,
        CROWN_DUEL,
        NULL_ROC,
        NULL_ROCK
    }

    record Mission(
            String id,
            String title,
            String briefing,
            BirdGame3.MapType map,
            ArenaVariant arenaVariant,
            PlayablePolicy playable,
            List<Fighter> allies,
            List<Fighter> enemies,
            List<MissionPhase> phases,
            String preSceneId,
            String postSceneId,
            BirdGame3.BirdType recruit,
            boolean finalBoss
    ) {
        Mission {
            id = requireId(id, "mission");
            title = requireText(title, "mission title");
            briefing = requireText(briefing, "mission briefing");
            map = Objects.requireNonNull(map, "map");
            arenaVariant = arenaVariant == null ? ArenaVariant.STANDARD : arenaVariant;
            playable = Objects.requireNonNull(playable, "playable");
            allies = allies == null ? List.of() : List.copyOf(allies);
            enemies = enemies == null ? List.of() : List.copyOf(enemies);
            phases = phases == null || phases.isEmpty()
                    ? List.of(MissionPhase.elimination("Defeat the opposition"))
                    : List.copyOf(phases);
            preSceneId = requireId(preSceneId, "pre-scene");
            postSceneId = requireId(postSceneId, "post-scene");
        }
    }

    record Act(String id, String title, String summary, List<Mission> missions) {
        Act {
            id = requireId(id, "act");
            title = requireText(title, "act title");
            summary = requireText(summary, "act summary");
            missions = missions == null ? List.of() : List.copyOf(missions);
        }
    }

    enum ShotStyle {
        ESTABLISHING,
        WIDE,
        TWO_SHOT,
        CLOSE,
        PAN,
        ACTION,
        REVEAL,
        CROWD,
        BLACK
    }

    enum ActorMotion {
        IDLE,
        ENTER_LEFT,
        ENTER_RIGHT,
        FLY_BY,
        ATTACK,
        RECOIL,
        TURN_AWAY,
        EXIT_LEFT,
        EXIT_RIGHT,
        FALL,
        RISE
    }

    record DialogueLine(
            String speaker,
            BirdGame3.BirdType bird,
            String text,
            ShotStyle shot,
            ActorMotion motion,
            BirdGame3.BirdType whenSelected,
            String musicCue
    ) {
        DialogueLine {
            speaker = requireText(speaker, "speaker");
            text = requireText(text, "dialogue");
            shot = shot == null ? ShotStyle.TWO_SHOT : shot;
            motion = motion == null ? ActorMotion.IDLE : motion;
            musicCue = musicCue == null ? "" : musicCue.strip();
        }

        static DialogueLine line(String speaker, BirdGame3.BirdType bird, String text) {
            return new DialogueLine(speaker, bird, text, ShotStyle.TWO_SHOT, ActorMotion.IDLE, null, "");
        }

        static DialogueLine line(String speaker, BirdGame3.BirdType bird, String text,
                                 ShotStyle shot, ActorMotion motion) {
            return new DialogueLine(speaker, bird, text, shot, motion, null, "");
        }

        static DialogueLine selected(String speaker, BirdGame3.BirdType bird, String text,
                                     BirdGame3.BirdType selected) {
            return new DialogueLine(speaker, bird, text, ShotStyle.CLOSE, ActorMotion.IDLE, selected, "");
        }
    }

    record Cutscene(
            String id,
            String title,
            BirdGame3.MapType location,
            String musicCue,
            List<DialogueLine> lines,
            List<BirdGame3.BirdType> handoffBirds,
            boolean deathScene,
            boolean finale
    ) {
        Cutscene {
            id = requireId(id, "scene");
            title = requireText(title, "scene title");
            location = Objects.requireNonNull(location, "location");
            musicCue = musicCue == null ? "" : musicCue;
            lines = lines == null ? List.of() : List.copyOf(lines);
            handoffBirds = handoffBirds == null ? List.of() : List.copyOf(handoffBirds);
        }

        List<DialogueLine> linesFor(BirdGame3.BirdType selected) {
            if (selected == null) {
                return lines.stream().filter(line -> line.whenSelected == null).toList();
            }
            List<DialogueLine> result = new ArrayList<>();
            for (DialogueLine line : lines) {
                if (line.whenSelected == null || line.whenSelected == selected) {
                    result.add(line);
                }
            }
            return List.copyOf(result);
        }
    }

    final int version;
    final String id;
    final String title;
    final String subtitle;
    final List<Act> acts;
    final Map<String, Cutscene> scenes;
    final Map<String, Mission> missions;
    final List<Mission> orderedMissions;

    StoryCampaign(int version, String id, String title, String subtitle,
                  List<Act> acts, List<Cutscene> scenes) {
        this.version = Math.max(1, version);
        this.id = requireId(id, "campaign");
        this.title = requireText(title, "campaign title");
        this.subtitle = requireText(subtitle, "campaign subtitle");
        this.acts = acts == null ? List.of() : List.copyOf(acts);

        Map<String, Cutscene> sceneMap = new LinkedHashMap<>();
        if (scenes != null) {
            for (Cutscene scene : scenes) {
                if (sceneMap.put(scene.id, scene) != null) {
                    throw new IllegalArgumentException("Duplicate scene id: " + scene.id);
                }
            }
        }
        this.scenes = Collections.unmodifiableMap(sceneMap);

        Map<String, Mission> missionMap = new LinkedHashMap<>();
        List<Mission> missionList = new ArrayList<>();
        for (Act act : this.acts) {
            for (Mission mission : act.missions) {
                if (missionMap.put(mission.id, mission) != null) {
                    throw new IllegalArgumentException("Duplicate mission id: " + mission.id);
                }
                missionList.add(mission);
            }
        }
        this.missions = Collections.unmodifiableMap(missionMap);
        this.orderedMissions = List.copyOf(missionList);
    }

    Mission mission(String missionId) {
        return missions.get(missionId);
    }

    Cutscene scene(String sceneId) {
        return scenes.get(sceneId);
    }

    int missionIndex(String missionId) {
        for (int i = 0; i < orderedMissions.size(); i++) {
            if (orderedMissions.get(i).id.equals(missionId)) return i;
        }
        return -1;
    }

    Mission firstMission() {
        return orderedMissions.isEmpty() ? null : orderedMissions.getFirst();
    }

    Mission nextMission(String missionId) {
        int idx = missionIndex(missionId);
        if (idx < 0 || idx + 1 >= orderedMissions.size()) return null;
        return orderedMissions.get(idx + 1);
    }

    Act actForMission(String missionId) {
        for (Act act : acts) {
            for (Mission mission : act.missions) {
                if (mission.id.equals(missionId)) return act;
            }
        }
        return null;
    }

    int actIndexForMission(String missionId) {
        for (int i = 0; i < acts.size(); i++) {
            for (Mission mission : acts.get(i).missions) {
                if (mission.id.equals(missionId)) return i;
            }
        }
        return -1;
    }

    ValidationReport validate() {
        List<String> errors = new ArrayList<>();
        if (acts.size() != 12) errors.add("Campaign must contain exactly 12 acts");
        if (orderedMissions.size() != 40) errors.add("Campaign must contain exactly 40 missions");

        EnumSet<BirdGame3.MapType> usedMaps = EnumSet.noneOf(BirdGame3.MapType.class);
        EnumSet<BirdGame3.BirdType> playableBirds = EnumSet.noneOf(BirdGame3.BirdType.class);
        Set<String> ids = new HashSet<>();
        int finalBossCount = 0;
        for (Mission mission : orderedMissions) {
            if (!ids.add(mission.id)) errors.add("Duplicate mission: " + mission.id);
            usedMaps.add(mission.map);
            playableBirds.addAll(mission.playable.resolvedBirds());
            Cutscene pre = scenes.get(mission.preSceneId);
            Cutscene post = scenes.get(mission.postSceneId);
            if (pre == null) errors.add("Missing pre-scene " + mission.preSceneId + " for " + mission.id);
            if (post == null) errors.add("Missing post-scene " + mission.postSceneId + " for " + mission.id);
            if (pre != null && mission.playable.kind != PlayableKind.FULL_ROSTER) {
                if (!pre.handoffBirds.containsAll(mission.playable.birds)) {
                    errors.add("Pre-scene " + pre.id + " does not hand off every playable bird for " + mission.id);
                }
            }
            if (mission.finalBoss) finalBossCount++;
            boolean trueFinal = "the_null_rock".equals(mission.id);
            if (!trueFinal && (mission.title.contains("The Null Rock")
                    || mission.briefing.contains("The Null Rock"))) {
                errors.add("The Null Rock name appears before the true final mission: " + mission.id);
            }
        }
        if (!usedMaps.equals(EnumSet.allOf(BirdGame3.MapType.class))) {
            errors.add("Campaign must use all maps; used " + usedMaps.size());
        }
        if (!playableBirds.equals(EnumSet.allOf(BirdGame3.BirdType.class))) {
            errors.add("Campaign must make all birds playable");
        }
        if (finalBossCount != 2) {
            errors.add("Campaign must contain the two authored final boss missions");
        }
        Mission nullRoc = missions.get("null_roc");
        Mission nullRock = missions.get("the_null_rock");
        if (nullRoc == null || nullRoc.playable.kind != PlayableKind.FULL_ROSTER) {
            errors.add("Null Roc must allow the full roster");
        }
        if (nullRock == null || nullRock.playable.kind != PlayableKind.FULL_ROSTER) {
            errors.add("The Null Rock must allow the full roster");
        }

        int deathScenes = 0;
        Map<BirdGame3.BirdType, Set<String>> speakingScenes = new HashMap<>();
        Map<String, Integer> speakerCounts = new HashMap<>();
        int totalLines = 0;
        for (Cutscene scene : scenes.values()) {
            if (scene.deathScene) deathScenes++;
            for (DialogueLine line : scene.lines) {
                totalLines++;
                speakerCounts.merge(line.speaker, 1, Integer::sum);
                if (line.bird != null) {
                    speakingScenes.computeIfAbsent(line.bird, ignored -> new HashSet<>()).add(scene.id);
                }
            }
        }
        if (deathScenes != 2) errors.add("Campaign must contain exactly two permanent-death scenes");
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int count = speakingScenes.getOrDefault(type, Set.of()).size();
            if (count < 3) errors.add(type.name + " must speak in at least three scenes");
        }
        int maxSpeakerLines = speakerCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (totalLines > 0 && maxSpeakerLines > Math.ceil(totalLines * 0.15)) {
            errors.add("One speaker owns more than 15% of campaign dialogue");
        }
        if (totalLines < 700 || totalLines > 900) {
            errors.add("Campaign dialogue must contain 700-900 authored lines; found " + totalLines);
        }
        return new ValidationReport(errors, totalLines, usedMaps, playableBirds);
    }

    record ValidationReport(List<String> errors, int dialogueLineCount,
                            Set<BirdGame3.MapType> maps,
                            Set<BirdGame3.BirdType> playableBirds) {
        ValidationReport {
            errors = List.copyOf(errors);
            maps = Set.copyOf(maps);
            playableBirds = Set.copyOf(playableBirds);
        }

        boolean valid() {
            return errors.isEmpty();
        }

        void throwIfInvalid() {
            if (!valid()) {
                throw new IllegalStateException("Invalid story campaign:\n - " + String.join("\n - ", errors));
            }
        }
    }

    private static String requireId(String value, String label) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isEmpty() || !resolved.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid " + label + " id: " + value);
        }
        return resolved;
    }

    private static String requireText(String value, String label) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("Missing " + label);
        }
        return resolved;
    }
}
