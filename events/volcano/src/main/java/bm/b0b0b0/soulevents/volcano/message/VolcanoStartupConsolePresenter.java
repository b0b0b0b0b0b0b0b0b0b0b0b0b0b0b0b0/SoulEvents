package bm.b0b0b0.soulevents.volcano.message;

import java.util.Map;
import java.util.function.UnaryOperator;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class VolcanoStartupConsolePresenter {

    private final JavaPlugin plugin;
    private final VolcanoMessageService messages;

    public VolcanoStartupConsolePresenter(JavaPlugin plugin, VolcanoMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void logTypeLoaded(String typeId) {
        line(console(), "volcano.startup.type-loaded", Map.of("id", typeId));
    }

    public void logRegistered() {
        ConsoleCommandSender console = console();
        blank(console);
        line(console, "volcano.startup.separator");
        coloredLine(console, "volcano.startup.registered", VolcanoConsolePalette::green);
        line(console, "volcano.startup.separator");
        blank(console);
    }

    public void logCoreMissing() {
        logFailure("volcano.startup.core-missing");
    }

    public void logDatabaseFailed() {
        logFailure("volcano.startup.database-failed");
    }

    private void logFailure(String key) {
        ConsoleCommandSender console = console();
        line(console, "volcano.startup.separator");
        coloredLine(console, key, VolcanoConsolePalette::red);
        line(console, "volcano.startup.separator");
        blank(console);
    }

    private ConsoleCommandSender console() {
        return plugin.getServer().getConsoleSender();
    }

    private void blank(ConsoleCommandSender console) {
        console.sendMessage(" ");
    }

    private void line(ConsoleCommandSender console, String key, Map<String, String> placeholders) {
        console.sendMessage(VolcanoConsolePalette.prefixLine(messages.resolvePlain(key, placeholders)));
    }

    private void line(ConsoleCommandSender console, String key) {
        line(console, key, Map.of());
    }

    private void coloredLine(ConsoleCommandSender console, String key, UnaryOperator<String> color) {
        console.sendMessage(VolcanoConsolePalette.prefixLine(color.apply(messages.resolvePlain(key, Map.of()))));
    }
}

