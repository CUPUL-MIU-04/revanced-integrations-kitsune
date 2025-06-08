package app.kitsune.integrations.reddit.patches;

import static app.kitsune.integrations.shared.utils.StringRef.str;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import app.kitsune.integrations.reddit.settings.Settings;
import app.kitsune.integrations.shared.utils.Utils;

@SuppressWarnings("unused")
public class RemoveSubRedditDialogPatch {

    public static void confirmDialog(@NonNull TextView textView) {
        if (!Settings.REMOVE_NSFW_DIALOG.get())
            return;

        if (!textView.getText().toString().equals(str("nsfw_continue_non_anonymously")))
            return;

        clickViewDelayed(textView);
    }

    public static void dismissDialog(View cancelButtonView) {
        if (!Settings.REMOVE_NOTIFICATION_DIALOG.get())
            return;

        clickViewDelayed(cancelButtonView);
    }

    private static void clickViewDelayed(View view) {
        Utils.runOnMainThreadDelayed(() -> {
            if (view != null) {
                view.setSoundEffectsEnabled(false);
                view.performClick();
            }
        }, 0);
    }
}
