package app.kitsune.integrations.youtube.patches.misc;

import app.kitsune.integrations.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public class BackgroundPlaybackPatch {

    public static boolean allowBackgroundPlayback(boolean original) {
        return original || ShortsPlayerState.getCurrent().isClosed();
    }

}
