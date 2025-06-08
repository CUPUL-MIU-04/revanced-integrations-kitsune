package app.kitsune.integrations.youtube.patches.volume;

import android.content.Context;
import android.content.SharedPreferences;
import app.kitsune.integrations.youtube.SharedPrefs;

public class StableVolumePatch {
    private static final String PREF_KEY = "revanced_stable_volume_enabled";
    private static final String VOLUME_LEVEL_KEY = "revanced_stable_volume_level";
    private static final int DEFAULT_VOLUME_LEVEL = 7;

    public static void enableStableVolume(Context context, boolean enable) {
        SharedPreferences.Editor editor = SharedPrefs.getPreferences(context).edit();
        editor.putBoolean(PREF_KEY, enable).apply();
    }

    public static boolean isStableVolumeEnabled(Context context) {
        return SharedPrefs.getPreferences(context).getBoolean(PREF_KEY, false);
    }

    public static int getFixedVolumeLevel(Context context) {
        if (!isStableVolumeEnabled(context)) {
            return -1; // Indica que está desactivado
        }
        return SharedPrefs.getPreferences(context).getInt(VOLUME_LEVEL_KEY, DEFAULT_VOLUME_LEVEL);
    }

    public static void setFixedVolumeLevel(Context context, int level) {
        if (level < 0 || level > 15) {
            throw new IllegalArgumentException("Volume level must be between 0-15");
        }
        SharedPreferences.Editor editor = SharedPrefs.getPreferences(context).edit();
        editor.putInt(VOLUME_LEVEL_KEY, level).apply();
    }
}