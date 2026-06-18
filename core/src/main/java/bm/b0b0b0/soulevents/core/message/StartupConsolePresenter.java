package bm.b0b0b0.soulevents.core.message;

import bm.b0b0b0.soulevents.api.module.EventModuleRegistry;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class StartupConsolePresenter {

    private final JavaPlugin plugin;
    private final YamlMessageService messageService;

    public StartupConsolePresenter(JavaPlugin plugin, YamlMessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
    }

    public void logStartupHeader() {
        ConsoleCommandSender console = console();
        blank(console);
        line(console, "startup.banner.separator");
        line(console, "startup.banner.intro");
        line(console, "startup.banner.version", Map.of("version", plugin.getPluginMeta().getVersion()));
        blank(console);
        line(console, "startup.banner.init");
    }

    public void logStartupComplete(EventModuleRegistry moduleRegistry) {
        ConsoleCommandSender console = console();
        line(console, "startup.banner.separator");
        logModules(console, moduleRegistry);
        coloredLine(console, "startup.loaded", ConsolePalette::green);
        line(console, "startup.banner.separator");
        blank(console);
    }

    public void logReloadComplete() {
        ConsoleCommandSender console = console();
        blank(console);
        line(console, "startup.banner.separator");
        line(console, "startup.banner.intro");
        line(console, "startup.banner.version", Map.of("version", plugin.getPluginMeta().getVersion()));
        blank(console);
        line(console, "startup.banner.init");
        line(console, "startup.reload-complete");
        line(console, "startup.banner.separator");
        blank(console);
    }

    private void logModules(ConsoleCommandSender console, EventModuleRegistry moduleRegistry) {
        int count = moduleRegistry.modules().size();
        if (count == 0) {
            return;
        }
        line(console, "startup.modules.line", Map.of("count", String.valueOf(count)));
    }

    private ConsoleCommandSender console() {
        return plugin.getServer().getConsoleSender();
    }

    private void blank(ConsoleCommandSender console) {
        console.sendMessage(" ");
    }

    private void line(ConsoleCommandSender console, String key) {
        line(console, key, Map.of());
    }

    private void line(ConsoleCommandSender console, String key, Map<String, String> placeholders) {
        console.sendMessage(ConsolePalette.prefixLine(messageService.resolvePlain(key, placeholders)));
    }

    private void coloredLine(ConsoleCommandSender console, String key, UnaryOperator<String> color) {
        console.sendMessage(ConsolePalette.prefixLine(color.apply(messageService.resolvePlain(key, Map.of()))));
    }
}
