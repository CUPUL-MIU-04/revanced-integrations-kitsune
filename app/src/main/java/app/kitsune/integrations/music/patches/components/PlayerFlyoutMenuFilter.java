package app.kitsune.integrations.music.patches.components;

import app.kitsune.integrations.music.settings.Settings;
import app.kitsune.integrations.shared.patches.components.Filter;
import app.kitsune.integrations.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class PlayerFlyoutMenuFilter extends Filter {

    public PlayerFlyoutMenuFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_3_COLUMN_COMPONENT,
                        "music_highlight_menu_item_carousel.eml",
                        "tile_button_carousel.eml"
                )
        );
    }
}
