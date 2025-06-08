package app.kitsune.integrations.youtube.patches.overlaybutton;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.youtube.settings.Settings;
import app.kitsune.integrations.youtube.settings.preference.WhitelistedChannelsPreference;
import app.kitsune.integrations.youtube.whitelist.Whitelist;

@SuppressWarnings("unused")
public class Whitelists extends BottomControlButton {
    @Nullable
    private static Whitelists instance;

    public Whitelists(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "whitelist_button",
                Settings.OVERLAY_BUTTON_WHITELIST,
                view -> Whitelist.showWhitelistDialog(view.getContext()),
                view -> {
                    WhitelistedChannelsPreference.showWhitelistedChannelDialog(view.getContext());
                    return true;
                }
        );
    }

    /**
     * Injection point.
     */
    public static void initialize(View bottomControlsViewGroup) {
        try {
            if (bottomControlsViewGroup instanceof ViewGroup viewGroup) {
                instance = new Whitelists(viewGroup);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void changeVisibility(boolean showing, boolean animation) {
        if (instance != null) instance.setVisibility(showing, animation);
    }

    public static void changeVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }

}