package bm.b0b0b0.soulevents.mobwaves.config;

import bm.b0b0b0.soulevents.mobwaves.config.settings.LootEntrySettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeTypeSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class TypeDirectoryLoader {

    private static final List<String> DEFAULT_TYPE_IDS = List.of("default", "elite");

    private TypeDirectoryLoader() {
    }

    public static Map<String, HordeTypeDefinition> load(JavaPlugin plugin) {
        Path typesDir = plugin.getDataFolder().toPath().resolve("types");
        Path lootDir = plugin.getDataFolder().toPath().resolve("loot");
        try {
            Files.createDirectories(typesDir);
            Files.createDirectories(lootDir);
        } catch (IOException exception) {
            plugin.getLogger().severe("MobWaves types/loot folders failed: " + exception.getMessage());
            return Map.of();
        }
        ensureDefaults(plugin, typesDir, lootDir);
        Map<String, HordeTypeDefinition> types = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(typesDir)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(path -> loadType(plugin, path, lootDir, types));
        } catch (IOException exception) {
            plugin.getLogger().severe("MobWaves types load failed: " + exception.getMessage());
        }
        return Map.copyOf(types);
    }

    private static void ensureDefaults(JavaPlugin plugin, Path typesDir, Path lootDir) {
        for (String typeId : DEFAULT_TYPE_IDS) {
            Path typePath = typesDir.resolve(typeId + ".yml");
            if (!Files.exists(typePath)) {
                HordeTypeSettings settings = defaultTypeSettings(typeId);
                settings.reload(typePath);
            }
            Path lootPath = lootDir.resolve(typeId + ".yml");
            if (!Files.exists(lootPath)) {
                LootTableSettings loot = defaultLootSettings(typeId);
                loot.reload(lootPath);
            }
        }
    }

    private static HordeTypeSettings defaultTypeSettings(String typeId) {
        HordeTypeSettings settings = new HordeTypeSettings();
        settings.displayNameKey = "mobwaves.types." + typeId + ".name";
        if ("elite".equals(typeId)) {
            settings.intervalMinutes = 120L;
            settings.waveProfileId = "default";
            settings.randomSpawn.minRadiusFromCenter = 800;
        }
        return settings;
    }

    private static LootTableSettings defaultLootSettings(String typeId) {
        LootTableSettings loot = new LootTableSettings();
        if ("elite".equals(typeId)) {
            loot.entries.clear();
            loot.entries.add(LootEntrySettings.of("DIAMOND", 1, 3, 1.0));
            loot.entries.add(LootEntrySettings.of("NETHERITE_SCRAP", 1, 1, 0.25));
        }
        return loot;
    }

    private static void loadType(JavaPlugin plugin, Path typePath, Path lootDir, Map<String, HordeTypeDefinition> types) {
        String typeId = stripExtension(typePath.getFileName().toString());
        if (!ConfigIds.isValid(typeId)) {
            plugin.getLogger().warning("Skipping horde type with invalid id: " + typeId);
            return;
        }
        HordeTypeSettings settings = new HordeTypeSettings();
        settings.reload(typePath);
        String lootId = settings.lootTableId == null || settings.lootTableId.isEmpty() ? typeId : settings.lootTableId;
        if (!ConfigIds.isValid(lootId)) {
            lootId = typeId;
        }
        Path lootPath = lootDir.resolve(lootId + ".yml");
        LootTableSettings loot = new LootTableSettings();
        if (Files.exists(lootPath)) {
            loot.reload(lootPath);
        } else {
            loot.reload(lootDir.resolve(typeId + ".yml"));
        }
        types.put(typeId, new HordeTypeDefinition(typeId, settings, loot));
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - 4);
    }
}
