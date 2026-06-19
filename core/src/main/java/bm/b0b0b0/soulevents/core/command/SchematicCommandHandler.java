package bm.b0b0b0.soulevents.core.command;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.schematic.SchematicProfile;
import bm.b0b0b0.soulevents.core.schematic.MarkerValidation;
import bm.b0b0b0.soulevents.core.schematic.SchematicDefinition;
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
                Optional<SchematicDefinition> definition = schematics.definition(id);
                if (definition.isEmpty() || definition.get().isReady()) {
                    sender.sendMessage(" - " + id);
                    continue;
                }
                SchematicDefinition.SchematicMetadata metadata = definition.get().metadata();
                if (metadata == null) {
                    sender.sendMessage(" - " + id + " (scan failed)");
                    continue;
                }
                api.messages().send(sender, "schematic.list-entry-invalid", Map.of(
                        "id", id,
                        "block", metadata.markerBlock(),
                        "count", Integer.toString(metadata.markerCount())
                ));
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
        Optional<SchematicDefinition> definitionOptional = schematics.definition(id);
        if (definitionOptional.isEmpty()) {
            api.messages().send(sender, "schematic.unknown", Map.of("id", id));
            return true;
        }
        SchematicDefinition definition = definitionOptional.get();
        SchematicDefinition.SchematicMetadata metadata = definition.metadata();
        if (metadata == null) {
            api.messages().send(sender, "schematic.scan-failed", Map.of("id", id));
            return true;
        }
        if (!definition.isReady()) {
            sendMarkerErrors(sender, id, metadata);
            return true;
        }
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
                "markers", Integer.toString(profile.markerCount()),
                "vertical", Integer.toString(profile.verticalOffset()),
                "footprint", Integer.toString(metadata.footprint().size()),
                "probe", Integer.toString(profile.surfaceProbe().size())
        ));
        return true;
    }

    private void sendMarkerErrors(CommandSender sender, String id, SchematicDefinition.SchematicMetadata metadata) {
        Map<String, String> placeholders = Map.of(
                "id", id,
                "block", metadata.markerBlock(),
                "count", Integer.toString(metadata.markerCount())
        );
        api.messages().send(sender, "schematic.info-invalid", placeholders);
        if (metadata.markerValidation() == MarkerValidation.AMBIGUOUS) {
            api.messages().send(sender, "schematic.marker-ambiguous", placeholders);
            sendMarkerFixHint(sender, placeholders);
            return;
        }
        if (metadata.markerValidation() == MarkerValidation.NOT_FOUND) {
            api.messages().send(sender, "schematic.marker-missing", placeholders);
            sendMarkerFixHint(sender, placeholders);
        }
    }

    private void sendMarkerFixHint(CommandSender sender, Map<String, String> placeholders) {
        for (int index = 1; index <= 4; index++) {
            api.messages().send(sender, "schematic.marker-fix-hint." + index, placeholders);
        }
    }
}
