package app.kitsune.integrations.music.settings.preference;

import static app.kitsune.integrations.music.utils.ExtendedUtils.getDialogBuilder;
import static app.kitsune.integrations.music.utils.ExtendedUtils.getLayoutParams;
import static app.kitsune.integrations.shared.utils.StringRef.str;

import android.app.Activity;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;

import app.kitsune.integrations.shared.settings.Setting;
import app.kitsune.integrations.shared.utils.Logger;

public class ResettableEditTextPreference {

    public static void showDialog(Activity mActivity, @NonNull Setting<String> setting) {
        try {
            final EditText textView = new EditText(mActivity);
            textView.setText(setting.get());

            TextInputLayout textInputLayout = new TextInputLayout(mActivity);
            textInputLayout.setLayoutParams(getLayoutParams());
            textInputLayout.addView(textView);

            FrameLayout container = new FrameLayout(mActivity);
            container.addView(textInputLayout);

            getDialogBuilder(mActivity)
                    .setTitle(str(setting.key + "_title"))
                    .setView(container)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(str("revanced_extended_settings_reset"), (dialog, which) -> {
                        setting.resetToDefault();
                        ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        setting.save(textView.getText().toString().trim());
                        ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }

}
