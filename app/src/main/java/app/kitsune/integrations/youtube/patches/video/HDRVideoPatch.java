package app.kitsune.integrations.youtube.patches.video;

import app.kitsune.integrations.youtube.settings.Settings;

@SuppressWarnings("unused")
public class HDRVideoPatch {

    public static boolean disableHDRVideo() {
        return !Settings.DISABLE_HDR_VIDEO.get();
    }
}
