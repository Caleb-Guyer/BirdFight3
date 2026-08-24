package com.example.birdgame3;

import java.util.Objects;

/** Headless state machine for the local-versus front-end journey. */
final class FrontEndMatchFlow {
    enum Screen {
        TITLE,
        HUB,
        RULES,
        FIGHTERS,
        STAGES,
        LOADING,
        BATTLE,
        RESULTS
    }

    private Screen screen = Screen.TITLE;
    private VersusRulesPreset rulesPreset = VersusRulesPreset.STANDARD;
    private VersusRules rules = VersusRules.standard();

    Screen screen() {
        return screen;
    }

    VersusRulesPreset rulesPreset() {
        return rulesPreset;
    }

    VersusRules rules() {
        return rules;
    }

    void restoreRulesPreset(String preference) {
        selectRulesPreset(VersusRulesPreset.fromPreference(preference));
    }

    void restoreRules(String preference, VersusRules customRules) {
        VersusRulesPreset preset = VersusRulesPreset.fromPreference(preference);
        if (preset == VersusRulesPreset.CUSTOM) {
            selectCustomRules(customRules);
        } else {
            selectRulesPreset(preset);
        }
    }

    void selectRulesPreset(VersusRulesPreset preset) {
        rulesPreset = Objects.requireNonNullElse(preset, VersusRulesPreset.STANDARD);
        if (rulesPreset != VersusRulesPreset.CUSTOM) {
            rules = rulesPreset.rules;
        }
    }

    void selectCustomRules(VersusRules customRules) {
        rulesPreset = VersusRulesPreset.CUSTOM;
        rules = Objects.requireNonNullElse(customRules, VersusRules.standard().withName("CUSTOM RULES"));
    }

    void showTitle() {
        screen = Screen.TITLE;
    }

    void showHub() {
        screen = Screen.HUB;
    }

    void beginVersus() {
        screen = Screen.RULES;
    }

    void confirmRules() {
        require(Screen.RULES);
        screen = Screen.FIGHTERS;
    }

    void showFighters() {
        screen = Screen.FIGHTERS;
    }

    void confirmFighters(boolean allReady) {
        require(Screen.FIGHTERS);
        if (!allReady) {
            throw new IllegalStateException("All active fighter slots must be ready");
        }
        screen = Screen.STAGES;
    }

    void showStages() {
        screen = Screen.STAGES;
    }

    void showLoading() {
        require(Screen.STAGES);
        screen = Screen.LOADING;
    }

    void beginBattle() {
        require(Screen.LOADING, Screen.RESULTS);
        screen = Screen.BATTLE;
    }

    void showResults() {
        screen = Screen.RESULTS;
    }

    Screen back() {
        screen = switch (screen) {
            case RULES -> Screen.HUB;
            case FIGHTERS -> Screen.RULES;
            case STAGES -> Screen.FIGHTERS;
            case RESULTS -> Screen.HUB;
            case TITLE, HUB -> Screen.TITLE;
            case LOADING -> Screen.STAGES;
            case BATTLE -> screen;
        };
        return screen;
    }

    String progressLabel() {
        return switch (screen) {
            case RULES -> "1  RULES   ›   2  FIGHTERS   ›   3  STAGE";
            case FIGHTERS -> "1  RULES   ›   2  FIGHTERS   ›   3  STAGE";
            case STAGES, LOADING -> "1  RULES   ›   2  FIGHTERS   ›   3  STAGE";
            default -> "";
        };
    }

    private void require(Screen... accepted) {
        for (Screen candidate : accepted) {
            if (screen == candidate) {
                return;
            }
        }
        throw new IllegalStateException("Front-end transition is not valid from " + screen);
    }
}
