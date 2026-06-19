package bm.b0b0b0.soulevents.core.command;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.schematic.SchematicProfile;
import bm.b0b0b0.soulevents.core.schematic.SchematicServiceImpl;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Optional;

public final class SchematicCommandHandler {

    private final SoulEventsApi api;
    private final SchematicServiceImpl schematics;

    public SchematicCommandHandler(SoulEventsApi api, SchematicServiceImpl schematics) {
        this.api = api;
        this.schematics = schematics;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            api.messages().send(sender, "schematic.usage", Map.of());
            return true;
        }
        String action = args[1].toLowerCase();
        if ("list".equals(action)) {
            for (String id : schematics.schematicIds()) {
                sender.sendMessage(" - " + id);
            }
            if (schematics.schematicIds().isEmpty()) {
                api.messages().send(sender, "schematic.list-empty", Map.of());
            }
            return true;
        }
        if ("info".equals(action)) {
            if (args.length < 3) {
                api.messages().send(sender, "schematic.usage", Map.of());
                return true;
            }
            return info(sender, args[2]);
        }
        if ("scan".equals(action)) {
            if (args.length < 3) {
                api.messages().send(sender, "schematic.usage", Map.of());
                return true;
            }
            schematics.reload();
            api.messages().send(sender, "schematic.scan-started", Map.of("id", args[2]));
            return true;
        }
        api.messages().send(sender, "schematic.usage", Map.of());
        return true;
    }

    private boolean info(CommandSender sender, String id) {
        Optional<SchematicProfile> profileOptional = schematics.profile(id);
        if (profileOptional.isEmpty()) {
            api.messages().send(sender, "schematic.unknown", Map.of("id", id));
            return true;
        }
        SchematicProfile profile = profileOptional.get();
        api.messages().send(sender, "schematic.info", Map.of(
                "id", profile.id(),
                "size", profile.sizeX() + "x" + profile.sizeY() + "x" + profile.sizeZ(),
                "chest", profile.chestOffsetX() + ", " + profile.chestOffsetY() + ", " + profile.chestOffsetZ(),
                "vertical", Integer.toString(profile.verticalOffset()),
                "probe", Integer.toString(profile.surfaceProbe().size())
        ));
        return true;
    }
}
