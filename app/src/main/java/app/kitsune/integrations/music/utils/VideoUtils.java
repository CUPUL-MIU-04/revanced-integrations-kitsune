package app.kitsune.integrations.music.utils;

import static app.kitsune.integrations.music.settings.preference.ExternalDownloaderPreference.checkPackageIsEnabled;
import static app.kitsune.integrations.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;

import app.kitsune.integrations.music.settings.Settings;
import app.kitsune.integrations.music.shared.VideoInformation;
import app.kitsune.integrations.shared.settings.StringSetting;
import app.kitsune.integrations.shared.utils.IntentUtils;
import app.kitsune.integrations.shared.utils.Logger;

@SuppressWarnings("unused")
public class VideoUtils extends IntentUtils {
    private static final StringSetting externalDownloaderPackageName =
            Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME;

    public static void launchExternalDownloader() {
        launchExternalDownloader(VideoInformation.getVideoId());
    }

    public static void launchExternalDownloader(@NonNull String videoId) {
        try {
            String downloaderPackageName = externalDownloaderPackageName.get().trim();

            if (downloaderPackageName.isEmpty()) {
                externalDownloaderPackageName.resetToDefault();
                downloaderPackageName = externalDownloaderPackageName.defaultValue;
            }

            if (!checkPackageIsEnabled()) {
                return;
            }

            final String content = String.format("https://music.youtube.com/watch?v=%s", videoId);
            launchExternalDownloader(content, downloaderPackageName);
        } catch (Exception ex) {
            Logger.printException(() -> "launchExternalDownloader failure", ex);
        }
    }

    @SuppressLint("IntentReset")
    public static void openInYouTube() {
        final String videoId = VideoInformation.getVideoId();
        if (videoId.isEmpty()) {
            showToastShort(str("revanced_replace_flyout_menu_dismiss_queue_watch_on_youtube_warning"));
            return;
        }

        if (context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE) instanceof AudioManager audioManager) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        String url = String.format("vnd.youtube://%s", videoId);
        if (Settings.REPLACE_FLYOUT_MENU_DISMISS_QUEUE_CONTINUE_WATCH.get()) {
            long seconds = VideoInformation.getVideoTime() / 1000;
            url += String.format("?t=%s", seconds);
        }

        launchView(url);
    }

    public static void openInYouTubeMusic(@NonNull String songId) {
        final String url = String.format("vnd.youtube.music://%s", songId);
        launchView(url, context.getPackageName());
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void shuffleTracks() {
        Log.d("Extended: VideoUtils", "Tracks are shuffled");
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void showPlaybackSpeedFlyoutMenu() {
        Logger.printDebug(() -> "Playback speed flyout menu opened");
    }
}
