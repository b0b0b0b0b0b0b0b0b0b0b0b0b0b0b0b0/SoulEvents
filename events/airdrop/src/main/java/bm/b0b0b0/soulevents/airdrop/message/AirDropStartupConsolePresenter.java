package bm.b0b0b0.soulevents.airdrop.message;

import java.util.Map;
import java.util.function.UnaryOperator;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class AirDropStartupConsolePresenter {

    private final JavaPlugin plugin;
    private final AirDropMessageService messages;

    public AirDropStartupConsolePresenter(JavaPlugin plugin, AirDropMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void logRegistered() {
        ConsoleCommandSender console = console();
        blank(console);
        line(console, "airdrop.startup.separator");
        coloredLine(console, "airdrop.startup.registered", AirDropConsolePalette::green);
        line(console, "airdrop.startup.separator");
        blank(console);
    }

    public void logCoreMissing() {
        logFailure("airdrop.startup.core-missing");
    }

    public void logDatabaseFailed() {
        logFailure("airdrop.startup.database-failed");
    }

    private void logFailure(String key) {
        ConsoleCommandSender console = console();
        line(console, "airdrop.startup.separator");
        coloredLine(console, key, AirDropConsolePalette::red);
        line(console, "airdrop.startup.separator");
        blank(console);
    }

    private ConsoleCommandSender console() {
        return plugin.getServer().getConsoleSender();
    }

    private void blank(ConsoleCommandSender console) {
        console.sendMessage(" ");
    }

    private void line(ConsoleCommandSender console, String key) {
        console.sendMessage(AirDropConsolePalette.prefixLine(messages.resolvePlain(key, Map.of())));
    }

    private void coloredLine(ConsoleCommandSender console, String key, UnaryOperator<String> color) {
        console.sendMessage(AirDropConsolePalette.prefixLine(color.apply(messages.resolvePlain(key, Map.of()))));
    }
}
