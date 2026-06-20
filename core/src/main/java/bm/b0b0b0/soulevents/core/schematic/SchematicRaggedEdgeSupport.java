package bm.b0b0b0.soulevents.core.schematic;

final class SchematicRaggedEdgeSupport {

    private SchematicRaggedEdgeSupport() {
    }

    static boolean shouldRaggedTrim(int x, int z, int y, int seedX, int seedZ, float density) {
        float clamped = Math.clamp(density, 0.05f, 0.95f);
        long hash = (x * 734287L) ^ (z * 912271L) ^ ((long) y * 91277L) ^ (seedX * 48271L) ^ (seedZ * 91823L);
        hash = hash * 6364136223846793005L + 1442695040888963407L;
        double roll = ((hash >>> 11) & 0xFFFFL) / 65535.0;
        return roll < clamped;
    }
}
