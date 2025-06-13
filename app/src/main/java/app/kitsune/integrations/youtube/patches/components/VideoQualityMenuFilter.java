package app.kitsune.integrations.youtube.patches.components;

import androidx.annotation.Nullable;

import app.kitsune.integrations.shared.patches.components.Filter;
import app.kitsune.integrations.shared.patches.components.StringFilterGroup;
import app.kitsune.integrations.youtube.patches.video.RestoreOldVideoQualityMenuPatch;
import app.kitsune.integrations.youtube.settings.Settings;

/**
 * Abuse LithoFilter for {@link RestoreOldVideoQualityMenuPatch}.
 */
public final class VideoQualityMenuFilter extends Filter {
    // Must be volatile or synchronized, as litho filtering runs off main thread and this field is then access from the main thread.
    public static volatile boolean isVideoQualityMenuVisible;

    public VideoQualityMenuFilter() {
        addPathCallbacks(
                new StringFilterGroup(
                        Settings.RESTORE_OLD_VIDEO_QUALITY_MENU,
                        "quick_quality_sheet_content.eml-js"
                )
        );
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        isVideoQualityMenuVisible = true;

        return false;
    }
}
