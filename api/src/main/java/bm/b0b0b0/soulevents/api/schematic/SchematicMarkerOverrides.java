package bm.b0b0b0.soulevents.api.schematic;

public record SchematicMarkerOverrides(int spawnCount, Boolean replaceWithAir) {

    public SchematicMarkerOverrides(int spawnCount) {
        this(spawnCount, null);
    }

    public static SchematicMarkerOverrides defaults() {
        return new SchematicMarkerOverrides(1, null);
    }

    public int effectiveSpawnCount() {
        return Math.max(1, spawnCount);
    }
}
