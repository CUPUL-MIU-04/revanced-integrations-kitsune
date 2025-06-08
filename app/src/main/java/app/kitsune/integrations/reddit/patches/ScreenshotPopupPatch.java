package app.kitsune.integrations.reddit.patches;

import app.kitsune.integrations.reddit.settings.Settings;

@SuppressWarnings("unused")
public class ScreenshotPopupPatch {

    public static boolean disableScreenshotPopup() {
        return Settings.DISABLE_SCREENSHOT_POPUP.get();
    }
}
