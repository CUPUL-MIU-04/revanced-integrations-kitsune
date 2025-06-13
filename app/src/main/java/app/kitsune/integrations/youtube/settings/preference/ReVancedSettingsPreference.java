package app.kitsune.integrations.youtube.settings.preference;

import static app.kitsune.integrations.shared.utils.StringRef.str;
import static app.kitsune.integrations.shared.utils.Utils.isSDKAbove;
import static app.kitsune.integrations.youtube.patches.general.MiniplayerPatch.MiniplayerType.MODERN_1;
import static app.kitsune.integrations.youtube.patches.general.MiniplayerPatch.MiniplayerType.MODERN_3;
import static app.kitsune.integrations.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

import android.preference.Preference;
import android.preference.SwitchPreference;

import app.kitsune.integrations.shared.settings.Setting;
import app.kitsune.integrations.youtube.patches.general.LayoutSwitchPatch;
import app.kitsune.integrations.youtube.patches.general.MiniplayerPatch;
import app.kitsune.integrations.youtube.patches.utils.PatchStatus;
import app.kitsune.integrations.youtube.patches.utils.ReturnYouTubeDislikePatch;
import app.kitsune.integrations.youtube.returnyoutubedislike.ReturnYouTubeDislike;
import app.kitsune.integrations.youtube.settings.Settings;
import app.kitsune.integrations.youtube.utils.ExtendedUtils;

@SuppressWarnings("deprecation")
public class ReVancedSettingsPreference extends ReVancedPreferenceFragment {

    private static void enableDisablePreferences() {
        for (Setting<?> setting : Setting.allLoadedSettings()) {
            final Preference preference = mPreferenceManager.findPreference(setting.key);
            if (preference != null) {
                preference.setEnabled(setting.isAvailable());
            }
        }
    }

    private static void enableDisablePreferences(final boolean isAvailable, final Setting<?>... unavailableEnum) {
        if (!isAvailable) {
            return;
        }
        for (Setting<?> setting : unavailableEnum) {
            final Preference preference = mPreferenceManager.findPreference(setting.key);
            if (preference != null) {
                preference.setEnabled(false);
            }
        }
    }

    public static void initializeReVancedSettings() {
        enableDisablePreferences();

        AmbientModePreferenceLinks();
        ChangeHeaderPreferenceLinks();
        ExternalDownloaderPreferenceLinks();
        FullScreenPanelPreferenceLinks();
        LayoutOverrideLinks();
        MiniPlayerPreferenceLinks();
        NavigationPreferenceLinks();
        RYDPreferenceLinks();
        SeekBarPreferenceLinks();
        SpeedOverlayPreferenceLinks();
        QuickActionsPreferenceLinks();
        TabletLayoutLinks();
        WhitelistPreferenceLinks();
    }

    /**
     * Enable/Disable Preference related to Ambient Mode
     */
    private static void AmbientModePreferenceLinks() {
        enableDisablePreferences(
                Settings.DISABLE_AMBIENT_MODE.get(),
                Settings.BYPASS_AMBIENT_MODE_RESTRICTIONS,
                Settings.DISABLE_AMBIENT_MODE_IN_FULLSCREEN
        );
    }

    /**
     * Enable/Disable Preference related to Change header
     */
    private static void ChangeHeaderPreferenceLinks() {
        enableDisablePreferences(
                PatchStatus.MinimalHeader(),
                Settings.CHANGE_YOUTUBE_HEADER
        );
    }

    /**
     * Enable/Disable Preference for External downloader settings
     */
    private static void ExternalDownloaderPreferenceLinks() {
        // Override download button will not work if spoofed with YouTube 18.24.xx or earlier.
        enableDisablePreferences(
                isSpoofingToLessThan("18.24.00"),
                Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON,
                Settings.OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON
        );
    }

    /**
     * Enable/Disable Layout Override Preference
     */
    private static void LayoutOverrideLinks() {
        enableDisablePreferences(
                ExtendedUtils.isTablet(),
                Settings.FORCE_FULLSCREEN
        );
    }

    /**
     * Enable/Disable Preferences not working in tablet layout
     */
    private static void TabletLayoutLinks() {
        final boolean isTablet = ExtendedUtils.isTablet() &&
                !LayoutSwitchPatch.phoneLayoutEnabled();

        enableDisablePreferences(
                isTablet,
                Settings.DISABLE_ENGAGEMENT_PANEL,
                Settings.HIDE_COMMUNITY_POSTS_HOME_RELATED_VIDEOS,
                Settings.HIDE_COMMUNITY_POSTS_SUBSCRIPTIONS,
                Settings.HIDE_MIX_PLAYLISTS,
                Settings.HIDE_RELATED_VIDEO_OVERLAY,
                Settings.SHOW_VIDEO_TITLE_SECTION
        );
    }

    /**
     * Enable/Disable Preference related to Fullscreen Panel
     */
    private static void FullScreenPanelPreferenceLinks() {
        enableDisablePreferences(
                Settings.DISABLE_ENGAGEMENT_PANEL.get(),
                Settings.HIDE_RELATED_VIDEO_OVERLAY,
                Settings.HIDE_QUICK_ACTIONS,
                Settings.HIDE_QUICK_ACTIONS_COMMENT_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_DISLIKE_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_LIKE_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_LIVE_CHAT_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_MORE_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_OPEN_MIX_PLAYLIST_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_OPEN_PLAYLIST_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_SAVE_TO_PLAYLIST_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_SHARE_BUTTON
        );

        enableDisablePreferences(
                Settings.DISABLE_LANDSCAPE_MODE.get(),
                Settings.FORCE_FULLSCREEN
        );

        enableDisablePreferences(
                Settings.FORCE_FULLSCREEN.get(),
                Settings.DISABLE_LANDSCAPE_MODE
        );

    }

    /**
     * Enable/Disable Preference related to Hide Quick Actions
     */
    private static void QuickActionsPreferenceLinks() {
        final boolean isEnabled =
                Settings.DISABLE_ENGAGEMENT_PANEL.get() || Settings.HIDE_QUICK_ACTIONS.get();

        enableDisablePreferences(
                isEnabled,
                Settings.HIDE_QUICK_ACTIONS_COMMENT_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_DISLIKE_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_LIKE_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_LIVE_CHAT_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_MORE_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_OPEN_MIX_PLAYLIST_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_OPEN_PLAYLIST_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_SAVE_TO_PLAYLIST_BUTTON,
                Settings.HIDE_QUICK_ACTIONS_SHARE_BUTTON
        );
    }

    /**
     * Enable/Disable Preference related to Miniplayer settings
     */
    private static void MiniPlayerPreferenceLinks() {
        final MiniplayerPatch.MiniplayerType CURRENT_TYPE = Settings.MINIPLAYER_TYPE.get();
        final boolean available =
                (CURRENT_TYPE == MODERN_1 || CURRENT_TYPE == MODERN_3) &&
                        !Settings.MINIPLAYER_DOUBLE_TAP_ACTION.get() &&
                        !Settings.MINIPLAYER_DRAG_AND_DROP.get();

        enableDisablePreferences(
                !available,
                Settings.MINIPLAYER_HIDE_EXPAND_CLOSE
        );
    }

    /**
     * Enable/Disable Preference related to Navigation settings
     */
    private static void NavigationPreferenceLinks() {
        enableDisablePreferences(
                Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON.get(),
                Settings.HIDE_NAVIGATION_CREATE_BUTTON
        );
        enableDisablePreferences(
                !Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON.get(),
                Settings.HIDE_NAVIGATION_NOTIFICATIONS_BUTTON,
                Settings.REPLACE_TOOLBAR_CREATE_BUTTON,
                Settings.REPLACE_TOOLBAR_CREATE_BUTTON_TYPE
        );
        enableDisablePreferences(
                !isSDKAbove(31),
                Settings.ENABLE_TRANSLUCENT_NAVIGATION_BAR
        );
    }

    /**
     * Enable/Disable Preference related to RYD settings
     */
    private static void RYDPreferenceLinks() {
        if (!(mPreferenceManager.findPreference(Settings.RYD_ENABLED.key) instanceof SwitchPreference enabledPreference)) {
            return;
        }
        if (!(mPreferenceManager.findPreference(Settings.RYD_SHORTS.key) instanceof SwitchPreference shortsPreference)) {
            return;
        }
        if (!(mPreferenceManager.findPreference(Settings.RYD_DISLIKE_PERCENTAGE.key) instanceof SwitchPreference percentagePreference)) {
            return;
        }
        if (!(mPreferenceManager.findPreference(Settings.RYD_COMPACT_LAYOUT.key) instanceof SwitchPreference compactLayoutPreference)) {
            return;
        }
        final Preference.OnPreferenceChangeListener clearAllUICaches = (pref, newValue) -> {
            ReturnYouTubeDislike.clearAllUICaches();

            return true;
        };
        enabledPreference.setOnPreferenceChangeListener((pref, newValue) -> {
            ReturnYouTubeDislikePatch.onRYDStatusChange();

            return true;
        });
        String shortsSummary = ReturnYouTubeDislikePatch.IS_SPOOFING_TO_NON_LITHO_SHORTS_PLAYER
                ? str("revanced_ryd_shorts_summary_on")
                : str("revanced_ryd_shorts_summary_on_disclaimer");
        shortsPreference.setSummaryOn(shortsSummary);
        percentagePreference.setOnPreferenceChangeListener(clearAllUICaches);
        compactLayoutPreference.setOnPreferenceChangeListener(clearAllUICaches);
    }

    /**
     * Enable/Disable Preference related to Seek bar settings
     */
    private static void SeekBarPreferenceLinks() {
        enableDisablePreferences(
                Settings.RESTORE_OLD_SEEKBAR_THUMBNAILS.get(),
                Settings.ENABLE_SEEKBAR_THUMBNAILS_HIGH_QUALITY
        );
    }

    /**
     * Enable/Disable Preference related to Speed overlay settings
     */
    private static void SpeedOverlayPreferenceLinks() {
        enableDisablePreferences(
                Settings.DISABLE_SPEED_OVERLAY.get(),
                Settings.SPEED_OVERLAY_VALUE
        );
    }

    private static void WhitelistPreferenceLinks() {
        final boolean enabled = PatchStatus.RememberPlaybackSpeed() || PatchStatus.SponsorBlock();
        final String[] whitelistKey = {Settings.OVERLAY_BUTTON_WHITELIST.key, "revanced_whitelist_settings"};

        for (String key : whitelistKey) {
            final Preference preference = mPreferenceManager.findPreference(key);
            if (preference != null) {
                preference.setEnabled(enabled);
            }
        }
    }
}
