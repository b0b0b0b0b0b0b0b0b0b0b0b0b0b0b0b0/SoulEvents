package bm.b0b0b0.soulevents.api.schematic;

public final class SchematicSpawnRoughness {

    private SchematicSpawnRoughness() {
    }

    public static int limit(int maxSurfaceDelta, int terrainAdaptBlocks) {
        int delta = Math.max(0, maxSurfaceDelta);
        int adapt = Math.max(0, terrainAdaptBlocks);
        if (adapt <= 0) {
            return delta;
        }
        return Math.max(delta, adapt);
    }
}
