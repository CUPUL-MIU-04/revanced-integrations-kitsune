package app.kitsune.integrations.music.patches.utils;

import static app.kitsune.integrations.music.utils.RestartUtils.showRestartDialog;

import android.app.Activity;

import androidx.annotation.NonNull;

import app.kitsune.integrations.music.utils.ExtendedUtils;
import app.kitsune.integrations.shared.settings.BaseSettings;
import app.kitsune.integrations.shared.utils.Utils;

@SuppressWarnings("unused")
public class InitializationPatch {

    /**
     * The new layout is not loaded normally when the app is first installed.
     * (Also reproduced on unPatched YouTube Music)
     * <p>
     * To fix this, show the reboot dialog when the app is installed for the first time.
     */
    public static void onCreate(@NonNull Activity mActivity) {
        if (BaseSettings.SETTINGS_INITIALIZED.get())
            return;

        showRestartDialog(mActivity, "revanced_extended_restart_first_run", 3000);
        Utils.runOnMainThreadDelayed(() -> BaseSettings.SETTINGS_INITIALIZED.save(true), 3000);
    }

    public static void setDeviceInformation(@NonNull Activity mActivity) {
        ExtendedUtils.setApplicationLabel();
        ExtendedUtils.setVersionName();
    }
}