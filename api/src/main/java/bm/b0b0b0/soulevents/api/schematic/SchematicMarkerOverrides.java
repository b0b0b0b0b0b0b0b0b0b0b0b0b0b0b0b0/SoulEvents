package bm.b0b0b0.soulevents.api.schematic;

public record SchematicMarkerOverrides(int spawnCount) {

    public static SchematicMarkerOverrides defaults() {
        return new SchematicMarkerOverrides(1);
    }

    public int effectiveSpawnCount() {
        return Math.max(1, spawnCount);
    }
}
