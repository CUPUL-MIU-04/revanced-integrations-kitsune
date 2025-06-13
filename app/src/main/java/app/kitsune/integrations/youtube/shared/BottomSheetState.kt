package app.kitsune.integrations.youtube.shared

import app.kitsune.integrations.shared.utils.Event
import app.kitsune.integrations.shared.utils.Logger

/**
 * BottomSheetState bottom sheet state.
 */
enum class BottomSheetState {
    CLOSED,
    OPEN;

    companion object {

        @JvmStatic
        fun set(enum: BottomSheetState) {
            if (current != enum) {
                Logger.printDebug { "BottomSheetState changed to: ${enum.name}" }
                current = enum
            }
        }

        /**
         * The current bottom sheet state.
         */
        @JvmStatic
        var current
            get() = currentBottomSheetState
            private set(value) {
                currentBottomSheetState = value
                onChange(currentBottomSheetState)
            }

        @Volatile // value is read/write from different threads
        private var currentBottomSheetState = CLOSED

        /**
         * bottom sheet state change listener
         */
        @JvmStatic
        val onChange = Event<BottomSheetState>()
    }

    /**
     * Check if the bottom sheet is [OPEN].
     * Useful for checking if a bottom sheet is open.
     */
    fun isOpen(): Boolean {
        return this == OPEN
    }
}