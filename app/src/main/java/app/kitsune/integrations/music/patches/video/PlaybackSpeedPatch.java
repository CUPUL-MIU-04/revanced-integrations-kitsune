package app.kitsune.integrations.music.patches.video;

import static app.kitsune.integrations.shared.utils.StringRef.str;
import static app.kitsune.integrations.shared.utils.Utils.showToastShort;

import app.kitsune.integrations.music.settings.Settings;
import app.kitsune.integrations.shared.utils.Logger;

@SuppressWarnings("unused")
public class PlaybackSpeedPatch {

    public static float getPlaybackSpeed(final float playbackSpeed) {
        try {
            return Settings.DEFAULT_PLAYBACK_SPEED.get();
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to getPlaybackSpeed", ex);
        }
        return playbackSpeed;
    }

    public static void userSelectedPlaybackSpeed(final float playbackSpeed) {
        if (!Settings.REMEMBER_PLAYBACK_SPEED_LAST_SELECTED.get())
            return;

        Settings.DEFAULT_PLAYBACK_SPEED.save(playbackSpeed);

        if (!Settings.REMEMBER_PLAYBACK_SPEED_LAST_SELECTED_TOAST.get())
            return;

        showToastShort(str("revanced_remember_playback_speed_toast", playbackSpeed + "x"));
    }
}
