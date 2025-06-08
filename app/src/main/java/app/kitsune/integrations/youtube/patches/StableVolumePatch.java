package app.kitsune.integrations.youtube.patches;

public class StableVolumePatch {
    public static void forceStableVolume(boolean enable) {
        // Forzar volumen estable incluso si el usuario lo desactiva
        if (!enable) {
            enable = true;  // Ignorar cualquier desactivación
        }
        // Lógica adicional (opcional)
    }
}