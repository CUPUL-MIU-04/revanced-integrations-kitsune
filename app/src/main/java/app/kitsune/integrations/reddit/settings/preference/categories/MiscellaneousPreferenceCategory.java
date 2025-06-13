package app.kitsune.integrations.reddit.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.kitsune.integrations.reddit.settings.Settings;
import app.kitsune.integrations.reddit.settings.SettingsStatus;
import app.kitsune.integrations.reddit.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class MiscellaneousPreferenceCategory extends ConditionalPreferenceCategory {
    public MiscellaneousPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Miscellaneous");
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.miscellaneousCategoryEnabled();
    }

    @Override
    public void addPreferences(Context context) {
        if (SettingsStatus.openLinksDirectlyEnabled) {
            addPreference(new TogglePreference(
                    context,
                    "Open links directly",
                    "Skips over redirection URLs in external links.",
                    Settings.OPEN_LINKS_DIRECTLY
            ));
        }
        if (SettingsStatus.openLinksExternallyEnabled) {
            addPreference(new TogglePreference(
                    context,
                    "Open links externally",
                    "Opens links in your browser instead of in the in-app-browser.",
                    Settings.OPEN_LINKS_EXTERNALLY
            ));
        }
        if (SettingsStatus.sanitizeUrlQueryEnabled) {
            addPreference(new TogglePreference(
                    context,
                    "Sanitize sharing links",
                    "Removes tracking query parameters from URLs when sharing links.",
                    Settings.SANITIZE_URL_QUERY
            ));
        }
    }
}
