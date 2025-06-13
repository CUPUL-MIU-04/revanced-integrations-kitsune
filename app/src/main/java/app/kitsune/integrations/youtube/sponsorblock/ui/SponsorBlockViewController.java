package app.kitsune.integrations.youtube.sponsorblock.ui;

import static app.kitsune.integrations.shared.utils.ResourceUtils.getDimension;
import static app.kitsune.integrations.shared.utils.ResourceUtils.getLayoutIdentifier;
import static app.kitsune.integrations.shared.utils.Utils.getChildView;
import static app.kitsune.integrations.youtube.utils.ExtendedUtils.isFullscreenHidden;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.shared.utils.Utils;
import app.kitsune.integrations.youtube.patches.player.PlayerPatch;
import app.kitsune.integrations.youtube.settings.Settings;
import app.kitsune.integrations.youtube.shared.PlayerType;
import app.kitsune.integrations.youtube.sponsorblock.objects.SponsorSegment;

@SuppressWarnings("unused")
public class SponsorBlockViewController {
    private static WeakReference<RelativeLayout> inlineSponsorOverlayRef = new WeakReference<>(null);
    private static WeakReference<ViewGroup> youtubeOverlaysLayoutRef = new WeakReference<>(null);
    private static WeakReference<SkipSponsorButton> skipHighlightButtonRef = new WeakReference<>(null);
    private static WeakReference<SkipSponsorButton> skipSponsorButtonRef = new WeakReference<>(null);
    private static WeakReference<NewSegmentLayout> newSegmentLayoutRef = new WeakReference<>(null);
    private static boolean canShowViewElements;
    private static boolean newSegmentLayoutVisible;
    @Nullable
    private static SponsorSegment skipHighlight;
    @Nullable
    private static SponsorSegment skipSegment;
    private static final int ctaBottomMargin;
    private static final int defaultBottomMargin;
    private static final int hiddenBottomMargin;

    static {
        PlayerType.getOnChange().addObserver((PlayerType type) -> {
            playerTypeChanged(type);
            return null;
        });

        defaultBottomMargin = getDimension("brand_interaction_default_bottom_margin");
        ctaBottomMargin = getDimension("brand_interaction_cta_bottom_margin") + PlayerPatch.getQuickActionsTopMargin();
        hiddenBottomMargin = (int) Math.round((ctaBottomMargin) * 0.5);
    }

    public static Context getOverLaysViewGroupContext() {
        ViewGroup group = youtubeOverlaysLayoutRef.get();
        if (group == null) {
            return null;
        }
        return group.getContext();
    }

    /**
     * Injection point.
     */
    public static void initialize(ViewGroup viewGroup) {
        try {
            Logger.printDebug(() -> "initializing");

            // hide any old components, just in case they somehow are still hanging around
            hideAll();

            Context context = Utils.getContext();
            RelativeLayout layout = new RelativeLayout(context);
            layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            LayoutInflater.from(context).inflate(getLayoutIdentifier("revanced_sb_inline_sponsor_overlay"), layout);
            inlineSponsorOverlayRef = new WeakReference<>(layout);

            viewGroup.addView(layout);
            viewGroup.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    // ensure SB buttons and controls are always on top, otherwise the endscreen cards can cover the skip button
                    RelativeLayout layout = inlineSponsorOverlayRef.get();
                    if (layout != null) {
                        layout.bringToFront();
                    }
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                }
            });
            youtubeOverlaysLayoutRef = new WeakReference<>(viewGroup);

            skipHighlightButtonRef = new WeakReference<>(
                    Objects.requireNonNull(getChildView(layout, "revanced_sb_skip_highlight_button")));
            skipSponsorButtonRef = new WeakReference<>(
                    Objects.requireNonNull(getChildView(layout, "revanced_sb_skip_sponsor_button")));
            newSegmentLayoutRef = new WeakReference<>(
                    Objects.requireNonNull(getChildView(layout, "revanced_sb_new_segment_view")));

            newSegmentLayoutVisible = false;
            skipHighlight = null;
            skipSegment = null;
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    public static void hideAll() {
        hideSkipHighlightButton();
        hideSkipSegmentButton();
        hideNewSegmentLayout();
    }

    public static void showSkipHighlightButton(@NonNull SponsorSegment segment) {
        skipHighlight = Objects.requireNonNull(segment);
        NewSegmentLayout newSegmentLayout = newSegmentLayoutRef.get();
        // don't show highlight button if create new segment is visible
        final boolean buttonVisibility = newSegmentLayout == null || newSegmentLayout.getVisibility() != View.VISIBLE;
        updateSkipButton(skipHighlightButtonRef.get(), segment, buttonVisibility);
    }

    public static void showSkipSegmentButton(@NonNull SponsorSegment segment) {
        skipSegment = Objects.requireNonNull(segment);
        updateSkipButton(skipSponsorButtonRef.get(), segment, true);
    }

    public static void hideSkipHighlightButton() {
        skipHighlight = null;
        updateSkipButton(skipHighlightButtonRef.get(), null, false);
    }

    public static void hideSkipSegmentButton() {
        skipSegment = null;
        updateSkipButton(skipSponsorButtonRef.get(), null, false);
    }

    private static void updateSkipButton(@Nullable SkipSponsorButton button,
                                         @Nullable SponsorSegment segment, boolean visible) {
        if (button == null) {
            return;
        }
        if (segment != null) {
            button.updateSkipButtonText(segment);
        }
        setViewVisibility(button, visible);
    }

    public static void toggleNewSegmentLayoutVisibility() {
        NewSegmentLayout newSegmentLayout = newSegmentLayoutRef.get();
        if (newSegmentLayout == null) { // should never happen
            Logger.printException(() -> "toggleNewSegmentLayoutVisibility failure");
            return;
        }
        newSegmentLayoutVisible = (newSegmentLayout.getVisibility() != View.VISIBLE);
        if (skipHighlight != null) {
            setViewVisibility(skipHighlightButtonRef.get(), !newSegmentLayoutVisible);
        }
        setViewVisibility(newSegmentLayout, newSegmentLayoutVisible);
    }

    public static void hideNewSegmentLayout() {
        newSegmentLayoutVisible = false;
        setViewVisibility(newSegmentLayoutRef.get(), false);
    }

    private static void setViewVisibility(@Nullable View view, boolean visible) {
        if (view == null) {
            return;
        }
        visible &= canShowViewElements;
        final int desiredVisibility = visible ? View.VISIBLE : View.GONE;
        if (view.getVisibility() != desiredVisibility) {
            view.setVisibility(desiredVisibility);
        }
    }

    private static void playerTypeChanged(@NonNull PlayerType playerType) {
        try {
            final boolean isWatchFullScreen = playerType == PlayerType.WATCH_WHILE_FULLSCREEN;
            canShowViewElements = (isWatchFullScreen || playerType == PlayerType.WATCH_WHILE_MAXIMIZED);

            NewSegmentLayout newSegmentLayout = newSegmentLayoutRef.get();
            setNewSegmentLayoutMargins(newSegmentLayout, isWatchFullScreen);
            setViewVisibility(newSegmentLayoutRef.get(), newSegmentLayoutVisible);

            SkipSponsorButton skipHighlightButton = skipHighlightButtonRef.get();
            setSkipButtonMargins(skipHighlightButton, isWatchFullScreen);
            setViewVisibility(skipHighlightButton, skipHighlight != null);

            SkipSponsorButton skipSponsorButton = skipSponsorButtonRef.get();
            setSkipButtonMargins(skipSponsorButton, isWatchFullScreen);
            setViewVisibility(skipSponsorButton, skipSegment != null);
        } catch (Exception ex) {
            Logger.printException(() -> "Player type changed failure", ex);
        }
    }

    private static void setNewSegmentLayoutMargins(@Nullable NewSegmentLayout layout, boolean fullScreen) {
        if (layout != null) {
            setLayoutMargins(layout, fullScreen);
        }
    }

    private static void setSkipButtonMargins(@Nullable SkipSponsorButton button, boolean fullScreen) {
        if (button != null) {
            setLayoutMargins(button, fullScreen);
        }
    }

    private static void setLayoutMargins(@NonNull View view, boolean fullScreen) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        if (params == null) {
            Logger.printException(() -> "Unable to setNewSegmentLayoutMargins (params are null)");
            return;
        }
        params.bottomMargin = fullScreen ? (isFullscreenHidden() ? hiddenBottomMargin : ctaBottomMargin) : defaultBottomMargin;
        view.setLayoutParams(params);
    }

    /**
     * Injection point.
     */
    public static void endOfVideoReached() {
        try {
            Logger.printDebug(() -> "endOfVideoReached");
            // the buttons automatically set themselves to visible when appropriate,
            // but if buttons are showing when the end of the video is reached then they need
            // to be forcefully hidden
            if (!Settings.ALWAYS_REPEAT.get()) {
                CreateSegmentButtonController.hide();
                VotingButtonController.hide();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "endOfVideoReached failure", ex);
        }
    }
}
