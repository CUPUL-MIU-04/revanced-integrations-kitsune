package app.kitsune.integrations.youtube.patches.overlaybutton;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.youtube.settings.Settings;

@SuppressWarnings("unused")
public class AlwaysRepeat extends BottomControlButton {
    @Nullable
    private static AlwaysRepeat instance;

    public AlwaysRepeat(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "always_repeat_button",
                Settings.OVERLAY_BUTTON_ALWAYS_REPEAT,
                Settings.ALWAYS_REPEAT,
                Settings.ALWAYS_REPEAT_PAUSE,
                view -> {
                    if (instance != null)
                        instance.changeSelected(!view.isSelected());
                },
                view -> {
                    if (instance != null)
                        instance.changeColorFilter();
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
                instance = new AlwaysRepeat(viewGroup);
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