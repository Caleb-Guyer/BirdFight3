package com.example.birdgame3;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class BirdGame3SaveProfilesUi {
    @FunctionalInterface
    interface TitleBannerBuilder {
        StackPane build(String text, double width, double fontSize);
    }

    @FunctionalInterface
    interface EscapeBinder {
        void bind(Scene scene, Button backButton);
    }

    @FunctionalInterface
    interface StageSceneSetter {
        void accept(Stage stage, Scene scene);
    }

    private static final DateTimeFormatter PROFILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a", Locale.US);
    private static final DateTimeFormatter EXPORT_FILENAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final GameSaveRepository saveRepository;
    private final UIFactory uiFactory;
    private final int width;
    private final int height;
    private final Runnable playMenuMusic;
    private final Consumer<Stage> showMenu;
    private final TitleBannerBuilder titleBannerBuilder;
    private final Consumer<Label> noEllipsis;
    private final EscapeBinder escapeBinder;
    private final Consumer<Scene> keyboardNavigationSetup;
    private final Consumer<Scene> consoleHighlighter;
    private final StageSceneSetter sceneSetter;
    private final Runnable flushAchievementsNow;
    private final Runnable loadAchievements;

    BirdGame3SaveProfilesUi(GameSaveRepository saveRepository,
                            UIFactory uiFactory,
                            int width,
                            int height,
                            Runnable playMenuMusic,
                            Consumer<Stage> showMenu,
                            TitleBannerBuilder titleBannerBuilder,
                            Consumer<Label> noEllipsis,
                            EscapeBinder escapeBinder,
                            Consumer<Scene> keyboardNavigationSetup,
                            Consumer<Scene> consoleHighlighter,
                            StageSceneSetter sceneSetter,
                            Runnable flushAchievementsNow,
                            Runnable loadAchievements) {
        this.saveRepository = saveRepository;
        this.uiFactory = uiFactory;
        this.width = width;
        this.height = height;
        this.playMenuMusic = playMenuMusic;
        this.showMenu = showMenu;
        this.titleBannerBuilder = titleBannerBuilder;
        this.noEllipsis = noEllipsis;
        this.escapeBinder = escapeBinder;
        this.keyboardNavigationSetup = keyboardNavigationSetup;
        this.consoleHighlighter = consoleHighlighter;
        this.sceneSetter = sceneSetter;
        this.flushAchievementsNow = flushAchievementsNow;
        this.loadAchievements = loadAchievements;
    }

    void showProfileManager(Stage stage) {
        playMenuMusic.run();
        List<GameSaveRepository.SaveProfile> profiles = saveRepository.profiles();
        String activeProfileId = saveRepository.activeProfile().id();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle(MenuTheme.pageBackground());

        Button back = uiFactory.action("BACK TO HUB", 320, 84, 30, "#D32F2F", 22, () -> showMenu.accept(stage));
        Button create = uiFactory.action("NEW PROFILE", 290, 84, 28, "#2E7D32", 22, () -> createProfilePrompt(stage));
        Button backupNow = uiFactory.action("BACK UP NOW", 260, 84, 26, "#00897B", 20, () -> createManualBackup(stage));
        Button backups = uiFactory.action("BACKUPS", 220, 84, 26, "#5E35B1", 20, () -> showBackupManager(stage));
        Button export = uiFactory.action("EXPORT SAVE", 250, 84, 26, "#1565C0", 20, () -> exportSaveData(stage));
        Button importSave = uiFactory.action("IMPORT SAVE", 250, 84, 26, "#EF6C00", 20, () -> importSaveData(stage));
        StackPane title = titleBannerBuilder.build("SAVE PROFILES", 520, 34);

        Label subtitle = new Label("Settings and controls stay global. Coins, unlocks, story progress, and match history are stored per profile.");
        subtitle.setFont(Font.font("Consolas", 20));
        subtitle.setTextFill(Color.web("#CFD8DC"));
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(920);
        noEllipsis.accept(subtitle);

        VBox titleBox = new VBox(10, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FlowPane tools = new FlowPane(12, 12, create, backupNow, backups, export, importSave);
        tools.setAlignment(Pos.CENTER_RIGHT);
        tools.setPrefWrapLength(1040);
        tools.setPadding(new Insets(12, 14, 12, 14));
        tools.setStyle(MenuTheme.insetPanelStyle("#90CAF9", 22));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(18, back, titleBox, spacer, tools);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 16, 12, 16));
        topBar.setStyle(MenuTheme.topStripStyle());

        VBox cards = new VBox(18);
        cards.setAlignment(Pos.TOP_CENTER);
        cards.setPadding(new Insets(8, 0, 8, 0));
        for (GameSaveRepository.SaveProfile profile : profiles) {
            cards.getChildren().add(buildProfileCard(stage, profile, profile.id().equals(activeProfileId), profiles.size() > 1));
        }

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Label note = new Label("Automatic backups are created before resets, deletes, imports, and save recovery. Switching or resetting the active profile reloads the hub immediately.");
        note.setFont(Font.font("Consolas", 18));
        note.setTextFill(Color.web("#80DEEA"));
        note.setWrapText(true);
        note.setPadding(new Insets(16, 18, 16, 18));
        note.setMaxWidth(Double.MAX_VALUE);
        note.setStyle(MenuTheme.insetPanelStyle("#80DEEA", 20));
        noEllipsis.accept(note);

        root.setTop(topBar);
        root.setCenter(scroll);
        root.setBottom(note);
        BorderPane.setAlignment(note, Pos.CENTER_LEFT);
        BorderPane.setMargin(note, new Insets(18, 8, 0, 8));

        Scene scene = new Scene(root, width, height);
        escapeBinder.bind(scene, back);
        keyboardNavigationSetup.accept(scene);
        consoleHighlighter.accept(scene);
        sceneSetter.accept(stage, scene);
        create.requestFocus();
    }

    private VBox buildProfileCard(Stage stage, GameSaveRepository.SaveProfile profile, boolean active, boolean canDelete) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMaxWidth(1540);
        card.setStyle(MenuTheme.panelStyle(active ? "#FFE082" : "#607D8B", 24));

        Label name = new Label(profile.name());
        name.setFont(Font.font("Arial Black", 30));
        name.setTextFill(active ? Color.web("#FFF59D") : Color.WHITE);
        noEllipsis.accept(name);

        Label state = new Label(active ? "ACTIVE PROFILE" : "AVAILABLE");
        state.setFont(Font.font("Consolas", 18));
        state.setTextFill(active ? Color.web("#80DEEA") : Color.web("#B0BEC5"));
        noEllipsis.accept(state);

        Label details = new Label("Created: " + formatProfileTimestamp(profile.createdAtMillis())
                + "   |   Last updated: " + formatProfileTimestamp(profile.updatedAtMillis()));
        details.setFont(Font.font("Consolas", 17));
        details.setTextFill(Color.web("#CFD8DC"));
        details.setWrapText(true);
        details.setMaxWidth(1460);
        noEllipsis.accept(details);

        Button activate = uiFactory.action(active ? "ACTIVE" : "SWITCH", 180, 64, 22,
                active ? "#546E7A" : "#1565C0", 18, () -> switchToProfile(stage, profile.id()));
        activate.setDisable(active);

        Button rename = uiFactory.action("RENAME", 180, 64, 22, "#6A1B9A", 18,
                () -> renameProfilePrompt(stage, profile.id()));
        Button reset = uiFactory.action("RESET", 180, 64, 22, "#EF6C00", 18,
                () -> confirmResetProfile(stage, profile.id()));
        Button delete = uiFactory.action("DELETE", 180, 64, 22, "#B71C1C", 18,
                () -> confirmDeleteProfile(stage, profile.id()));
        delete.setDisable(!canDelete);

        FlowPane actions = new FlowPane(12, 12, activate, rename, reset, delete);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(12, 14, 12, 14));
        actions.setStyle(MenuTheme.insetPanelStyle(active ? "#FFE082" : "#607D8B", 18));

        card.getChildren().addAll(name, state, details, actions);
        return card;
    }

    private void createProfilePrompt(Stage stage) {
        TextInputDialog dialog = new TextInputDialog("Profile " + (saveRepository.profiles().size() + 1));
        dialog.setTitle("Create Profile");
        dialog.setHeaderText("Create and switch to a new save profile.");
        dialog.setContentText("Profile name:");
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(name -> {
            flushAchievementsNow.run();
            saveRepository.createProfile(name, true);
            loadAchievements.run();
            showMenu.accept(stage);
        });
    }

    private void renameProfilePrompt(Stage stage, String profileId) {
        GameSaveRepository.SaveProfile profile = findProfile(profileId);
        if (profile == null) {
            showProfileManager(stage);
            return;
        }

        TextInputDialog dialog = new TextInputDialog(profile.name());
        dialog.setTitle("Rename Profile");
        dialog.setHeaderText("Rename save profile.");
        dialog.setContentText("Profile name:");
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(name -> {
            saveRepository.renameProfile(profileId, name);
            showProfileManager(stage);
        });
    }

    private void switchToProfile(Stage stage, String profileId) {
        if (profileId == null || profileId.isBlank()) {
            showProfileManager(stage);
            return;
        }
        if (profileId.equals(saveRepository.activeProfile().id())) {
            showMenu.accept(stage);
            return;
        }

        flushAchievementsNow.run();
        saveRepository.setActiveProfile(profileId);
        loadAchievements.run();
        showMenu.accept(stage);
    }

    private void confirmResetProfile(Stage stage, String profileId) {
        GameSaveRepository.SaveProfile profile = findProfile(profileId);
        if (profile == null) {
            showProfileManager(stage);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Reset coins, unlocks, story progress, and match history for this profile?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Reset Profile");
        alert.setHeaderText("Reset " + profile.name() + "?");
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(choice -> {
            if (choice != ButtonType.YES) {
                return;
            }
            flushAchievementsNow.run();
            boolean active = profileId.equals(saveRepository.activeProfile().id());
            saveRepository.clearProfile(profileId);
            if (active) {
                loadAchievements.run();
                showMenu.accept(stage);
            } else {
                showProfileManager(stage);
            }
        });
    }

    private void confirmDeleteProfile(Stage stage, String profileId) {
        GameSaveRepository.SaveProfile profile = findProfile(profileId);
        if (profile == null) {
            showProfileManager(stage);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this profile and all of its profile-specific progress?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Delete Profile");
        alert.setHeaderText("Delete " + profile.name() + "?");
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(choice -> {
            if (choice != ButtonType.YES) {
                return;
            }
            flushAchievementsNow.run();
            boolean active = profileId.equals(saveRepository.activeProfile().id());
            saveRepository.deleteProfile(profileId);
            if (active) {
                loadAchievements.run();
                showMenu.accept(stage);
            } else {
                showProfileManager(stage);
            }
        });
    }

    private void createManualBackup(Stage stage) {
        flushAchievementsNow.run();
        GameSaveRepository.SaveBackup backup = saveRepository.createBackup("Manual backup from profile manager");
        showInfoAlert(stage, "Backup Created", "Backup saved.",
                "Created " + formatProfileTimestamp(backup.createdAtMillis()) + " for "
                        + (backup.activeProfileName().isBlank() ? "the active save." : backup.activeProfileName() + '.'));
        showBackupManager(stage);
    }

    private void showBackupManager(Stage stage) {
        playMenuMusic.run();
        List<GameSaveRepository.SaveBackup> backups = saveRepository.backups();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle(MenuTheme.pageBackground());

        Button back = uiFactory.action("BACK TO PROFILES", 360, 84, 28, "#D32F2F", 22, () -> showProfileManager(stage));
        Button backupNow = uiFactory.action("BACK UP NOW", 260, 84, 26, "#00897B", 20, () -> createManualBackup(stage));
        StackPane title = titleBannerBuilder.build("SAVE BACKUPS", 520, 34);

        Label subtitle = new Label("Use restore before risky changes or after a bad update. Imports also create a restore point automatically.");
        subtitle.setFont(Font.font("Consolas", 20));
        subtitle.setTextFill(Color.web("#CFD8DC"));
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(920);
        noEllipsis.accept(subtitle);

        VBox titleBox = new VBox(10, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(18, back, titleBox, spacer, backupNow);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 16, 12, 16));
        topBar.setStyle(MenuTheme.topStripStyle());

        VBox cards = new VBox(18);
        cards.setAlignment(Pos.TOP_CENTER);
        cards.setPadding(new Insets(8, 0, 8, 0));
        if (backups.isEmpty()) {
            Label empty = new Label("No backups yet. Create one now or trigger one automatically by importing, resetting, or deleting a profile.");
            empty.setFont(Font.font("Consolas", 22));
            empty.setTextFill(Color.web("#CFD8DC"));
            empty.setWrapText(true);
            empty.setMaxWidth(1320);
            empty.setTextAlignment(TextAlignment.CENTER);
            noEllipsis.accept(empty);
            VBox emptyBox = new VBox(empty);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            emptyBox.setStyle(MenuTheme.panelStyle("#607D8B", 24));
            cards.getChildren().add(emptyBox);
        } else {
            for (GameSaveRepository.SaveBackup backup : backups) {
                cards.getChildren().add(buildBackupCard(stage, backup));
            }
        }

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Label note = new Label("Restoring a backup first snapshots your current save, so you can undo the restore if needed.");
        note.setFont(Font.font("Consolas", 18));
        note.setTextFill(Color.web("#80DEEA"));
        note.setWrapText(true);
        note.setPadding(new Insets(16, 18, 16, 18));
        note.setMaxWidth(Double.MAX_VALUE);
        note.setStyle(MenuTheme.insetPanelStyle("#80DEEA", 20));
        noEllipsis.accept(note);

        root.setTop(topBar);
        root.setCenter(scroll);
        root.setBottom(note);
        BorderPane.setAlignment(note, Pos.CENTER_LEFT);
        BorderPane.setMargin(note, new Insets(18, 8, 0, 8));

        Scene scene = new Scene(root, width, height);
        escapeBinder.bind(scene, back);
        keyboardNavigationSetup.accept(scene);
        consoleHighlighter.accept(scene);
        sceneSetter.accept(stage, scene);
        backupNow.requestFocus();
    }

    private VBox buildBackupCard(Stage stage, GameSaveRepository.SaveBackup backup) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMaxWidth(1540);
        card.setStyle(MenuTheme.panelStyle("#78909C", 24));

        Label name = new Label(backup.reason());
        name.setFont(Font.font("Arial Black", 28));
        name.setTextFill(Color.web("#FFF59D"));
        noEllipsis.accept(name);

        Label state = new Label("Created: " + formatProfileTimestamp(backup.createdAtMillis()));
        state.setFont(Font.font("Consolas", 18));
        state.setTextFill(Color.web("#80DEEA"));
        noEllipsis.accept(state);

        String profileLabel = backup.activeProfileName().isBlank() ? "Unknown profile" : backup.activeProfileName();
        Label details = new Label("Snapshot of: " + profileLabel
                + (backup.sourceProfileId().isBlank() ? "" : "   |   Source profile id: " + backup.sourceProfileId()));
        details.setFont(Font.font("Consolas", 17));
        details.setTextFill(Color.web("#CFD8DC"));
        details.setWrapText(true);
        details.setMaxWidth(1460);
        noEllipsis.accept(details);

        Button restore = uiFactory.action("RESTORE", 210, 64, 22, "#2E7D32", 18,
                () -> confirmRestoreBackup(stage, backup.id()));
        Button delete = uiFactory.action("DELETE", 210, 64, 22, "#B71C1C", 18,
                () -> confirmDeleteBackup(stage, backup.id()));
        FlowPane actions = new FlowPane(12, 12, restore, delete);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(12, 14, 12, 14));
        actions.setStyle(MenuTheme.insetPanelStyle("#78909C", 18));

        card.getChildren().addAll(name, state, details, actions);
        return card;
    }

    private void confirmRestoreBackup(Stage stage, String backupId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Restore this backup and replace the current global settings and all save profiles?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Restore Backup");
        alert.setHeaderText("Restore save backup?");
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(choice -> {
            if (choice != ButtonType.YES) {
                return;
            }
            flushAchievementsNow.run();
            try {
                saveRepository.restoreBackup(backupId);
                loadAchievements.run();
                showInfoAlert(stage, "Backup Restored", "Save data restored.",
                        "The selected backup is now active. Your previous live save was also backed up first.");
                showMenu.accept(stage);
            } catch (RuntimeException e) {
                showErrorAlert(stage, "Restore Failed", "Could not restore backup.", e.getMessage());
                showBackupManager(stage);
            }
        });
    }

    private void confirmDeleteBackup(Stage stage, String backupId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this backup permanently?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Delete Backup");
        alert.setHeaderText("Delete save backup?");
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(choice -> {
            if (choice != ButtonType.YES) {
                return;
            }
            try {
                saveRepository.deleteBackup(backupId);
                showBackupManager(stage);
            } catch (RuntimeException e) {
                showErrorAlert(stage, "Delete Failed", "Could not delete backup.", e.getMessage());
                showBackupManager(stage);
            }
        });
    }

    private void exportSaveData(Stage stage) {
        flushAchievementsNow.run();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Bird Fight 3 Save");
        chooser.setInitialFileName(defaultSaveExportFilename());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bird Fight 3 Save", "*.birdsave"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            saveRepository.exportTo(file.toPath());
            showInfoAlert(stage, "Save Exported", "Export complete.", "Wrote save data to " + file.getAbsolutePath());
        } catch (RuntimeException e) {
            showErrorAlert(stage, "Export Failed", "Could not export save.", e.getMessage());
        }
    }

    private void importSaveData(Stage stage) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Importing replaces the current global settings and all save profiles. A backup of your current save will be created first.",
                ButtonType.YES,
                ButtonType.NO);
        confirm.setTitle("Import Save");
        confirm.setHeaderText("Import save data?");
        confirm.initOwner(stage);
        confirm.showAndWait().ifPresent(choice -> {
            if (choice != ButtonType.YES) {
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import Bird Fight 3 Save");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bird Fight 3 Save", "*.birdsave"));
            File file = chooser.showOpenDialog(stage);
            if (file == null) {
                return;
            }
            flushAchievementsNow.run();
            try {
                saveRepository.importFrom(file.toPath());
                loadAchievements.run();
                showInfoAlert(stage, "Save Imported", "Import complete.",
                        "Loaded save data from " + file.getAbsolutePath() + ". Your previous save was backed up first.");
                showMenu.accept(stage);
            } catch (RuntimeException e) {
                showErrorAlert(stage, "Import Failed", "Could not import save.", e.getMessage());
                showProfileManager(stage);
            }
        });
    }

    private GameSaveRepository.SaveProfile findProfile(String profileId) {
        return saveRepository.profiles().stream()
                .filter(candidate -> candidate.id().equals(profileId))
                .findFirst()
                .orElse(null);
    }

    private void showInfoAlert(Stage stage, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showErrorAlert(Stage stage, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content == null || content.isBlank() ? "Unknown error." : content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private String defaultSaveExportFilename() {
        return "birdfight3-save-" + EXPORT_FILENAME_FORMAT.format(LocalDateTime.now()) + ".birdsave";
    }

    private String formatProfileTimestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return "Never";
        }
        return PROFILE_TIMESTAMP_FORMAT.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()));
    }
}
