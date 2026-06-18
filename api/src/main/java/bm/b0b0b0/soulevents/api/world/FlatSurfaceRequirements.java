package bm.b0b0b0.soulevents.api.world;

public record FlatSurfaceRequirements(
        int maxSurfaceDelta,
        int surfaceYOffset,
        int minAirAbove,
        boolean requireSolidBelow
) {

    public static FlatSurfaceRequirements defaults() {
        return new FlatSurfaceRequirements(0, 1, 2, true);
    }
}
