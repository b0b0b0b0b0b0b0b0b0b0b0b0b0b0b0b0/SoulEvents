package bm.b0b0b0.soulevents.core.command;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.EventModule;
import bm.b0b0b0.soulevents.core.SoulEventsPlugin;
import bm.b0b0b0.soulevents.core.command.SchematicCommandHandler;
import bm.b0b0b0.soulevents.core.schematic.SchematicServiceImpl;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SoulEventsCommand implements CommandExecutor, TabCompleter {

    private final SoulEventsApi api;
    private final SchematicCommandHandler schematicCommands;

    public SoulEventsCommand(SoulEventsApi api, SchematicServiceImpl schematics) {
        this.api = api;
        this.schematicCommands = new SchematicCommandHandler(api, schematics);
    }

    public void register(SoulEventsPlugin plugin) {
        var command = plugin.getCommand("soulevents");
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("soulevents.admin")) {
            api.messages().send(sender, "command.no-permission", Map.of());
            return true;
        }
        if (args.length == 0) {
            api.messages().send(sender, "command.usage", Map.of());
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            SoulEventsPlugin plugin = (SoulEventsPlugin) api.plugin();
            plugin.reloadAll();
            api.messages().send(sender, "command.reloaded", Map.of());
            return true;
        }
        if ("modules".equalsIgnoreCase(args[0])) {
            for (EventModule module : api.modules().modules()) {
                sender.sendMessage(module.id());
            }
            return true;
        }
        if ("schematic".equalsIgnoreCase(args[0]) || "schematics".equalsIgnoreCase(args[0])) {
            return schematicCommands.handle(sender, args);
        }
        api.messages().send(sender, "command.usage", Map.of());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("reload");
            options.add("modules");
            options.add("schematic");
            return options;
        }
        if (args.length == 2 && ("schematic".equalsIgnoreCase(args[0]) || "schematics".equalsIgnoreCase(args[0]))) {
            return List.of("list", "info", "scan");
        }
        return List.of();
    }
}
