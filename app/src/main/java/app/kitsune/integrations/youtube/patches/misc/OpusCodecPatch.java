package app.kitsune.integrations.youtube.patches.misc;

import app.kitsune.integrations.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpusCodecPatch {

    public static boolean enableOpusCodec() {
        return Settings.ENABLE_OPUS_CODEC.get();
    }
}
