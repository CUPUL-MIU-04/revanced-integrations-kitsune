package app.kitsune.integrations.youtube.patches.utils;

import androidx.annotation.Nullable;

import app.kitsune.integrations.youtube.shared.LockModeState;

@SuppressWarnings("unused")
public class LockModeStateHookPatch {
    /**
     * Injection point.
     */
    public static void setLockModeState(@Nullable Enum<?> lockModeState) {
        if (lockModeState == null) return;

        LockModeState.setFromString(lockModeState.name());
    }
}

