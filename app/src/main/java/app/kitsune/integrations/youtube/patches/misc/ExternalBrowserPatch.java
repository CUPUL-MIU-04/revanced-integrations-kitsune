package app.kitsune.integrations.youtube.patches.misc;

import app.kitsune.integrations.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ExternalBrowserPatch {

    public static String enableExternalBrowser(final String original) {
        if (!Settings.ENABLE_EXTERNAL_BROWSER.get())
            return original;

        return "";
    }
}
