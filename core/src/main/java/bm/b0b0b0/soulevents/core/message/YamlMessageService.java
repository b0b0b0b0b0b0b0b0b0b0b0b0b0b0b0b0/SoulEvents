package bm.b0b0b0.soulevents.core.message;

import bm.b0b0b0.soulevents.api.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class YamlMessageService implements MessageService {

    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages = new HashMap<>();

    public YamlMessageService(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public Component resolve(String key, Map<String, String> placeholders) {
        String raw = messages.getOrDefault(key, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace('<' + entry.getKey() + '>', entry.getValue());
        }
        return miniMessage.deserialize(raw);
    }

    @Override
    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(resolve(key, placeholders));
    }

    @Override
    public void broadcast(String key, Map<String, String> placeholders) {
        Component message = resolve(key, placeholders);
        org.bukkit.Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }

    public String resolvePlain(String key, Map<String, String> placeholders) {
        String raw = messages.getOrDefault(key, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace('<' + entry.getKey() + '>', entry.getValue());
        }
        return raw.replace("<newline>", "\n");
    }

    public boolean hasPlain(String key) {
        return messages.containsKey(key);
    }

    @Override
    public void reload() {
        messages.clear();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        File ru = new File(langFolder, "ru.yml");
        if (!ru.exists()) {
            plugin.saveResource("lang/ru.yml", false);
            plugin.saveResource("lang/en.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(ru);
        for (String messageKey : yaml.getKeys(true)) {
            if (yaml.isString(messageKey)) {
                messages.put(messageKey, yaml.getString(messageKey, messageKey));
            }
        }
    }
}
