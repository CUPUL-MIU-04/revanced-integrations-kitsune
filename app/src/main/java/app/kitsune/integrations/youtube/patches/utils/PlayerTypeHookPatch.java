package app.kitsune.integrations.youtube.patches.utils;

import android.view.View;

import androidx.annotation.Nullable;

import app.kitsune.integrations.youtube.shared.PlayerType;
import app.kitsune.integrations.youtube.shared.ShortsPlayerState;
import app.kitsune.integrations.youtube.shared.VideoState;

@SuppressWarnings("unused")
public class PlayerTypeHookPatch {
    /**
     * Injection point.
     */
    public static void setPlayerType(@Nullable Enum<?> youTubePlayerType) {
        if (youTubePlayerType == null) return;

        PlayerType.setFromString(youTubePlayerType.name());
    }

    /**
     * Injection point.
     */
    public static void setVideoState(@Nullable Enum<?> youTubeVideoState) {
        if (youTubeVideoState == null) return;

        VideoState.setFromString(youTubeVideoState.name());
    }

    /**
     * Injection point.
     * <p>
     * Add a listener to the shorts player overlay View.
     * Triggered when a shorts player is attached or detached to Windows.
     *
     * @param view shorts player overlay (R.id.reel_watch_player).
     */
    public static void onShortsCreate(View view) {
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@Nullable View v) {
                ShortsPlayerState.set(ShortsPlayerState.OPEN);
            }
            @Override
            public void onViewDetachedFromWindow(@Nullable View v) {
                ShortsPlayerState.set(ShortsPlayerState.CLOSED);
            }
        });
    }
}

