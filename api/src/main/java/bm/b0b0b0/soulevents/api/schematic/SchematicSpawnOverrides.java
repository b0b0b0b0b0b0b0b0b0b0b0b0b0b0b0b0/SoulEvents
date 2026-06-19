package bm.b0b0b0.soulevents.api.schematic;

public record SchematicSpawnOverrides(
        SchematicPlacementOverrides placement,
        SchematicBlendOverrides blend,
        SchematicPasteOverrides paste
) {
}
