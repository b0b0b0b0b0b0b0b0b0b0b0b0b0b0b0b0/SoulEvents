package bm.b0b0b0.soulevents.mobwaves.message;

import bm.b0b0b0.soulevents.mobwaves.config.settings.LocaleSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobWaveMessageService {

    private final JavaPlugin plugin;
    private final LocaleSettings localeSettings;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Map<String, String>> bundles = new HashMap<>();

    public MobWaveMessageService(JavaPlugin plugin, LocaleSettings localeSettings) {
        this.plugin = plugin;
        this.localeSettings = localeSettings;
    }

    public void load() {
        bundles.clear();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        ensureLangFolder(langFolder);
        for (String locale : localeSettings.locales) {
            copyDefaultLang(langFolder, locale + ".yml");
            loadBundle(langFolder, locale + ".yml");
        }
    }

    public void reload() {
        load();
    }

    public Component resolve(String key, Map<String, String> placeholders) {
        String raw = resolveRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace('<' + entry.getKey() + '>', entry.getValue());
        }
        return miniMessage.deserialize(raw);
    }

    public String resolvePlain(String key, Map<String, String> placeholders) {
        String raw = resolveRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace('<' + entry.getKey() + '>', entry.getValue());
        }
        return raw.replace("<newline>", "\n");
    }

    public List<Component> resolveLore(String prefix, Map<String, String> placeholders) {
        List<Component> lore = new ArrayList<>();
        for (int index = 1; index <= 16; index++) {
            String key = prefix + "." + index;
            String raw = resolveRawOrNull(key);
            if (raw == null) {
                break;
            }
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace('<' + entry.getKey() + '>', entry.getValue());
            }
            lore.add(miniMessage.deserialize(raw));
        }
        return lore;
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(resolve(key, placeholders));
    }

    public void broadcast(String key, Map<String, String> placeholders) {
        Component message = resolve(key, placeholders);
        plugin.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }

    private String resolveRawOrNull(String key) {
        Map<String, String> primary = bundles.get(localeSettings.defaultLocale);
        if (primary != null && primary.containsKey(key)) {
            return primary.get(key);
        }
        Map<String, String> fallback = bundles.get(localeSettings.fallbackLocale);
        if (fallback != null && fallback.containsKey(key)) {
            return fallback.get(key);
        }
        return null;
    }

    private String resolveRaw(String key) {
        String raw = resolveRawOrNull(key);
        return raw == null ? "<red>" + key + "</red>" : raw;
    }

    private void loadBundle(File langFolder, String fileName) {
        File file = new File(langFolder, fileName);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().warning("Broken lang/" + fileName + ", restoring from jar: " + exception.getMessage());
            if (!file.delete()) {
                plugin.getLogger().severe("Failed to delete broken lang/" + fileName);
                return;
            }
            copyDefaultLang(langFolder, fileName);
            try {
                yaml.load(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
            } catch (IOException | InvalidConfigurationException retryException) {
                plugin.getLogger().severe("Cannot load lang/" + fileName + ": " + retryException.getMessage());
                return;
            }
        }
        Map<String, String> messages = new HashMap<>();
        for (String messageKey : yaml.getKeys(true)) {
            if (yaml.isString(messageKey)) {
                messages.put(messageKey, yaml.getString(messageKey, messageKey));
            }
        }
        String locale = fileName.substring(0, fileName.length() - 4);
        bundles.put(locale, messages);
    }

    private void ensureLangFolder(File langFolder) {
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create lang folder.");
        }
    }

    private void copyDefaultLang(File langFolder, String fileName) {
        File target = new File(langFolder, fileName);
        if (target.exists()) {
            return;
        }
        try (InputStream stream = plugin.getResource("lang/" + fileName)) {
            if (stream == null) {
                return;
            }
            Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to copy lang/" + fileName + ": " + exception.getMessage());
        }
    }
}
