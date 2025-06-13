package app.kitsune.integrations.music.patches.components;

import app.kitsune.integrations.music.settings.Settings;
import app.kitsune.integrations.shared.patches.components.Filter;
import app.kitsune.integrations.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class AdsFilter extends Filter {

    public AdsFilter() {
        final StringFilterGroup alertBannerPromo = new StringFilterGroup(
                Settings.HIDE_PROMOTION_ALERT_BANNER,
                "alert_banner_promo.eml"
        );

        final StringFilterGroup paidPromotionLabel = new StringFilterGroup(
                Settings.HIDE_PAID_PROMOTION_LABEL,
                "music_paid_content_overlay.eml"
        );

        addIdentifierCallbacks(alertBannerPromo, paidPromotionLabel);

        final StringFilterGroup statementBanner = new StringFilterGroup(
                Settings.HIDE_GENERAL_ADS,
                "statement_banner"
        );

        addPathCallbacks(statementBanner);

    }
}
