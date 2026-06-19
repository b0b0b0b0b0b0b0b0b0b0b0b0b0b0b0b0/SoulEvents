package bm.b0b0b0.soulevents.core.message;

import java.util.Map;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreConsoleLog {

    private CoreConsoleLog() {
    }

    public static void line(JavaPlugin plugin, YamlMessageService messages, String key) {
        line(plugin, messages, key, Map.of());
    }

    public static void line(JavaPlugin plugin, YamlMessageService messages, String key, Map<String, String> placeholders) {
        ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        console.sendMessage(ConsolePalette.prefixLine(messages.resolvePlain(key, placeholders)));
    }
}
