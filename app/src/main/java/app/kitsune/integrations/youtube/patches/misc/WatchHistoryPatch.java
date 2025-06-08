package app.kitsune.integrations.youtube.patches.misc;

import android.net.Uri;

import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class WatchHistoryPatch {

    public enum WatchHistoryType {
        ORIGINAL,
        REPLACE,
        BLOCK
    }

    private static final Uri UNREACHABLE_HOST_URI = Uri.parse("https://127.0.0.0");
    private static final String WWW_TRACKING_URL_AUTHORITY = "www.youtube.com";

    public static Uri replaceTrackingUrl(Uri trackingUrl) {
        final WatchHistoryType watchHistoryType = Settings.WATCH_HISTORY_TYPE.get();
        if (watchHistoryType != WatchHistoryType.ORIGINAL) {
            try {
                if (watchHistoryType == WatchHistoryType.REPLACE) {
                    return trackingUrl.buildUpon().authority(WWW_TRACKING_URL_AUTHORITY).build();
                } else if (watchHistoryType == WatchHistoryType.BLOCK) {
                    return UNREACHABLE_HOST_URI;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "replaceTrackingUrl failure", ex);
            }
        }

        return trackingUrl;
    }

}
