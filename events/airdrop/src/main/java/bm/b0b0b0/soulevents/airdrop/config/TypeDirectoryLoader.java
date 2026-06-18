package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootEntrySettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class TypeDirectoryLoader {

    private static final List<String> DEFAULT_TYPE_IDS = List.of("default", "rare", "donate");

    private TypeDirectoryLoader() {
    }

    public static Map<String, AirDropTypeDefinition> load(JavaPlugin plugin) {
        Path typesDir = plugin.getDataFolder().toPath().resolve("types");
        Path lootDir = plugin.getDataFolder().toPath().resolve("loot");
        try {
            Files.createDirectories(typesDir);
            Files.createDirectories(lootDir);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to create types/loot folders: " + exception.getMessage());
            return Map.of();
        }
        ensureDefaults(plugin, typesDir, lootDir);
        Map<String, AirDropTypeDefinition> types = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(typesDir)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .forEach(path -> loadType(plugin, path, lootDir, types));
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to load types: " + exception.getMessage());
        }
        return Map.copyOf(types);
    }

    private static void ensureDefaults(JavaPlugin plugin, Path typesDir, Path lootDir) {
        for (String typeId : DEFAULT_TYPE_IDS) {
            Path typePath = typesDir.resolve(typeId + ".yml");
            if (!Files.exists(typePath)) {
                AirDropTypeSettings settings = defaultTypeSettings(typeId);
                settings.reload(typePath);
            }
            Path lootPath = lootDir.resolve(typeId + ".yml");
            if (!Files.exists(lootPath)) {
                LootTableSettings loot = defaultLootSettings(typeId);
                loot.reload(lootPath);
            }
        }
    }

    private static AirDropTypeSettings defaultTypeSettings(String typeId) {
        AirDropTypeSettings settings = new AirDropTypeSettings();
        settings.displayNameKey = "airdrop.types." + typeId + ".name";
        settings.broadcast.messageKey = "airdrop.broadcast." + typeId;
        switch (typeId) {
            case "rare" -> {
                settings.intervalMinutes = 90L;
                settings.preOpenBeacon.enabled = true;
                settings.randomSpawn.minRadiusFromCenter = 800;
                settings.randomSpawn.maxRadiusFromCenter = 8000;
            }
            case "donate" -> {
                settings.enabled = false;
                settings.intervalMinutes = 0L;
                settings.summon.playerSummonEnabled = true;
                settings.summon.vaultCost = 1000.0;
                settings.requiredLoot.enabled = true;
            }
            default -> {
            }
        }
        return settings;
    }

    private static LootTableSettings defaultLootSettings(String typeId) {
        LootTableSettings loot = new LootTableSettings();
        if ("rare".equals(typeId)) {
            loot.chestMaterial = "ENDER_CHEST";
            loot.entries.clear();
            loot.entries.add(LootEntrySettings.of("DIAMOND", 3, 8, 1.0));
            loot.entries.add(LootEntrySettings.of("NETHERITE_INGOT", 1, 2, 0.15));
        }
        if ("donate".equals(typeId)) {
            loot.chestMaterial = "ENDER_CHEST";
            loot.entries.clear();
            loot.entries.add(LootEntrySettings.of("ENCHANTED_GOLDEN_APPLE", 1, 3, 1.0));
        }
        return loot;
    }

    private static void loadType(JavaPlugin plugin, Path typePath, Path lootDir, Map<String, AirDropTypeDefinition> types) {
        String typeId = stripExtension(typePath.getFileName().toString());
        AirDropTypeSettings settings = new AirDropTypeSettings();
        settings.reload(typePath);
        String lootId = settings.lootTableId == null || settings.lootTableId.isEmpty() ? typeId : settings.lootTableId;
        Path lootPath = lootDir.resolve(lootId + ".yml");
        LootTableSettings loot = new LootTableSettings();
        if (Files.exists(lootPath)) {
            loot.reload(lootPath);
        } else {
            loot.reload(lootDir.resolve(typeId + ".yml"));
        }
        types.put(typeId, new AirDropTypeDefinition(typeId, settings, loot));
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - 4);
    }
}
