package app.kitsune.integrations.reddit.settings;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import app.kitsune.integrations.reddit.settings.preference.ReVancedPreferenceFragment;

/**
 * @noinspection ALL
 */
public class ActivityHook {
    public static void initialize(Activity activity) {
        SettingsStatus.load();

        final int fragmentId = View.generateViewId();
        final FrameLayout fragment = new FrameLayout(activity);
        fragment.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        fragment.setId(fragmentId);

        final LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setFitsSystemWindows(true);
        linearLayout.setTransitionGroup(true);
        linearLayout.addView(fragment);
        activity.setContentView(linearLayout);

        activity.getFragmentManager()
                .beginTransaction()
                .replace(fragmentId, new ReVancedPreferenceFragment())
                .commit();
    }
}
