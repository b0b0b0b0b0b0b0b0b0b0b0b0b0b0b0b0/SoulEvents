package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class TypeConfigPersistence {

    private TypeConfigPersistence() {
    }

    public static void saveTypeSettings(JavaPlugin plugin, String typeId, AirDropTypeSettings settings) {
        ConfigIds.requireSafe(typeId);
        Path typePath = plugin.getDataFolder().toPath().resolve("types").resolve(typeId + ".yml");
        settings.save(typePath);
    }

    public static String resolveOpenPermission(String typeId, String configuredPermission) {
        if (configuredPermission != null && !configuredPermission.isBlank()) {
            return configuredPermission.trim();
        }
        return "soulevents.airdrop.open." + typeId;
    }
}
