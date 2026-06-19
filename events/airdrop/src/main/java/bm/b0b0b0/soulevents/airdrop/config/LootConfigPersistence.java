package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class LootConfigPersistence {

    private LootConfigPersistence() {
    }

    public static void saveLootTable(JavaPlugin plugin, String lootTableId, LootTableSettings loot) {
        Path lootPath = plugin.getDataFolder().toPath().resolve("loot").resolve(lootTableId + ".yml");
        loot.save(lootPath);
    }
}
