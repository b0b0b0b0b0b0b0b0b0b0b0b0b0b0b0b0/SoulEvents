package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;

import java.nio.file.Path;
import java.util.List;

public record SchematicDefinition(
        String id,
        Path schematicFile,
        Path configFile,
        SchematicSettings settings,
        SchematicMetadata metadata
) {

    public boolean isReady() {
        if (metadata == null) {
            return false;
        }
        return switch (metadata.markerValidation()) {
            case OK, MANUAL -> true;
            case NOT_FOUND, AMBIGUOUS -> false;
        };
    }

    public record SchematicMetadata(
            int sizeX,
            int sizeY,
            int sizeZ,
            int originX,
            int originY,
            int originZ,
            int regionMinX,
            int regionMinY,
            int regionMinZ,
            int regionMaxX,
            int regionMaxY,
            int regionMaxZ,
            int chestOffsetX,
            int chestOffsetY,
            int chestOffsetZ,
            boolean markerDetected,
            MarkerValidation markerValidation,
            int markerCount,
            String markerBlock,
            List<FlatSurfaceOffset> footprint,
            List<FlatSurfaceOffset> surfaceProbe,
            int blockCount
    ) {
    }
}
