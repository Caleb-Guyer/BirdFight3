package com.example.birdgame3;

import java.util.List;
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

    record ScreenGuide(String breadcrumb, String primaryAction, String backAction,
                       List<UiInputPrompts.Prompt> prompts) {
        ScreenGuide {
            breadcrumb = breadcrumb == null ? "" : breadcrumb;
            primaryAction = primaryAction == null ? "" : primaryAction;
            backAction = backAction == null ? "" : backAction;
            prompts = prompts == null ? List.of() : List.copyOf(prompts);
        }
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
        screen = backTarget();
        return screen;
    }

    Screen backTarget() {
        return switch (screen) {
            case RULES -> Screen.HUB;
            case FIGHTERS -> Screen.RULES;
            case STAGES -> Screen.FIGHTERS;
            case RESULTS -> Screen.HUB;
            case TITLE, HUB -> Screen.TITLE;
            case LOADING -> Screen.STAGES;
            case BATTLE -> screen;
        };
    }

    boolean canGoBack() {
        return backTarget() != screen;
    }

    ScreenGuide guide() {
        return guideFor(screen);
    }

    static ScreenGuide guideFor(Screen screen) {
        Objects.requireNonNull(screen, "screen");
        return switch (screen) {
            case TITLE -> guide("BIRD FIGHT 3", "START", "EXIT",
                    prompt(UiInputPrompts.Command.START, "START"),
                    prompt(UiInputPrompts.Command.BACK, "EXIT"),
                    prompt(UiInputPrompts.Command.FULLSCREEN, "FULLSCREEN"));
            case HUB -> guide("BIRD FIGHT 3", "SELECT MODE", "TITLE",
                    prompt(UiInputPrompts.Command.MOVE, "CHOOSE"),
                    prompt(UiInputPrompts.Command.SELECT, "SELECT"),
                    prompt(UiInputPrompts.Command.BACK, "TITLE"));
            case RULES -> guide("1  RULES   ›   2  FIGHTERS   ›   3  STAGE",
                    "CHOOSE FIGHTERS", "FIGHT MENU",
                    prompt(UiInputPrompts.Command.MOVE, "CHOOSE"),
                    prompt(UiInputPrompts.Command.SELECT, "SELECT"),
                    prompt(UiInputPrompts.Command.BACK, "BACK"));
            case FIGHTERS -> guide("1  RULES   ›   2  FIGHTERS   ›   3  STAGE",
                    "CHOOSE STAGE", "RULESETS",
                    prompt(UiInputPrompts.Command.POINTER, "MOVE CURSOR"),
                    prompt(UiInputPrompts.Command.POINTER_SELECT, "PICK / RELEASE"),
                    prompt(UiInputPrompts.Command.READY, "READY"),
                    prompt(UiInputPrompts.Command.BACK, "RULESETS"));
            case STAGES -> guide("1  RULES   ›   2  FIGHTERS   ›   3  STAGE",
                    "CONFIRM BATTLE", "FIGHTERS",
                    prompt(UiInputPrompts.Command.PREVIEW, "PREVIEW"),
                    prompt(UiInputPrompts.Command.SELECT, "SELECT"),
                    prompt(UiInputPrompts.Command.BACK, "FIGHTERS"));
            case LOADING -> guide("BATTLE READY", "START BATTLE", "STAGES",
                    prompt(UiInputPrompts.Command.SELECT, "START BATTLE"),
                    prompt(UiInputPrompts.Command.BACK, "STAGES"));
            case BATTLE -> guide("BATTLE", "FIGHT", "",
                    prompt(UiInputPrompts.Command.PAUSE, "PAUSE"));
            case RESULTS -> guide("RESULTS", "REMATCH", "HUB",
                    prompt(UiInputPrompts.Command.MOVE, "CHOOSE ACTION"),
                    prompt(UiInputPrompts.Command.SELECT, "CONFIRM"),
                    prompt(UiInputPrompts.Command.BACK, "HUB"));
        };
    }

    private static ScreenGuide guide(String breadcrumb, String primaryAction, String backAction,
                                     UiInputPrompts.Prompt... prompts) {
        return new ScreenGuide(breadcrumb, primaryAction, backAction,
                prompts == null ? List.of() : List.of(prompts));
    }

    private static UiInputPrompts.Prompt prompt(UiInputPrompts.Command command, String verb) {
        return UiInputPrompts.prompt(command, verb);
    }

    String progressLabel() {
        return switch (screen) {
            case RULES, FIGHTERS, STAGES -> guide().breadcrumb();
            case LOADING -> "1  RULES   ›   2  FIGHTERS   ›   3  STAGE";
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
