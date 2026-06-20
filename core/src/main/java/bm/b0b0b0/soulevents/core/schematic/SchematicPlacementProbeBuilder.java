package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SchematicPlacementProbeBuilder {

    static final int FULL_FOOTPRINT_PROBE_LIMIT = 64;

    private static final int AUTO_TARGET_SAMPLES = 56;

    private SchematicPlacementProbeBuilder() {
    }

    public static List<FlatSurfaceOffset> build(
            List<FlatSurfaceOffset> footprint,
            int probeStep,
            int safetyMargin
    ) {
        if (footprint.isEmpty()) {
            return List.of(new FlatSurfaceOffset(0, 0));
        }

        Bounds bounds = Bounds.of(footprint);
        Set<String> seen = new LinkedHashSet<>();
        List<FlatSurfaceOffset> probe = new ArrayList<>();

        if (footprint.size() <= FULL_FOOTPRINT_PROBE_LIMIT) {
            for (FlatSurfaceOffset point : footprint) {
                add(probe, seen, point.dx(), point.dz());
            }
        } else {
            addCornerAndCenter(probe, seen, bounds);
            int step = resolveStep(bounds, probeStep, footprint.size());
            addGrid(probe, seen, bounds, step);
        }

        if (safetyMargin > 0) {
            addSafetyRing(probe, seen, bounds, safetyMargin);
        }
        return List.copyOf(probe);
    }

    static boolean isInteriorFootprint(int dx, int dz, Bounds bounds) {
        return dx >= bounds.minDx() && dx <= bounds.maxDx()
                && dz >= bounds.minDz() && dz <= bounds.maxDz();
    }

    public static List<FlatSurfaceOffset> cornerSampleOffsets(List<FlatSurfaceOffset> footprint) {
        if (footprint.isEmpty()) {
            return List.of(new FlatSurfaceOffset(0, 0));
        }
        Bounds bounds = Bounds.of(footprint);
        return List.of(
                new FlatSurfaceOffset(bounds.minDx(), bounds.minDz()),
                new FlatSurfaceOffset(bounds.maxDx(), bounds.minDz()),
                new FlatSurfaceOffset(bounds.minDx(), bounds.maxDz()),
                new FlatSurfaceOffset(bounds.maxDx(), bounds.maxDz()),
                new FlatSurfaceOffset(bounds.centerDx(), bounds.centerDz())
        );
    }

    private static int resolveStep(Bounds bounds, int configuredStep, int footprintSize) {
        if (configuredStep > 0) {
            return configuredStep;
        }
        int width = bounds.width();
        int depth = bounds.depth();
        int area = width * depth;
        int targetSamples = Math.min(144, Math.max(AUTO_TARGET_SAMPLES, footprintSize / 8));
        return Math.max(1, (int) Math.ceil(Math.sqrt((double) area / targetSamples)));
    }

    private static void addCornerAndCenter(List<FlatSurfaceOffset> probe, Set<String> seen, Bounds bounds) {
        add(probe, seen, bounds.minDx(), bounds.minDz());
        add(probe, seen, bounds.maxDx(), bounds.minDz());
        add(probe, seen, bounds.minDx(), bounds.maxDz());
        add(probe, seen, bounds.maxDx(), bounds.maxDz());
        add(probe, seen, bounds.centerDx(), bounds.centerDz());
    }

    private static void addGrid(List<FlatSurfaceOffset> probe, Set<String> seen, Bounds bounds, int step) {
        for (int dx = bounds.minDx(); dx <= bounds.maxDx(); dx += step) {
            for (int dz = bounds.minDz(); dz <= bounds.maxDz(); dz += step) {
                add(probe, seen, dx, dz);
            }
        }
        add(probe, seen, bounds.maxDx(), bounds.centerDz());
        add(probe, seen, bounds.centerDx(), bounds.maxDz());
    }

    private static void addSafetyRing(
            List<FlatSurfaceOffset> probe,
            Set<String> seen,
            Bounds bounds,
            int margin
    ) {
        int minDx = bounds.minDx() - margin;
        int maxDx = bounds.maxDx() + margin;
        int minDz = bounds.minDz() - margin;
        int maxDz = bounds.maxDz() + margin;
        for (int dx = minDx; dx <= maxDx; dx++) {
            add(probe, seen, dx, minDz);
            add(probe, seen, dx, maxDz);
        }
        for (int dz = minDz + 1; dz < maxDz; dz++) {
            add(probe, seen, minDx, dz);
            add(probe, seen, maxDx, dz);
        }
    }

    private static void add(List<FlatSurfaceOffset> probe, Set<String> seen, int dx, int dz) {
        String key = dx + ":" + dz;
        if (seen.add(key)) {
            probe.add(new FlatSurfaceOffset(dx, dz));
        }
    }

    record Bounds(int minDx, int maxDx, int minDz, int maxDz) {

        static Bounds of(List<FlatSurfaceOffset> footprint) {
            int minDx = Integer.MAX_VALUE;
            int maxDx = Integer.MIN_VALUE;
            int minDz = Integer.MAX_VALUE;
            int maxDz = Integer.MIN_VALUE;
            for (FlatSurfaceOffset point : footprint) {
                minDx = Math.min(minDx, point.dx());
                maxDx = Math.max(maxDx, point.dx());
                minDz = Math.min(minDz, point.dz());
                maxDz = Math.max(maxDz, point.dz());
            }
            return new Bounds(minDx, maxDx, minDz, maxDz);
        }

        int width() {
            return maxDx - minDx + 1;
        }

        int depth() {
            return maxDz - minDz + 1;
        }

        int centerDx() {
            return (minDx + maxDx) / 2;
        }

        int centerDz() {
            return (minDz + maxDz) / 2;
        }
    }
}
