package app.kitsune.integrations.reddit.patches;

import app.kitsune.integrations.reddit.settings.Settings;

@SuppressWarnings("unused")
public final class SanitizeUrlQueryPatch {

    public static boolean stripQueryParameters() {
        return Settings.SANITIZE_URL_QUERY.get();
    }

}
