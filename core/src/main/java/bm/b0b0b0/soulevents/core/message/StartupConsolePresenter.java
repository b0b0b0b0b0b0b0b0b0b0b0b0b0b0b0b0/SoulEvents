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
    }

    public void beginInitialization() {
        line(console(), "startup.banner.init");
    }

    public void logIntegration(String key, Map<String, String> placeholders) {
        line(console(), key, placeholders);
    }

    public void logModule(String moduleId) {
        String moduleKey = "startup.module." + moduleId;
        if (messageService.hasPlain(moduleKey)) {
            line(console(), moduleKey);
            return;
        }
        line(console(), "startup.module.generic", Map.of("module", moduleId));
    }

    public void logStartupFooter(EventModuleRegistry moduleRegistry) {
        ConsoleCommandSender console = console();
        int count = moduleRegistry == null ? 0 : moduleRegistry.modules().size();
        if (count == 0) {
            line(console, "startup.modules.none");
        }
        blank(console);
        line(console, "startup.banner.separator");
        coloredLine(console, "startup.loaded", ConsolePalette::green);
        line(console, "startup.banner.separator");
        blank(console);
    }

    public void logReloadComplete() {
        ConsoleCommandSender console = console();
        blank(console);
        line(console, "startup.banner.separator");
        coloredLine(console, "startup.reload-complete", ConsolePalette::green);
        line(console, "startup.banner.separator");
        blank(console);
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
