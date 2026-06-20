package bm.b0b0b0.soulevents.mobwaves.command;



import bm.b0b0b0.soulevents.mobwaves.MobWavesPermissions;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;

import bm.b0b0b0.soulevents.mobwaves.gui.MobWavesGuiFactory;

import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;

import bm.b0b0b0.soulevents.mobwaves.service.MobHordeService;

import org.bukkit.command.Command;

import org.bukkit.command.CommandExecutor;

import org.bukkit.command.CommandSender;

import org.bukkit.command.TabCompleter;

import org.bukkit.entity.Player;



import java.util.ArrayList;

import java.util.List;

import java.util.Locale;

import java.util.Map;



public final class MobWavesCommand implements CommandExecutor, TabCompleter {



    private final MobWavesPlugin plugin;

    private final MobWaveMessageService messages;

    private final MobWavesGuiFactory guiFactory;

    private final MobHordeService hordeService;



    public MobWavesCommand(

            MobWavesPlugin plugin,

            MobWaveMessageService messages,

            MobWavesGuiFactory guiFactory,

            MobHordeService hordeService

    ) {

        this.plugin = plugin;

        this.messages = messages;

        this.guiFactory = guiFactory;

        this.hordeService = hordeService;

    }



    public void register(MobWavesPlugin plugin) {

        var command = plugin.getCommand("mobwaves");

        if (command != null) {

            command.setExecutor(this);

            command.setTabCompleter(this);

        }

    }



    @Override

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {

            messages.send(sender, "mobwaves.usage", Map.of());

            return true;

        }

        if ("reload".equalsIgnoreCase(args[0])) {

            if (!sender.hasPermission(MobWavesPermissions.STAFF)) {

                messages.send(sender, "command.no-permission", Map.of());

                return true;

            }

            plugin.reloadAll();

            messages.send(sender, "mobwaves.reloaded", Map.of());

            return true;

        }

        if ("admin".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player player)) {

                messages.send(sender, "mobwaves.usage-admin-console", Map.of());

                return true;

            }

            if (!sender.hasPermission(MobWavesPermissions.STAFF)) {

                messages.send(sender, "command.no-permission", Map.of());

                return true;

            }

            guiFactory.openAdmin(player);

            return true;

        }

        if ("summon".equalsIgnoreCase(args[0])) {

            if (args.length < 2) {

                messages.send(sender, "mobwaves.usage", Map.of());

                return true;

            }

            if (!sender.hasPermission(MobWavesPermissions.STAFF)) {

                messages.send(sender, "command.no-permission", Map.of());

                return true;

            }

            String typeId = args[1].toLowerCase(Locale.ROOT);

            hordeService.spawnAdminAsync(sender, typeId);

            return true;

        }

        messages.send(sender, "mobwaves.usage", Map.of());

        return true;

    }



    @Override

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {

            List<String> options = new ArrayList<>();

            if (sender.hasPermission(MobWavesPermissions.STAFF)) {

                options.add("admin");

                options.add("summon");

                options.add("reload");

            }

            String prefix = args[0].toLowerCase(Locale.ROOT);

            return options.stream().filter(option -> option.startsWith(prefix)).toList();

        }

        if (args.length == 2 && "summon".equalsIgnoreCase(args[0]) && sender.hasPermission(MobWavesPermissions.STAFF)) {

            return new ArrayList<>(hordeService.config().typesById().keySet());

        }

        return List.of();

    }

}

