package app.kitsune.integrations.youtube.swipecontrols.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import app.kitsune.integrations.shared.utils.ResourceUtils.ResourceType
import app.kitsune.integrations.shared.utils.ResourceUtils.getIdentifier
import app.kitsune.integrations.shared.utils.StringRef.str
import app.kitsune.integrations.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.kitsune.integrations.youtube.swipecontrols.misc.SwipeControlsOverlay
import app.kitsune.integrations.youtube.swipecontrols.misc.applyDimension
import kotlin.math.round

/**
 * main overlay layout for volume and brightness swipe controls
 *
 * @param context context to create in
 */
class SwipeControlsOverlayLayout(
    context: Context,
    private val config: SwipeControlsConfigurationProvider,
) : RelativeLayout(context), SwipeControlsOverlay {
    /**
     * DO NOT use this, for tools only
     */
    constructor(context: Context) : this(context, SwipeControlsConfigurationProvider(context))

    private val feedbackTextView: TextView
    private val autoBrightnessIcon: Drawable
    private val manualBrightnessIcon: Drawable
    private val mutedVolumeIcon: Drawable
    private val normalVolumeIcon: Drawable

    private fun getDrawable(name: String, width: Int, height: Int): Drawable {
        return resources.getDrawable(
            getIdentifier(name, ResourceType.DRAWABLE, context),
            context.theme
        ).apply {
            setTint(config.overlayForegroundColor)
            setBounds(
                0,
                0,
                width,
                height,
            )
        }
    }

    init {
        // init views
        val feedbackYTextViewPadding = 5.applyDimension(context, TypedValue.COMPLEX_UNIT_DIP)
        val feedbackXTextViewPadding = 12.applyDimension(context, TypedValue.COMPLEX_UNIT_DIP)
        val compoundIconPadding = 4.applyDimension(context, TypedValue.COMPLEX_UNIT_DIP)
        feedbackTextView = TextView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(CENTER_IN_PARENT, TRUE)
                setPadding(
                    feedbackXTextViewPadding,
                    feedbackYTextViewPadding,
                    feedbackXTextViewPadding,
                    feedbackYTextViewPadding
                )
            }
            background = GradientDrawable().apply {
                cornerRadius = 30f
                setColor(config.overlayTextBackgroundColor)
            }
            setTextColor(config.overlayForegroundColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, config.overlayTextSize.toFloat())
            compoundDrawablePadding = compoundIconPadding
            visibility = GONE
        }
        addView(feedbackTextView)

        // get icons scaled, assuming square icons
        val iconHeight = round(feedbackTextView.lineHeight * .8).toInt()
        autoBrightnessIcon = getDrawable("ic_sc_brightness_auto", iconHeight, iconHeight)
        manualBrightnessIcon = getDrawable("ic_sc_brightness_manual", iconHeight, iconHeight)
        mutedVolumeIcon = getDrawable("ic_sc_volume_mute", iconHeight, iconHeight)
        normalVolumeIcon = getDrawable("ic_sc_volume_normal", iconHeight, iconHeight)
    }

    private val feedbackHideHandler = Handler(Looper.getMainLooper())
    private val feedbackHideCallback = Runnable {
        feedbackTextView.visibility = View.GONE
    }

    /**
     * show the feedback view for a given time
     *
     * @param message the message to show
     * @param icon the icon to use
     */
    private fun showFeedbackView(message: String, icon: Drawable) {
        feedbackHideHandler.removeCallbacks(feedbackHideCallback)
        feedbackHideHandler.postDelayed(feedbackHideCallback, config.overlayShowTimeoutMillis)
        feedbackTextView.apply {
            text = message
            setCompoundDrawablesRelative(
                icon,
                null,
                null,
                null,
            )
            visibility = VISIBLE
        }
    }

    override fun onVolumeChanged(newVolume: Int, maximumVolume: Int) {
        showFeedbackView(
            "$newVolume",
            if (newVolume > 0) normalVolumeIcon else mutedVolumeIcon,
        )
    }

    override fun onBrightnessChanged(brightness: Double) {
        if (config.shouldLowestValueEnableAutoBrightness && brightness <= 0) {
            showFeedbackView(
                str("revanced_swipe_lowest_value_auto_brightness_overlay_text"),
                autoBrightnessIcon,
            )
        } else if (brightness >= 0) {
            showFeedbackView("${round(brightness).toInt()}%", manualBrightnessIcon)
        }
    }

    @Suppress("DEPRECATION")
    override fun onEnterSwipeSession() {
        if (config.shouldEnableHapticFeedback) {
            performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
            )
        }
    }
}
