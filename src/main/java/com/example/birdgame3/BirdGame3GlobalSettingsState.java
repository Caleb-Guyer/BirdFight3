package com.example.birdgame3;

import java.util.prefs.Preferences;

final class BirdGame3GlobalSettingsState {
    private static final String KEY_LAN_LAST_HOST = "lan_last_host";
    private static final String KEY_MUSIC_ENABLED = "setting_music";
    private static final String KEY_SFX_ENABLED = "setting_sfx";
    private static final String KEY_MUSIC_VOLUME = "setting_music_volume";
    private static final String KEY_SFX_VOLUME = "setting_sfx_volume";
    private static final String KEY_SCREEN_SHAKE = "setting_shake";
    private static final String KEY_FULLSCREEN = "setting_fullscreen";
    private static final String KEY_PARTICLES = "setting_particles";
    private static final String KEY_AMBIENT_FX = "setting_ambient_fx";
    private static final String KEY_FPS_CAP = "setting_fps_cap";
    private static final String WIIMOTE_MODE_PREFIX = "setting_wiimote_mode_p";

    String[][] controlBindingNames = new String[0][];
    String[] wiimoteModeNames = new String[0];
    String lanLastHost = "";
    boolean musicEnabled = true;
    boolean sfxEnabled = true;
    double musicVolume = 1.0;
    double sfxVolume = 1.0;
    boolean screenShakeEnabled = true;
    boolean fullscreenEnabled = true;
    boolean particleEffectsEnabled = true;
    boolean ambientEffectsEnabled = true;
    int fpsCap = 60;

    static BirdGame3GlobalSettingsState load(Preferences prefs, String[] controlActionPrefKeys, int playerCount) {
        BirdGame3GlobalSettingsState state = new BirdGame3GlobalSettingsState();
        int safePlayerCount = Math.max(0, playerCount);
        int actionCount = controlActionPrefKeys == null ? 0 : controlActionPrefKeys.length;
        state.controlBindingNames = new String[safePlayerCount][actionCount];
        state.wiimoteModeNames = new String[safePlayerCount];

        for (int playerIdx = 0; playerIdx < safePlayerCount; playerIdx++) {
            for (int actionIdx = 0; actionIdx < actionCount; actionIdx++) {
                state.controlBindingNames[playerIdx][actionIdx] = prefs == null
                        ? null
                        : prefs.get(controlPrefKey(playerIdx, controlActionPrefKeys[actionIdx]), null);
            }
            state.wiimoteModeNames[playerIdx] = prefs == null
                    ? WiimoteControlMode.OFF.name()
                    : prefs.get(wiimotePrefKey(playerIdx), WiimoteControlMode.OFF.name());
        }

        if (prefs == null) {
            return state;
        }

        state.lanLastHost = prefs.get(KEY_LAN_LAST_HOST, "");
        state.musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true);
        state.sfxEnabled = prefs.getBoolean(KEY_SFX_ENABLED, true);
        state.musicVolume = sanitizeVolume(prefs.getDouble(KEY_MUSIC_VOLUME, state.musicEnabled ? 1.0 : 0.0));
        state.sfxVolume = sanitizeVolume(prefs.getDouble(KEY_SFX_VOLUME, state.sfxEnabled ? 1.0 : 0.0));
        state.musicEnabled = !isMutedVolume(state.musicVolume);
        state.sfxEnabled = !isMutedVolume(state.sfxVolume);
        state.screenShakeEnabled = prefs.getBoolean(KEY_SCREEN_SHAKE, true);
        state.fullscreenEnabled = prefs.getBoolean(KEY_FULLSCREEN, true);
        state.particleEffectsEnabled = prefs.getBoolean(KEY_PARTICLES, true);
        state.ambientEffectsEnabled = prefs.getBoolean(KEY_AMBIENT_FX, true);
        state.fpsCap = FrameRateLimiter.sanitizeFpsCap(prefs.getInt(KEY_FPS_CAP, 60));
        return state;
    }

    void saveTo(Preferences prefs, String[] controlActionPrefKeys) {
        if (prefs == null) {
            return;
        }

        if (controlActionPrefKeys != null) {
            for (int playerIdx = 0; playerIdx < controlBindingNames.length; playerIdx++) {
                String[] bindings = controlBindingNames[playerIdx];
                for (int actionIdx = 0; actionIdx < Math.min(bindings.length, controlActionPrefKeys.length); actionIdx++) {
                    String keyName = bindings[actionIdx];
                    if (keyName == null || keyName.isBlank()) {
                        prefs.remove(controlPrefKey(playerIdx, controlActionPrefKeys[actionIdx]));
                    } else {
                        prefs.put(controlPrefKey(playerIdx, controlActionPrefKeys[actionIdx]), keyName);
                    }
                }
            }
        }

        for (int playerIdx = 0; playerIdx < wiimoteModeNames.length; playerIdx++) {
            String modeName = wiimoteModeNames[playerIdx];
            prefs.put(wiimotePrefKey(playerIdx), modeName == null || modeName.isBlank()
                    ? WiimoteControlMode.OFF.name()
                    : modeName);
        }

        prefs.put(KEY_LAN_LAST_HOST, nullToEmpty(lanLastHost));
        prefs.putBoolean(KEY_MUSIC_ENABLED, musicEnabled);
        prefs.putBoolean(KEY_SFX_ENABLED, sfxEnabled);
        prefs.putDouble(KEY_MUSIC_VOLUME, sanitizeVolume(musicVolume));
        prefs.putDouble(KEY_SFX_VOLUME, sanitizeVolume(sfxVolume));
        prefs.putBoolean(KEY_SCREEN_SHAKE, screenShakeEnabled);
        prefs.putBoolean(KEY_FULLSCREEN, fullscreenEnabled);
        prefs.putBoolean(KEY_PARTICLES, particleEffectsEnabled);
        prefs.putBoolean(KEY_AMBIENT_FX, ambientEffectsEnabled);
        prefs.putInt(KEY_FPS_CAP, FrameRateLimiter.sanitizeFpsCap(fpsCap));
    }

    private static String controlPrefKey(int playerIdx, String actionPrefKey) {
        return "bind_p" + (playerIdx + 1) + "_" + actionPrefKey;
    }

    private static String wiimotePrefKey(int playerIdx) {
        return WIIMOTE_MODE_PREFIX + (playerIdx + 1);
    }

    private static double sanitizeVolume(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.clamp(value, 0.0, 1.0);
    }

    private static boolean isMutedVolume(double value) {
        return sanitizeVolume(value) <= 0.0001;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
