package bm.b0b0b0.soulevents.volcano.config;

import bm.b0b0b0.soulevents.volcano.config.settings.LootEntrySettings;
import bm.b0b0b0.soulevents.volcano.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoTypeSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class TypeCreatorService {

    private final JavaPlugin plugin;

    public TypeCreatorService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String createFromPreset(Preset preset) {
        String typeId = ConfigIds.requireValid(nextId(preset.prefix()));
        Path typePath = plugin.getDataFolder().toPath().resolve("types").resolve(typeId + ".yml");
        Path lootPath = plugin.getDataFolder().toPath().resolve("loot").resolve(typeId + ".yml");

        VolcanoTypeSettings settings = preset.settings(typeId);
        settings.reload(typePath);

        LootTableSettings loot = preset.loot();
        loot.reload(lootPath);
        return typeId;
    }

    private String nextId(String prefix) {
        int index = 1;
        while (plugin.getDataFolder().toPath().resolve("types").resolve(prefix + index + ".yml").toFile().exists()) {
            index++;
        }
        return prefix + index;
    }

    public enum Preset {
        COMMON("common_"),
        RARE("rare_"),
        DONATE("donate_");

        private final String prefix;

        Preset(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }

        public VolcanoTypeSettings settings(String typeId) {
            VolcanoTypeSettings settings = new VolcanoTypeSettings();
            settings.displayNameKey = "volcano.types.generated.name";
            settings.lootTableId = typeId;
            settings.broadcast.messageKey = "volcano.broadcast.default";
            if (this == RARE) {
                settings.intervalMinutes = 90L;
            }
            if (this == DONATE) {
                settings.enabled = false;
                settings.intervalMinutes = 0L;
                settings.summon.playerSummonEnabled = true;
                settings.summon.vaultCost = 1000.0;
                settings.broadcast.messageKey = "volcano.broadcast.donate";
            }
            return settings;
        }

        public LootTableSettings loot() {
            LootTableSettings loot = new LootTableSettings();
            if (this == RARE) {
                loot.entries.clear();
                loot.entries.add(LootEntrySettings.of("DIAMOND", 4, 10, 1.0));
                loot.entries.add(LootEntrySettings.of("NETHERITE_INGOT", 1, 2, 0.2));
            }
            if (this == DONATE) {
                loot.entries.clear();
                loot.entries.add(LootEntrySettings.of("ENCHANTED_GOLDEN_APPLE", 1, 3, 1.0));
            }
            return loot;
        }
    }
}
