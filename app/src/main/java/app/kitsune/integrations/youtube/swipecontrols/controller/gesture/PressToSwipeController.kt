package app.kitsune.integrations.youtube.swipecontrols.controller.gesture

import android.view.MotionEvent
import app.kitsune.integrations.youtube.patches.swipe.SwipeControlsPatch.isEngagementOverlayVisible
import app.kitsune.integrations.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.kitsune.integrations.youtube.swipecontrols.SwipeControlsHostActivity
import app.kitsune.integrations.youtube.swipecontrols.controller.gesture.core.BaseGestureController
import app.kitsune.integrations.youtube.swipecontrols.controller.gesture.core.SwipeDetector
import app.kitsune.integrations.youtube.swipecontrols.misc.contains
import app.kitsune.integrations.youtube.swipecontrols.misc.toPoint

/**
 * provides the press-to-swipe (PtS) swipe controls experience
 *
 * @param controller reference to the main swipe controller
 */
class PressToSwipeController(
    private val controller: SwipeControlsHostActivity,
    private val config: SwipeControlsConfigurationProvider,
) : BaseGestureController(controller) {
    /**
     * monitors if the user is currently in a swipe session.
     */
    private var isInSwipeSession = false

    override val shouldForceInterceptEvents: Boolean
        get() = currentSwipe == SwipeDetector.SwipeDirection.VERTICAL && isInSwipeSession

    override fun shouldDropMotion(motionEvent: MotionEvent): Boolean = false

    override fun isInSwipeZone(motionEvent: MotionEvent): Boolean {
        val inVolumeZone = if (controller.config.enableVolumeControls) {
            (motionEvent.toPoint() in controller.zones.volume)
        } else {
            false
        }
        val inBrightnessZone = if (controller.config.enableBrightnessControl) {
            (motionEvent.toPoint() in controller.zones.brightness)
        } else {
            false
        }

        return inVolumeZone || inBrightnessZone
    }

    override fun onUp(motionEvent: MotionEvent) {
        super.onUp(motionEvent)
        isInSwipeSession = false
    }

    override fun onLongPress(motionEvent: MotionEvent) {
        // enter swipe session with feedback
        isInSwipeSession = true
        controller.overlay.onEnterSwipeSession()

        // send GestureDetector a ACTION_CANCEL event so it will handle further events
        motionEvent.action = MotionEvent.ACTION_CANCEL
        detector.onTouchEvent(motionEvent)
    }

    override fun onSwipe(
        from: MotionEvent,
        to: MotionEvent,
        distanceX: Double,
        distanceY: Double,
    ): Boolean {
        // cancel if locked
        if (!config.enableSwipeControlsLockMode && config.isScreenLocked)
            return false
        // cancel if not in swipe session or vertical
        if (!isInSwipeSession || currentSwipe != SwipeDetector.SwipeDirection.VERTICAL)
            return false
        // ignore gestures when engagement overlay is visible
        if (isEngagementOverlayVisible())
            return false
        return when (from.toPoint()) {
            in controller.zones.volume -> {
                scrollVolume(distanceY)
                true
            }

            in controller.zones.brightness -> {
                scrollBrightness(distanceY)
                true
            }

            else -> false
        }
    }
}
