package bm.b0b0b0.soulevents.mobwaves.config;

import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveDefinitionSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveMobEntrySettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveProfileSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProfileDirectoryLoader {

    private ProfileDirectoryLoader() {
    }

    public static Map<String, WaveProfileDefinition> load(JavaPlugin plugin) {
        Path profilesDir = plugin.getDataFolder().toPath().resolve("profiles");
        profilesDir.toFile().mkdirs();
        Map<String, WaveProfileDefinition> loaded = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.yml")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String id = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
                WaveProfileSettings settings = new WaveProfileSettings();
                settings.reload(file);
                ensureDefaults(settings);
                ensureMobOverrideDefaults(settings);
                if (repairEmptyWaveOneEntries(settings)) {
                    settings.save(file);
                }
                loaded.put(id, new WaveProfileDefinition(id, settings));
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("MobWaves profiles load failed: " + exception.getMessage());
        }
        if (loaded.isEmpty()) {
            WaveProfileSettings defaults = new WaveProfileSettings();
            Path defaultPath = profilesDir.resolve("default.yml");
            defaults.reload(defaultPath);
            ensureDefaults(defaults);
            ensureMobOverrideDefaults(defaults);
            defaults.save(defaultPath);
            loaded.put("default", new WaveProfileDefinition("default", defaults));
        }
        return loaded;
    }

    private static void ensureDefaults(WaveProfileSettings settings) {
        if (settings.defaultMobEffects == null) {
            settings.defaultMobEffects = WaveProfileSettings.defaultMobEffects();
        }
        if (settings.waves == null || settings.waves.isEmpty()) {
            settings.waves = WaveProfileSettings.defaultWaves();
            return;
        }
        for (WaveDefinitionSettings wave : settings.waves) {
            if (wave.entries == null) {
                wave.entries = new ArrayList<>();
            }
            if (wave.superBoss == null) {
                wave.superBoss = WaveDefinitionSettings.defaultSuperBoss();
            }
        }
        for (MobTypeOverrideSettings override : settings.mobOverrides.values()) {
            if (override.effects == null) {
                override.effects = new ArrayList<>();
            }
        }
    }

    private static boolean repairEmptyWaveOneEntries(WaveProfileSettings settings) {
        if (settings.waves.isEmpty()) {
            return false;
        }
        WaveDefinitionSettings first = settings.waves.get(0);
        if (!first.entries.isEmpty()) {
            return false;
        }
        first.entries.addAll(WaveProfileSettings.waveOneDefaultEntries());
        return true;
    }

    private static void ensureMobOverrideDefaults(WaveProfileSettings settings) {
        if (settings.mobOverrides == null) {
            settings.mobOverrides = new LinkedHashMap<>();
        }
        for (Map.Entry<String, MobTypeOverrideSettings> entry : WaveProfileSettings.defaultMobOverrides().entrySet()) {
            settings.mobOverrides.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public static void save(JavaPlugin plugin, WaveProfileDefinition profile) {
        Path path = plugin.getDataFolder().toPath().resolve("profiles").resolve(profile.id() + ".yml");
        path.getParent().toFile().mkdirs();
        profile.settings().save(path);
    }
}
