package bm.b0b0b0.soulevents.mobwaves.message;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.function.UnaryOperator;

public final class MobWavesStartupConsolePresenter {

    private final JavaPlugin plugin;
    private final MobWaveMessageService messages;

    public MobWavesStartupConsolePresenter(JavaPlugin plugin, MobWaveMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void logTypeLoaded(String typeId) {
        line(console(), "mobwaves.startup.type-loaded", Map.of("id", typeId));
    }

    public void logProfileLoaded(String profileId) {
        line(console(), "mobwaves.startup.profile-loaded", Map.of("id", profileId));
    }

    public void logRegistered(int typeCount, int profileCount) {
        line(console(), "mobwaves.startup.registered", Map.of(
                "types", Integer.toString(typeCount),
                "profiles", Integer.toString(profileCount)
        ));
    }

    public void logCoreMissing() {
        logFailure("mobwaves.startup.core-missing");
    }

    private void logFailure(String key) {
        ConsoleCommandSender console = console();
        line(console, "mobwaves.startup.separator");
        coloredLine(console, key, MobWavesConsolePalette::red);
        line(console, "mobwaves.startup.separator");
        blank(console);
    }

    private ConsoleCommandSender console() {
        return plugin.getServer().getConsoleSender();
    }

    private void blank(ConsoleCommandSender console) {
        console.sendMessage(" ");
    }

    private void line(ConsoleCommandSender console, String key, Map<String, String> placeholders) {
        console.sendMessage(MobWavesConsolePalette.prefixLine(messages.resolvePlain(key, placeholders)));
    }

    private void line(ConsoleCommandSender console, String key) {
        line(console, key, Map.of());
    }

    private void coloredLine(ConsoleCommandSender console, String key, UnaryOperator<String> color) {
        console.sendMessage(MobWavesConsolePalette.prefixLine(color.apply(messages.resolvePlain(key, Map.of()))));
    }
}
