package bm.b0b0b0.soulevents.volcano.config;

import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoTypeSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class TypeConfigPersistence {

    private TypeConfigPersistence() {
    }

    public static void saveTypeSettings(JavaPlugin plugin, String typeId, VolcanoTypeSettings settings) {
        String safeId = ConfigIds.requireValid(typeId);
        Path typePath = plugin.getDataFolder().toPath().resolve("types").resolve(safeId + ".yml");
        settings.save(typePath);
    }

    public static String resolveOpenPermission(String typeId, String configuredPermission) {
        if (configuredPermission != null && !configuredPermission.isBlank()) {
            return configuredPermission.trim();
        }
        return "soulevents.volcano.open." + typeId;
    }
}

