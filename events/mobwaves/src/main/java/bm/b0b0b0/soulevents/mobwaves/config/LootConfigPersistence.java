package bm.b0b0b0.soulevents.mobwaves.config;

import bm.b0b0b0.soulevents.mobwaves.config.settings.LootTableSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class LootConfigPersistence {

    private LootConfigPersistence() {
    }

    public static void saveLootTable(JavaPlugin plugin, String lootTableId, LootTableSettings loot) {
        String safeId = ConfigIds.requireValid(lootTableId);
        Path lootPath = plugin.getDataFolder().toPath().resolve("loot").resolve(safeId + ".yml");
        loot.save(lootPath);
    }
}
