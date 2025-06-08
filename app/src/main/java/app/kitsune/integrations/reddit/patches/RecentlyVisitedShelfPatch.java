package app.kitsune.integrations.reddit.patches;

import java.util.Collections;
import java.util.List;

import app.kitsune.integrations.reddit.settings.Settings;

@SuppressWarnings("unused")
public final class RecentlyVisitedShelfPatch {

    public static List<?> hideRecentlyVisitedShelf(List<?> list) {
        return Settings.HIDE_RECENTLY_VISITED_SHELF.get() ? Collections.emptyList() : list;
    }
}
