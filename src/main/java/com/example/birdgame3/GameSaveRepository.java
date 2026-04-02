package com.example.birdgame3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class GameSaveRepository {
    private static final String KEY_ACTIVE_PROFILE_ID = "save_active_profile_id";
    private static final String KEY_MIGRATED = "save_profiles_migrated";
    private static final String KEY_LEGACY_RECOVERED = "save_legacy_recovered";
    private static final String KEY_SCHEMA_VERSION = "save_schema_version";
    private static final String NODE_PROFILES = "save_profiles";
    private static final String NODE_SAVE = "save";
    private static final String NODE_BACKUPS = "save_backups";
    private static final String NODE_SNAPSHOT = "snapshot";
    private static final String KEY_NAME = "name";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_REASON = "reason";
    private static final String KEY_ACTIVE_PROFILE_NAME = "active_profile_name";
    private static final String KEY_SOURCE_PROFILE_ID = "source_profile_id";
    private static final String DEFAULT_PROFILE_NAME = "Profile 1";
    private static final int CURRENT_SCHEMA_VERSION = 2;
    private static final int EXPORT_FORMAT_VERSION = 1;
    private static final int MAX_BACKUPS = 12;
    private static final String EXPORT_VERSION_KEY = "birdfight3.export.version";
    private static final String EXPORT_VALUE_PREFIX = "value|";

    private static final List<String> GLOBAL_KEY_PREFIXES = List.of(
            "bind_p",
            "setting_"
    );

    private static final List<String> GLOBAL_KEYS = List.of(
            "lan_last_host"
    );

    record SaveProfile(String id, String name, long createdAtMillis, long updatedAtMillis) {
        SaveProfile {
            id = Objects.requireNonNull(id, "id");
            name = Objects.requireNonNull(name, "name");
            createdAtMillis = Math.max(0L, createdAtMillis);
            updatedAtMillis = Math.max(createdAtMillis, updatedAtMillis);
        }
    }

    record SaveBackup(String id, String reason, long createdAtMillis, String activeProfileId,
                      String activeProfileName, String sourceProfileId) {
        SaveBackup {
            id = Objects.requireNonNull(id, "id");
            reason = defaultString(reason);
            createdAtMillis = Math.max(0L, createdAtMillis);
            activeProfileId = defaultString(activeProfileId);
            activeProfileName = defaultString(activeProfileName);
            sourceProfileId = defaultString(sourceProfileId);
        }

        private static String defaultString(String value) {
            return value == null ? "" : value;
        }
    }

    private final Preferences root;

    GameSaveRepository(Class<?> prefsOwner) {
        this(Preferences.userNodeForPackage(prefsOwner));
    }

    GameSaveRepository(Preferences root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    Preferences globalPrefs() {
        ensureInitialized();
        return root;
    }

    Preferences activeProfilePrefs() {
        return profilePrefs(activeProfile().id());
    }

    Preferences profilePrefs(String profileId) {
        ensureInitialized();
        return existingProfileNode(profileId).node(NODE_SAVE);
    }

    SaveProfile activeProfile() {
        ensureInitialized();
        String activeId = root.get(KEY_ACTIVE_PROFILE_ID, "");
        SaveProfile profile = findProfileRaw(activeId);
        if (profile != null) {
            return profile;
        }
        List<SaveProfile> profiles = listProfilesRaw();
        SaveProfile fallback = profiles.getFirst();
        root.put(KEY_ACTIVE_PROFILE_ID, fallback.id());
        return fallback;
    }

    List<SaveProfile> profiles() {
        ensureInitialized();
        return listProfilesRaw();
    }

    List<SaveBackup> backups() {
        ensureInitialized();
        return listBackupsRaw();
    }

    SaveBackup createBackup(String reason) {
        ensureInitialized();
        SaveProfile active = activeProfile();
        return createBackupSnapshot(reason, active == null ? "" : active.id());
    }

    SaveProfile createProfile(String requestedName, boolean activate) {
        ensureInitialized();
        markLegacyRecoveryHandled();
        SaveProfile profile = createProfileInternal(requestedName, activate, listProfilesRaw().size() + 1);
        flushRoot();
        return profile;
    }

    SaveProfile renameProfile(String profileId, String requestedName) {
        ensureInitialized();
        Preferences profileNode = existingProfileNode(profileId);
        String name = normalizedProfileName(requestedName, listProfilesRaw().size());
        profileNode.put(KEY_NAME, name);
        profileNode.putLong(KEY_UPDATED_AT, System.currentTimeMillis());
        flushRoot();
        return readProfile(profileId);
    }

    void setActiveProfile(String profileId) {
        ensureInitialized();
        existingProfileNode(profileId);
        markLegacyRecoveryHandled();
        root.put(KEY_ACTIVE_PROFILE_ID, profileId);
        flushRoot();
    }

    void clearProfile(String profileId) {
        ensureInitialized();
        Preferences profileNode = existingProfileNode(profileId);
        SaveProfile profile = readProfile(profileId);
        markLegacyRecoveryHandled();
        createBackupSnapshot("Before resetting " + (profile == null ? DEFAULT_PROFILE_NAME : profile.name()), profileId);
        try {
            if (profileNode.nodeExists(NODE_SAVE)) {
                profileNode.node(NODE_SAVE).removeNode();
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to clear save profile " + profileId + '.', e);
        }
        profileNode.node(NODE_SAVE);
        profileNode.putLong(KEY_UPDATED_AT, System.currentTimeMillis());
        flushRoot();
    }

    SaveProfile deleteProfile(String profileId) {
        ensureInitialized();
        List<SaveProfile> profiles = listProfilesRaw();
        if (profiles.size() <= 1) {
            throw new IllegalStateException("At least one save profile must remain.");
        }

        Preferences profileNode = existingProfileNode(profileId);
        SaveProfile profile = readProfile(profileId);
        boolean deletedActive = activeProfile().id().equals(profileId);
        markLegacyRecoveryHandled();
        createBackupSnapshot("Before deleting " + (profile == null ? DEFAULT_PROFILE_NAME : profile.name()), profileId);
        try {
            profileNode.removeNode();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to delete save profile " + profileId + '.', e);
        }

        if (deletedActive) {
            root.put(KEY_ACTIVE_PROFILE_ID, listProfilesRaw().getFirst().id());
        }
        flushRoot();
        return activeProfile();
    }

    void touchActiveProfile() {
        ensureInitialized();
        Preferences profileNode = existingProfileNode(activeProfile().id());
        profileNode.putLong(KEY_UPDATED_AT, System.currentTimeMillis());
    }

    void deleteBackup(String backupId) {
        ensureInitialized();
        Preferences backupNode = existingBackupNode(backupId);
        try {
            backupNode.removeNode();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to delete save backup " + backupId + '.', e);
        }
        flushRoot();
    }

    void restoreBackup(String backupId) {
        ensureInitialized();
        Preferences backupNode = existingBackupNode(backupId);
        SaveProfile active = activeProfile();
        createBackupSnapshot("Before restoring backup", active == null ? "" : active.id());
        replaceRootWithSnapshot(backupNode.node(NODE_SNAPSHOT));
        ensureInitialized();
        flushRoot();
    }

    void exportTo(Path exportPath) {
        ensureInitialized();
        Objects.requireNonNull(exportPath, "exportPath");
        Properties properties = new Properties();
        properties.setProperty(EXPORT_VERSION_KEY, Integer.toString(EXPORT_FORMAT_VERSION));
        writeExportNode(properties, root, "", true);
        try {
            Path parent = exportPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(exportPath))) {
                properties.store(output, "Bird Fight 3 save export");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export save data.", e);
        }
    }

    void importFrom(Path importPath) {
        ensureInitialized();
        Objects.requireNonNull(importPath, "importPath");
        Properties properties = new Properties();
        try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(importPath))) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to import save data.", e);
        }
        validateImport(properties);
        SaveProfile active = activeProfile();
        createBackupSnapshot("Before importing " + importPath.getFileName(), active == null ? "" : active.id());
        clearNodeContents(root, Set.of(NODE_BACKUPS));
        applyImportedProperties(properties);
        ensureInitialized();
        flushRoot();
    }

    private void ensureInitialized() {
        int schemaVersion = root.getInt(KEY_SCHEMA_VERSION, root.getBoolean(KEY_MIGRATED, false) ? 1 : 0);
        if (schemaVersion < 1) {
            migrateLegacySaveIfNeeded();
            schemaVersion = 1;
            root.putInt(KEY_SCHEMA_VERSION, schemaVersion);
        }
        if (schemaVersion < CURRENT_SCHEMA_VERSION) {
            normalizeProfileMetadata();
            schemaVersion = CURRENT_SCHEMA_VERSION;
            root.putInt(KEY_SCHEMA_VERSION, schemaVersion);
        }
        root.putBoolean(KEY_MIGRATED, true);

        List<SaveProfile> profiles = listProfilesRaw();
        if (profiles.isEmpty()) {
            SaveProfile created = createProfileInternal(DEFAULT_PROFILE_NAME, true, 1);
            root.put(KEY_ACTIVE_PROFILE_ID, created.id());
            return;
        }

        String activeId = root.get(KEY_ACTIVE_PROFILE_ID, "");
        SaveProfile activeProfile = findProfileRaw(activeId);
        if (activeProfile == null) {
            activeProfile = profiles.getFirst();
            root.put(KEY_ACTIVE_PROFILE_ID, activeProfile.id());
        }
        maybeRecoverLegacySave(activeProfile, profiles);
    }

    private void migrateLegacySaveIfNeeded() {
        try {
            if (profilesRoot().childrenNames().length > 0) {
                return;
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to inspect save profile storage.", e);
        }

        boolean hasLegacyData = hasLegacySaveData();
        if (hasLegacyData) {
            createBackupSnapshot("Legacy save backup before profile migration", root.get(KEY_ACTIVE_PROFILE_ID, ""));
        }
        SaveProfile profile = createProfileInternal(DEFAULT_PROFILE_NAME, true, 1);
        if (!hasLegacyData) {
            return;
        }

        Preferences target = profileNode(profile.id()).node(NODE_SAVE);
        try {
            for (String key : root.keys()) {
                if (isRepositoryKey(key) || isGlobalKey(key)) {
                    continue;
                }
                String value = root.get(key, null);
                if (value != null) {
                    target.put(key, value);
                }
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to migrate legacy save data.", e);
        }
    }

    private boolean hasLegacySaveData() {
        try {
            return Arrays.stream(root.keys())
                    .anyMatch(key -> !isRepositoryKey(key) && !isGlobalKey(key));
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to inspect legacy save data.", e);
        }
    }

    private void maybeRecoverLegacySave(SaveProfile activeProfile, List<SaveProfile> profiles) {
        if (root.getBoolean(KEY_LEGACY_RECOVERED, false)) {
            return;
        }
        if (activeProfile == null || profiles.size() != 1) {
            return;
        }

        Preferences target = profileNode(activeProfile.id()).node(NODE_SAVE);
        int activeMeaningfulEntries = countMeaningfulSaveEntries(target, false);
        int legacyMeaningfulEntries = countMeaningfulSaveEntries(root, true);
        if (activeMeaningfulEntries > 1 || legacyMeaningfulEntries < 2 || legacyMeaningfulEntries <= activeMeaningfulEntries) {
            return;
        }

        createBackupSnapshot("Before automatic legacy recovery", activeProfile.id());
        copyLegacySave(target);
        profileNode(activeProfile.id()).putLong(KEY_UPDATED_AT, System.currentTimeMillis());
        root.putBoolean(KEY_LEGACY_RECOVERED, true);
        flushRoot();
    }

    private void copyLegacySave(Preferences target) {
        try {
            for (String key : target.keys()) {
                target.remove(key);
            }
            for (String key : root.keys()) {
                if (isRepositoryKey(key) || isGlobalKey(key)) {
                    continue;
                }
                String value = root.get(key, null);
                if (value != null) {
                    target.put(key, value);
                }
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to recover legacy save data.", e);
        }
    }

    private int countMeaningfulSaveEntries(Preferences prefs, boolean treatAsRootNode) {
        try {
            int count = 0;
            for (String key : prefs.keys()) {
                if (treatAsRootNode && (isRepositoryKey(key) || isGlobalKey(key))) {
                    continue;
                }
                if (isMeaningfulValue(prefs.get(key, null))) {
                    count++;
                }
            }
            return count;
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to inspect save data.", e);
        }
    }

    private boolean isMeaningfulValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return !trimmed.equals("0")
                && !trimmed.equals("0.0")
                && !trimmed.equals("0.00")
                && !trimmed.equals("false");
    }

    private void normalizeProfileMetadata() {
        try {
            int fallbackIndex = 1;
            for (String profileId : profilesRoot().childrenNames()) {
                Preferences profileNode = profileNode(profileId);
                long createdAt = Math.max(0L, profileNode.getLong(KEY_CREATED_AT, 0L));
                long updatedAt = Math.max(createdAt, profileNode.getLong(KEY_UPDATED_AT, createdAt));
                String name = normalizedProfileName(profileNode.get(KEY_NAME, ""), fallbackIndex);
                if (createdAt <= 0L) {
                    createdAt = System.currentTimeMillis();
                }
                if (updatedAt < createdAt) {
                    updatedAt = createdAt;
                }
                profileNode.put(KEY_NAME, name);
                profileNode.putLong(KEY_CREATED_AT, createdAt);
                profileNode.putLong(KEY_UPDATED_AT, updatedAt);
                profileNode.node(NODE_SAVE);
                fallbackIndex++;
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to normalize save profile metadata.", e);
        }
    }

    private List<SaveProfile> listProfilesRaw() {
        List<SaveProfile> profiles = new ArrayList<>();
        try {
            for (String id : profilesRoot().childrenNames()) {
                SaveProfile profile = readProfile(id);
                if (profile != null) {
                    profiles.add(profile);
                }
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to list save profiles.", e);
        }
        profiles.sort(Comparator
                .comparingLong(GameSaveRepository.SaveProfile::createdAtMillis)
                .thenComparing(GameSaveRepository.SaveProfile::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(GameSaveRepository.SaveProfile::id));
        return profiles;
    }

    private List<SaveBackup> listBackupsRaw() {
        List<SaveBackup> backups = new ArrayList<>();
        try {
            for (String id : backupsRoot().childrenNames()) {
                SaveBackup backup = readBackup(id);
                if (backup != null) {
                    backups.add(backup);
                }
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to list save backups.", e);
        }
        backups.sort(Comparator
                .comparingLong(GameSaveRepository.SaveBackup::createdAtMillis)
                .reversed()
                .thenComparing(GameSaveRepository.SaveBackup::id));
        return backups;
    }

    private SaveProfile createProfileInternal(String requestedName, boolean activate, int fallbackIndex) {
        String id = "profile_" + UUID.randomUUID().toString().replace("-", "");
        String name = normalizedProfileName(requestedName, fallbackIndex);
        long now = System.currentTimeMillis();
        Preferences profileNode = profileNode(id);
        profileNode.put(KEY_NAME, name);
        profileNode.putLong(KEY_CREATED_AT, now);
        profileNode.putLong(KEY_UPDATED_AT, now);
        profileNode.node(NODE_SAVE);
        if (activate) {
            root.put(KEY_ACTIVE_PROFILE_ID, id);
        }
        return readProfile(id);
    }

    private SaveBackup createBackupSnapshot(String requestedReason, String sourceProfileId) {
        String id = "backup_" + UUID.randomUUID().toString().replace("-", "");
        String reason = normalizedBackupReason(requestedReason);
        long now = System.currentTimeMillis();
        String activeProfileId = root.get(KEY_ACTIVE_PROFILE_ID, "");
        SaveProfile activeProfile = findProfileRaw(activeProfileId);
        Preferences backupNode = backupNode(id);
        backupNode.putLong(KEY_CREATED_AT, now);
        backupNode.put(KEY_REASON, reason);
        backupNode.put(KEY_ACTIVE_PROFILE_ID, activeProfileId);
        backupNode.put(KEY_ACTIVE_PROFILE_NAME, activeProfile == null ? "" : activeProfile.name());
        backupNode.put(KEY_SOURCE_PROFILE_ID, sourceProfileId == null ? "" : sourceProfileId);
        copyNodeContents(root, backupNode.node(NODE_SNAPSHOT), true);
        trimBackupsIfNeeded();
        flushRoot();
        return readBackup(id);
    }

    private void trimBackupsIfNeeded() {
        List<SaveBackup> backups = listBackupsRaw();
        if (backups.size() <= MAX_BACKUPS) {
            return;
        }
        for (int i = MAX_BACKUPS; i < backups.size(); i++) {
            try {
                backupNode(backups.get(i).id()).removeNode();
            } catch (BackingStoreException e) {
                throw new IllegalStateException("Failed to trim old save backups.", e);
            }
        }
    }

    private SaveProfile findProfileRaw(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }
        return listProfilesRaw().stream()
                .filter(profile -> profile.id().equals(profileId))
                .findFirst()
                .orElse(null);
    }

    private SaveProfile readProfile(String profileId) {
        try {
            if (profileId == null || profileId.isBlank() || !profilesRoot().nodeExists(profileId)) {
                return null;
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to inspect profile node " + profileId + '.', e);
        }

        Preferences profileNode = profileNode(profileId);
        long createdAt = Math.max(0L, profileNode.getLong(KEY_CREATED_AT, 0L));
        long updatedAt = Math.max(createdAt, profileNode.getLong(KEY_UPDATED_AT, createdAt));
        String name = normalizedExistingName(profileNode.get(KEY_NAME, DEFAULT_PROFILE_NAME), profileId);
        return new SaveProfile(profileId, name, createdAt, updatedAt);
    }

    private SaveBackup readBackup(String backupId) {
        try {
            if (backupId == null || backupId.isBlank() || !backupsRoot().nodeExists(backupId)) {
                return null;
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to inspect backup node " + backupId + '.', e);
        }

        Preferences backupNode = backupNode(backupId);
        return new SaveBackup(
                backupId,
                backupNode.get(KEY_REASON, "Save backup"),
                Math.max(0L, backupNode.getLong(KEY_CREATED_AT, 0L)),
                backupNode.get(KEY_ACTIVE_PROFILE_ID, ""),
                backupNode.get(KEY_ACTIVE_PROFILE_NAME, ""),
                backupNode.get(KEY_SOURCE_PROFILE_ID, "")
        );
    }

    private Preferences profilesRoot() {
        return root.node(NODE_PROFILES);
    }

    private Preferences profileNode(String profileId) {
        return profilesRoot().node(profileId);
    }

    private Preferences existingProfileNode(String profileId) {
        SaveProfile profile = findProfileRaw(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown save profile: " + profileId);
        }
        return profileNode(profile.id());
    }

    private Preferences backupsRoot() {
        return root.node(NODE_BACKUPS);
    }

    private Preferences backupNode(String backupId) {
        return backupsRoot().node(backupId);
    }

    private Preferences existingBackupNode(String backupId) {
        SaveBackup backup = readBackup(backupId);
        if (backup == null) {
            throw new IllegalArgumentException("Unknown save backup: " + backupId);
        }
        return backupNode(backup.id());
    }

    private boolean isRepositoryKey(String key) {
        return KEY_ACTIVE_PROFILE_ID.equals(key)
                || KEY_MIGRATED.equals(key)
                || KEY_LEGACY_RECOVERED.equals(key)
                || KEY_SCHEMA_VERSION.equals(key);
    }

    private void markLegacyRecoveryHandled() {
        root.putBoolean(KEY_LEGACY_RECOVERED, true);
    }

    private boolean isGlobalKey(String key) {
        if (GLOBAL_KEYS.contains(key)) {
            return true;
        }
        return GLOBAL_KEY_PREFIXES.stream().anyMatch(key::startsWith);
    }

    private String normalizedProfileName(String requestedName, int fallbackIndex) {
        if (requestedName == null) {
            return "Profile " + Math.max(1, fallbackIndex);
        }
        String trimmed = requestedName.trim();
        if (trimmed.isEmpty()) {
            return "Profile " + Math.max(1, fallbackIndex);
        }
        return trimmed.length() > 40 ? trimmed.substring(0, 40).trim() : trimmed;
    }

    private String normalizedExistingName(String storedName, String profileId) {
        String normalized = normalizedProfileName(storedName, 1);
        if (!normalized.isBlank()) {
            return normalized;
        }
        String suffix = profileId == null ? "" : profileId.toLowerCase(Locale.ROOT);
        return suffix.isBlank() ? DEFAULT_PROFILE_NAME : "Profile " + suffix;
    }

    private String normalizedBackupReason(String requestedReason) {
        if (requestedReason == null || requestedReason.isBlank()) {
            return "Save backup";
        }
        String trimmed = requestedReason.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80).trim() : trimmed;
    }

    private void copyNodeContents(Preferences source, Preferences target, boolean excludeBackupsFromRoot) {
        clearNodeContents(target, Set.of());
        copyNodeContentsIntoExisting(source, target, excludeBackupsFromRoot);
    }

    private void copyNodeContentsIntoExisting(Preferences source, Preferences target, boolean excludeBackupsFromRoot) {
        try {
            for (String key : source.keys()) {
                String value = source.get(key, null);
                if (value != null) {
                    target.put(key, value);
                }
            }
            boolean sourceIsRoot = sameNode(source, root);
            for (String childName : source.childrenNames()) {
                if (excludeBackupsFromRoot && sourceIsRoot && NODE_BACKUPS.equals(childName)) {
                    continue;
                }
                copyNodeContentsIntoExisting(source.node(childName), target.node(childName), excludeBackupsFromRoot);
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to copy save data.", e);
        }
    }

    private void clearNodeContents(Preferences node, Set<String> preservedChildren) {
        try {
            for (String key : node.keys()) {
                node.remove(key);
            }
            for (String childName : node.childrenNames()) {
                if (preservedChildren.contains(childName)) {
                    continue;
                }
                node.node(childName).removeNode();
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to clear save data.", e);
        }
    }

    private void replaceRootWithSnapshot(Preferences snapshotNode) {
        clearNodeContents(root, Set.of(NODE_BACKUPS));
        copyNodeContentsIntoExisting(snapshotNode, root, false);
    }

    private void validateImport(Properties properties) {
        String versionText = properties.getProperty(EXPORT_VERSION_KEY, "");
        int version;
        try {
            version = Integer.parseInt(versionText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Save file is not a valid Bird Fight 3 export.");
        }
        if (version != EXPORT_FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported save export version: " + version);
        }
    }

    private void writeExportNode(Properties properties, Preferences node, String relativePath, boolean excludeBackupsFromRoot) {
        try {
            for (String key : node.keys()) {
                properties.setProperty(exportPropertyKey(relativePath, key), node.get(key, ""));
            }
            boolean nodeIsRoot = sameNode(node, root);
            for (String childName : node.childrenNames()) {
                if (excludeBackupsFromRoot && nodeIsRoot && NODE_BACKUPS.equals(childName)) {
                    continue;
                }
                String childPath = relativePath.isBlank() ? childName : relativePath + "/" + childName;
                writeExportNode(properties, node.node(childName), childPath, excludeBackupsFromRoot);
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to export save data.", e);
        }
    }

    private void applyImportedProperties(Properties properties) {
        for (String propertyKey : properties.stringPropertyNames()) {
            if (EXPORT_VERSION_KEY.equals(propertyKey)) {
                continue;
            }
            if (!propertyKey.startsWith(EXPORT_VALUE_PREFIX)) {
                continue;
            }
            String encodedRemainder = propertyKey.substring(EXPORT_VALUE_PREFIX.length());
            int split = encodedRemainder.indexOf('|');
            if (split <= 0 && !encodedRemainder.startsWith("|")) {
                continue;
            }
            String encodedPath = split < 0 ? encodedRemainder : encodedRemainder.substring(0, split);
            String encodedKey = split < 0 ? "" : encodedRemainder.substring(split + 1);
            String relativePath = decodeExportToken(encodedPath);
            String key = decodeExportToken(encodedKey);
            if (key.isBlank()) {
                continue;
            }
            Preferences node = relativePath.isBlank() ? root : root.node(relativePath);
            node.put(key, properties.getProperty(propertyKey, ""));
        }
    }

    private String exportPropertyKey(String relativePath, String key) {
        return EXPORT_VALUE_PREFIX + encodeExportToken(relativePath) + "|" + encodeExportToken(key);
    }

    private String encodeExportToken(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeExportToken(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Save file is not a valid Bird Fight 3 export.", e);
        }
    }

    private boolean sameNode(Preferences left, Preferences right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.absolutePath(), right.absolutePath());
    }

    private void flushRoot() {
        try {
            root.flush();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to flush save data.", e);
        }
    }
}
