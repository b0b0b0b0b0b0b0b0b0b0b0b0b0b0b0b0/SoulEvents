package bm.b0b0b0.soulevents.airdrop.message;

import bm.b0b0b0.soulevents.airdrop.config.settings.LocaleSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class AirDropMessageService {

    private final JavaPlugin plugin;
    private final LocaleSettings localeSettings;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Map<String, String>> bundles = new HashMap<>();

    public AirDropMessageService(JavaPlugin plugin, LocaleSettings localeSettings) {
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

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(resolve(key, placeholders));
    }

    public void broadcast(String key, Map<String, String> placeholders) {
        Component message = resolve(key, placeholders);
        plugin.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }

    public String resolvePlain(String key, Map<String, String> placeholders) {
        String raw = resolveRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace('<' + entry.getKey() + '>', entry.getValue());
        }
        return raw.replace("<newline>", "\n");
    }

    private String resolveRaw(String key) {
        Map<String, String> primary = bundles.get(localeSettings.defaultLocale);
        if (primary != null && primary.containsKey(key)) {
            return primary.get(key);
        }
        Map<String, String> fallback = bundles.get(localeSettings.fallbackLocale);
        if (fallback != null && fallback.containsKey(key)) {
            return fallback.get(key);
        }
        return key;
    }

    private void loadBundle(File langFolder, String fileName) {
        File file = new File(langFolder, fileName);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
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
