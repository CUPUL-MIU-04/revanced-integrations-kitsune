package app.kitsune.integrations.music.patches.ads;

import static app.kitsune.integrations.shared.utils.StringRef.str;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.kitsune.integrations.music.settings.Settings;
import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.shared.utils.Utils;

@SuppressWarnings("unused")
public class PremiumRenewalPatch {

    public static void hidePremiumRenewal(LinearLayout buttonContainerView) {
        if (!Settings.HIDE_PREMIUM_RENEWAL.get())
            return;

        buttonContainerView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            try {
                Utils.runOnMainThreadDelayed(() -> {
                            if (!(buttonContainerView.getChildAt(0) instanceof ViewGroup closeButtonParentView))
                                return;
                            if (!(closeButtonParentView.getChildAt(0) instanceof TextView closeButtonView))
                                return;
                            if (closeButtonView.getText().toString().equals(str("dialog_got_it_text")))
                                Utils.clickView(closeButtonView);
                            else
                                Utils.hideViewByLayoutParams((View) buttonContainerView.getParent());
                        }, 0
                );
            } catch (Exception ex) {
                Logger.printException(() -> "hidePremiumRenewal failure", ex);
            }
        });
    }
}
