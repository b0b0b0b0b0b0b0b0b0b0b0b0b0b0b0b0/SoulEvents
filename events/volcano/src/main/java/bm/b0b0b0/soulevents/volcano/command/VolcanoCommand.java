package bm.b0b0b0.soulevents.volcano.command;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.volcano.VolcanoPlugin;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPermissions;
import bm.b0b0b0.soulevents.volcano.gui.VolcanoGuiFactory;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import bm.b0b0b0.soulevents.volcano.service.VolcanoService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VolcanoCommand implements CommandExecutor, TabCompleter {

    private final SoulEventsApi api;
    private final VolcanoService service;
    private final VolcanoMessageService messages;
    private final VolcanoGuiFactory guiFactory;
    private final VolcanoPlugin plugin;

    public VolcanoCommand(
            SoulEventsApi api,
            VolcanoService service,
            VolcanoMessageService messages,
            VolcanoGuiFactory guiFactory,
            VolcanoPlugin plugin
    ) {
        this.api = api;
        this.service = service;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.plugin = plugin;
    }

    public void register(VolcanoPlugin plugin) {
        var command = plugin.getCommand("volcano");
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "volcano.usage", Map.of());
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(VolcanoPermissions.STAFF)) {
                messages.send(sender, "command.no-permission", Map.of());
                return true;
            }
            plugin.reloadAll();
            messages.send(sender, "volcano.reloaded", Map.of());
            return true;
        }
        if ("admin".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "volcano.usage-admin-console", Map.of());
                return true;
            }
            if (!sender.hasPermission(VolcanoPermissions.STAFF)) {
                messages.send(sender, "command.no-permission", Map.of());
                return true;
            }
            guiFactory.openAdmin(player);
            return true;
        }
        if ("summon".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                messages.send(sender, "volcano.usage", Map.of());
                return true;
            }
            String typeId = args[1].toLowerCase(Locale.ROOT);
            if (sender instanceof Player player) {
                if (sender.hasPermission(VolcanoPermissions.STAFF)) {
                    service.spawnAdminAsync(sender, typeId);
                } else {
                    service.spawnPlayerAsync(player, typeId);
                }
                return true;
            }
            if (!sender.hasPermission(VolcanoPermissions.STAFF)) {
                messages.send(sender, "command.no-permission", Map.of());
                return true;
            }
            service.spawnAdminAsync(sender, typeId);
            return true;
        }
        messages.send(sender, "Volcano.usage", Map.of());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("admin");
            options.add("summon");
            options.add("reload");
            return options;
        }
        if (args.length == 2 && "summon".equalsIgnoreCase(args[0])) {
            return new ArrayList<>(service.config().typesById().keySet());
        }
        return List.of();
    }
}

