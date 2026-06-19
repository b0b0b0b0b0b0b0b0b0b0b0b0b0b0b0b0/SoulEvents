package bm.b0b0b0.soulevents.airdrop.command;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.airdrop.AirDropPlugin;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPermissions;
import bm.b0b0b0.soulevents.airdrop.gui.AirDropGuiFactory;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import bm.b0b0b0.soulevents.airdrop.service.AirDropService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AirDropCommand implements CommandExecutor, TabCompleter {

    private final SoulEventsApi api;
    private final AirDropService service;
    private final AirDropMessageService messages;
    private final AirDropGuiFactory guiFactory;
    private final AirDropPlugin plugin;

    public AirDropCommand(
            SoulEventsApi api,
            AirDropService service,
            AirDropMessageService messages,
            AirDropGuiFactory guiFactory,
            AirDropPlugin plugin
    ) {
        this.api = api;
        this.service = service;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.plugin = plugin;
    }

    public void register(AirDropPlugin plugin) {
        var command = plugin.getCommand("airdrop");
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "airdrop.usage", Map.of());
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(AirDropPermissions.STAFF)) {
                messages.send(sender, "command.no-permission", Map.of());
                return true;
            }
            plugin.reloadAll();
            messages.send(sender, "airdrop.reloaded", Map.of());
            return true;
        }
        if ("admin".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "airdrop.usage-admin-console", Map.of());
                return true;
            }
            if (!sender.hasPermission(AirDropPermissions.STAFF)) {
                messages.send(sender, "command.no-permission", Map.of());
                return true;
            }
            guiFactory.openAdmin(player);
            return true;
        }
        if ("summon".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                messages.send(sender, "airdrop.usage", Map.of());
                return true;
            }
            String typeId = args[1].toLowerCase(Locale.ROOT);
            if (sender instanceof Player player) {
                if (sender.hasPermission(AirDropPermissions.STAFF)) {
                    service.spawnAdminAsync(sender, typeId);
                } else {
                    service.spawnPlayerAsync(player, typeId);
                }
                return true;
            }
            if (!sender.hasPermission(AirDropPermissions.STAFF)) {
                messages.send(sender, "command.no-permission", Map.of());
                return true;
            }
            service.spawnAdminAsync(sender, typeId);
            return true;
        }
        messages.send(sender, "airdrop.usage", Map.of());
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
