package app.kitsune.integrations.youtube.patches.general;

import static app.kitsune.integrations.youtube.patches.general.MiniplayerPatch.MiniplayerType.MODERN_1;
import static app.kitsune.integrations.youtube.patches.general.MiniplayerPatch.MiniplayerType.MODERN_2;
import static app.kitsune.integrations.youtube.patches.general.MiniplayerPatch.MiniplayerType.MODERN_3;
import static app.kitsune.integrations.youtube.patches.general.MiniplayerPatch.MiniplayerType.ORIGINAL;
import static app.kitsune.integrations.youtube.utils.ExtendedUtils.validateValue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.shared.utils.ResourceUtils;
import app.kitsune.integrations.shared.utils.Utils;
import app.kitsune.integrations.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class MiniplayerPatch {

    /**
     * Mini player type. Null fields indicates to use the original un-patched value.
     */
    public enum MiniplayerType {
        /**
         * Unmodified type, and same as un-patched.
         */
        ORIGINAL(null, null),
        PHONE(false, null),
        TABLET(true, null),
        MODERN_1(null, 1),
        MODERN_2(null, 2),
        MODERN_3(null, 3);

        /**
         * Legacy tablet hook value.
         */
        @Nullable
        final Boolean legacyTabletOverride;

        /**
         * Modern player type used by YT.
         */
        @Nullable
        final Integer modernPlayerType;

        MiniplayerType(@Nullable Boolean legacyTabletOverride, @Nullable Integer modernPlayerType) {
            this.legacyTabletOverride = legacyTabletOverride;
            this.modernPlayerType = modernPlayerType;
        }

        public boolean isModern() {
            return modernPlayerType != null;
        }
    }

    /**
     * Modern subtitle overlay for {@link MiniplayerType#MODERN_2}.
     * Resource is not present in older targets, and this field will be zero.
     */
    private static final int MODERN_OVERLAY_SUBTITLE_TEXT
            = ResourceUtils.getIdIdentifier("modern_miniplayer_subtitle_text");

    private static final MiniplayerType CURRENT_TYPE = Settings.MINIPLAYER_TYPE.get();

    private static final boolean DOUBLE_TAP_ACTION_ENABLED =
            (CURRENT_TYPE == MODERN_1 || CURRENT_TYPE == MODERN_2 || CURRENT_TYPE == MODERN_3) && Settings.MINIPLAYER_DOUBLE_TAP_ACTION.get();

    private static final boolean DRAG_AND_DROP_ENABLED =
            CURRENT_TYPE == MODERN_1 && Settings.MINIPLAYER_DRAG_AND_DROP.get();

    private static final boolean HIDE_EXPAND_CLOSE_AVAILABLE =
            (CURRENT_TYPE == MODERN_1 || CURRENT_TYPE == MODERN_3) &&
                    !DOUBLE_TAP_ACTION_ENABLED &&
                    !DRAG_AND_DROP_ENABLED;

    private static final boolean HIDE_EXPAND_CLOSE_ENABLED =
            HIDE_EXPAND_CLOSE_AVAILABLE && Settings.MINIPLAYER_HIDE_EXPAND_CLOSE.get();

    private static final boolean HIDE_SUBTEXT_ENABLED =
            (CURRENT_TYPE == MODERN_1 || CURRENT_TYPE == MODERN_3) && Settings.MINIPLAYER_HIDE_SUBTEXT.get();

    private static final boolean HIDE_REWIND_FORWARD_ENABLED =
            CURRENT_TYPE == MODERN_1 && Settings.MINIPLAYER_HIDE_REWIND_FORWARD.get();

    private static final int OPACITY_LEVEL;

    static {
        final int opacity = validateValue(
                Settings.MINIPLAYER_OPACITY,
                0,
                100,
                "revanced_miniplayer_opacity_invalid_toast"
        );

        OPACITY_LEVEL = (opacity * 255) / 100;
    }

    /**
     * Injection point.
     */
    public static boolean getLegacyTabletMiniplayerOverride(boolean original) {
        Boolean isTablet = CURRENT_TYPE.legacyTabletOverride;
        return isTablet == null
                ? original
                : isTablet;
    }

    /**
     * Injection point.
     */
    public static boolean getModernMiniplayerOverride(boolean original) {
        return CURRENT_TYPE == ORIGINAL
                ? original
                : CURRENT_TYPE.isModern();
    }

    /**
     * Injection point.
     */
    public static int getModernMiniplayerOverrideType(int original) {
        Integer modernValue = CURRENT_TYPE.modernPlayerType;
        return modernValue == null
                ? original
                : modernValue;
    }

    /**
     * Injection point.
     */
    public static void adjustMiniplayerOpacity(ImageView view) {
        if (CURRENT_TYPE == MODERN_1) {
            view.setImageAlpha(OPACITY_LEVEL);
        }
    }

    /**
     * Injection point.
     */
    public static boolean enableMiniplayerDoubleTapAction() {
        return DOUBLE_TAP_ACTION_ENABLED;
    }

    /**
     * Injection point.
     */
    public static boolean enableMiniplayerDragAndDrop() {
        return DRAG_AND_DROP_ENABLED;
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerExpandClose(ImageView view) {
        Utils.hideViewByRemovingFromParentUnderCondition(HIDE_EXPAND_CLOSE_ENABLED, view);
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerRewindForward(ImageView view) {
        Utils.hideViewByRemovingFromParentUnderCondition(HIDE_REWIND_FORWARD_ENABLED, view);
    }

    /**
     * Injection point.
     */
    public static boolean hideMiniplayerSubTexts(View view) {
        // Different subviews are passed in, but only TextView and layouts are of interest here.
        final boolean hideView = HIDE_SUBTEXT_ENABLED && (view instanceof TextView || view instanceof LinearLayout);
        Utils.hideViewByRemovingFromParentUnderCondition(hideView, view);
        return hideView || view == null;
    }

    /**
     * Injection point.
     */
    public static void playerOverlayGroupCreated(View group) {
        // Modern 2 has an half broken subtitle that is always present.
        // Always hide it to make the miniplayer mostly usable.
        if (CURRENT_TYPE == MODERN_2 && MODERN_OVERLAY_SUBTITLE_TEXT != 0) {
            if (group instanceof ViewGroup viewGroup) {
                View subtitleText = Utils.getChildView(viewGroup, true,
                        view -> view.getId() == MODERN_OVERLAY_SUBTITLE_TEXT);

                if (subtitleText != null) {
                    subtitleText.setVisibility(View.GONE);
                    Logger.printDebug(() -> "Modern overlay subtitle view set to hidden");
                }
            }
        }
    }
}
