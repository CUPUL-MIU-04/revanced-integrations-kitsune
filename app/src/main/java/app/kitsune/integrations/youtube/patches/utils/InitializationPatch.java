package app.kitsune.integrations.youtube.patches.utils;

import static app.kitsune.integrations.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.kitsune.integrations.shared.utils.StringRef.str;
import static app.kitsune.integrations.shared.utils.Utils.runOnMainThreadDelayed;

import android.app.Activity;

import androidx.annotation.NonNull;

import app.kitsune.integrations.shared.settings.BaseSettings;
import app.kitsune.integrations.shared.settings.BooleanSetting;
import app.kitsune.integrations.youtube.utils.ExtendedUtils;

@SuppressWarnings("unused")
public class InitializationPatch {
    private static final BooleanSetting SETTINGS_INITIALIZED = BaseSettings.SETTINGS_INITIALIZED;

    /**
     * Some layouts that depend on litho do not load when the app is first installed.
     * (Also reproduced on unPatched YouTube)
     * <p>
     * To fix this, show the restart dialog when the app is installed for the first time.
     */
    public static void onCreate(@NonNull Activity mActivity) {
        if (SETTINGS_INITIALIZED.get()) {
            return;
        }
        runOnMainThreadDelayed(() -> showRestartDialog(mActivity, str("revanced_extended_restart_first_run"), 3500), 500);
        runOnMainThreadDelayed(() -> SETTINGS_INITIALIZED.save(true), 1000);
    }

    public static void setExtendedUtils(@NonNull Activity mActivity) {
        ExtendedUtils.setApplicationLabel();
        ExtendedUtils.setSmallestScreenWidthDp();
        ExtendedUtils.setVersionName();
        ExtendedUtils.setPlayerFlyoutMenuAdditionalSettings();
    }
}