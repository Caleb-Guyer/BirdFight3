package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class GameSaveRepository {
    private static final String KEY_ACTIVE_PROFILE_ID = "save_active_profile_id";
    private static final String KEY_MIGRATED = "save_profiles_migrated";
    private static final String KEY_LEGACY_RECOVERED = "save_legacy_recovered";
    private static final String NODE_PROFILES = "save_profiles";
    private static final String NODE_SAVE = "save";
    private static final String KEY_NAME = "name";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String DEFAULT_PROFILE_NAME = "Profile 1";

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

    SaveProfile createProfile(String requestedName, boolean activate) {
        ensureInitialized();
        markLegacyRecoveryHandled();
        return createProfileInternal(requestedName, activate, listProfilesRaw().size() + 1);
    }

    SaveProfile renameProfile(String profileId, String requestedName) {
        ensureInitialized();
        Preferences profileNode = existingProfileNode(profileId);
        String name = normalizedProfileName(requestedName, listProfilesRaw().size());
        profileNode.put(KEY_NAME, name);
        profileNode.putLong(KEY_UPDATED_AT, System.currentTimeMillis());
        return readProfile(profileId);
    }

    void setActiveProfile(String profileId) {
        ensureInitialized();
        existingProfileNode(profileId);
        markLegacyRecoveryHandled();
        root.put(KEY_ACTIVE_PROFILE_ID, profileId);
    }

    void clearProfile(String profileId) {
        ensureInitialized();
        Preferences profileNode = existingProfileNode(profileId);
        markLegacyRecoveryHandled();
        try {
            if (profileNode.nodeExists(NODE_SAVE)) {
                profileNode.node(NODE_SAVE).removeNode();
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to clear save profile " + profileId + '.', e);
        }
        profileNode.node(NODE_SAVE);
        profileNode.putLong(KEY_UPDATED_AT, System.currentTimeMillis());
    }

    SaveProfile deleteProfile(String profileId) {
        ensureInitialized();
        List<SaveProfile> profiles = listProfilesRaw();
        if (profiles.size() <= 1) {
            throw new IllegalStateException("At least one save profile must remain.");
        }

        Preferences profileNode = existingProfileNode(profileId);
        boolean deletedActive = activeProfile().id().equals(profileId);
        markLegacyRecoveryHandled();
        try {
            profileNode.removeNode();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to delete save profile " + profileId + '.', e);
        }

        if (deletedActive) {
            root.put(KEY_ACTIVE_PROFILE_ID, listProfilesRaw().getFirst().id());
        }
        return activeProfile();
    }

    void touchActiveProfile() {
        ensureInitialized();
        Preferences profileNode = existingProfileNode(activeProfile().id());
        profileNode.putLong(KEY_UPDATED_AT, System.currentTimeMillis());
    }

    private void ensureInitialized() {
        if (!root.getBoolean(KEY_MIGRATED, false)) {
            migrateLegacySaveIfNeeded();
            root.putBoolean(KEY_MIGRATED, true);
        }

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

        SaveProfile profile = createProfileInternal(DEFAULT_PROFILE_NAME, true, 1);
        if (!hasLegacySaveData()) {
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

        copyLegacySave(target);
        profileNode(activeProfile.id()).putLong(KEY_UPDATED_AT, System.currentTimeMillis());
        root.putBoolean(KEY_LEGACY_RECOVERED, true);
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

    private boolean isRepositoryKey(String key) {
        return KEY_ACTIVE_PROFILE_ID.equals(key)
                || KEY_MIGRATED.equals(key)
                || KEY_LEGACY_RECOVERED.equals(key);
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
}
