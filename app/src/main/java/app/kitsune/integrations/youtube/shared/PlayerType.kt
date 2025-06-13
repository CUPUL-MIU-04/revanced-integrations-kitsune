package app.kitsune.integrations.youtube.shared

import app.kitsune.integrations.shared.utils.Event
import app.kitsune.integrations.shared.utils.Logger

/**
 * WatchWhile player type
 */
enum class PlayerType {
    /**
     * Either no video, or a Short is playing.
     */
    NONE,

    /**
     * A Short is playing. Occurs if a regular video is first opened
     * and then a Short is opened (without first closing the regular video).
     */
    HIDDEN,

    /**
     * A regular video is minimized.
     *
     * When spoofing to 16.x YouTube and watching a short with a regular video in the background,
     * the type can be this (and not [HIDDEN]).
     */
    WATCH_WHILE_MINIMIZED,
    WATCH_WHILE_MAXIMIZED,
    WATCH_WHILE_FULLSCREEN,
    WATCH_WHILE_SLIDING_MAXIMIZED_FULLSCREEN,
    WATCH_WHILE_SLIDING_MINIMIZED_MAXIMIZED,

    /**
     * Player is either sliding to [HIDDEN] state because a Short was opened while a regular video is on screen.
     * OR
     * The user has swiped a minimized player away to be closed (and no Short is being opened).
     */
    WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED,
    WATCH_WHILE_SLIDING_FULLSCREEN_DISMISSED,

    /**
     * Home feed video playback.
     */
    INLINE_MINIMAL,
    VIRTUAL_REALITY_FULLSCREEN,
    WATCH_WHILE_PICTURE_IN_PICTURE;

    companion object {

        private val nameToPlayerType = entries.associateBy { it.name }

        @JvmStatic
        fun setFromString(enumName: String) {
            val newType = nameToPlayerType[enumName]
            if (newType == null) {
                Logger.printException { "Unknown PlayerType encountered: $enumName" }
            } else if (current != newType) {
                Logger.printDebug { "PlayerType changed to: $newType" }
                current = newType
            }
        }

        /**
         * The current player type.
         */
        @JvmStatic
        var current
            get() = currentPlayerType
            private set(value) {
                currentPlayerType = value
                onChange(value)
            }

        @Volatile // value is read/write from different threads
        private var currentPlayerType = NONE

        /**
         * player type change listener
         */
        @JvmStatic
        val onChange = Event<PlayerType>()
    }

    /**
     * Check if the current player type is [NONE] or [HIDDEN].
     * Useful to check if a short is currently playing.
     *
     * Does not include the first moment after a short is opened when a regular video is minimized on screen,
     * or while watching a short with a regular video present on a spoofed 16.x version of YouTube.
     * To include those situations instead use [isNoneHiddenOrMinimized].
     */
    fun isNoneOrHidden(): Boolean {
        return this == NONE || this == HIDDEN
    }

    /**
     * Check if the current player type is
     * [NONE], [HIDDEN], [WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED].
     *
     * Useful to check if a Short is being played or opened.
     *
     * Usually covers all use cases with no false positives, except if called from some hooks
     * when spoofing to an old version this will return false even
     * though a Short is being opened or is on screen (see [isNoneHiddenOrMinimized]).
     *
     * @return If nothing, a Short, or a regular video is sliding off screen to a dismissed or hidden state.
     */
    fun isNoneHiddenOrSlidingMinimized(): Boolean {
        return isNoneOrHidden() || this == WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED
    }

    /**
     * Check if the current player type is
     * [NONE], [HIDDEN], [WATCH_WHILE_MINIMIZED], [WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED].
     *
     * Useful to check if a Short is being played,
     * although will return false positive if a regular video is
     * opened and minimized (and a Short is not playing or being opened).
     *
     * Typically used to detect if a Short is playing when the player cannot be in a minimized state,
     * such as the user interacting with a button or element of the player.
     *
     * @return If nothing, a Short, a regular video is sliding off screen to a dismissed or hidden state,
     *         a regular video is minimized (and a new video is not being opened).
     */
    fun isNoneHiddenOrMinimized(): Boolean {
        return isNoneHiddenOrSlidingMinimized() || this == WATCH_WHILE_MINIMIZED
    }

    /**
     * Check if the current player type is
     * [WATCH_WHILE_MAXIMIZED], [WATCH_WHILE_FULLSCREEN].
     *
     * Useful to check if a regular video is being played.
     */
    fun isMaximizedOrFullscreen(): Boolean {
        return this == WATCH_WHILE_MAXIMIZED || this == WATCH_WHILE_FULLSCREEN
    }

    /**
     * Check if the current player type is
     * [WATCH_WHILE_FULLSCREEN], [WATCH_WHILE_SLIDING_MAXIMIZED_FULLSCREEN].
     *
     * Useful to check if a video is fullscreen.
     */
    fun isFullScreenOrSlidingFullScreen(): Boolean {
        return this == WATCH_WHILE_FULLSCREEN || this == WATCH_WHILE_SLIDING_MAXIMIZED_FULLSCREEN
    }

}