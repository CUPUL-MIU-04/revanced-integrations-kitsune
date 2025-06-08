package app.kitsune.integrations.reddit.patches;

import android.net.Uri;

import app.kitsune.integrations.reddit.settings.Settings;
import app.kitsune.integrations.shared.utils.Logger;

@SuppressWarnings("unused")
public final class OpenLinksDirectlyPatch {

    /**
     * Parses the given Reddit redirect uri by extracting the redirect query.
     *
     * @param uri The Reddit redirect uri.
     * @return The redirect query.
     */
    public static Uri parseRedirectUri(Uri uri) {
        try {
            if (Settings.OPEN_LINKS_DIRECTLY.get()) {
                final String parsedUri = uri.getQueryParameter("url");
                if (parsedUri != null && !parsedUri.isEmpty())
                    return Uri.parse(parsedUri);
            }
        } catch (Exception e) {
            Logger.printException(() -> "Can not parse URL: " + uri, e);
        }
        return uri;
    }

}
