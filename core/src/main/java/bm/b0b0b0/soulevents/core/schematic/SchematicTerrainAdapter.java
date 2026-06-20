package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicTerrainMaterialsSettings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public final class SchematicTerrainAdapter {

    private static final int APPROACH_TERRACE_DEPTH = 3;

    private final SchematicMaterialSet naturalTop;
    private final SchematicMaterialSet removable;
    private final Material anchorSurface;
    private final Material anchorSubsurface;

    private SchematicTerrainAdapter(
            SchematicMaterialSet naturalTop,
            SchematicMaterialSet removable,
            Material anchorSurface,
            Material anchorSubsurface
    ) {
        this.naturalTop = naturalTop;
        this.removable = removable;
        this.anchorSurface = anchorSurface;
        this.anchorSubsurface = anchorSubsurface;
    }

    public static SchematicTerrainAdapter from(SchematicTerrainMaterialsSettings settings) {
        return new SchematicTerrainAdapter(
                SchematicMaterialSet.terrainNaturalTop(settings),
                SchematicMaterialSet.terrainRemovable(settings),
                null,
                null
        );
    }

    public static SchematicTerrainAdapter from(
            SchematicTerrainMaterialsSettings settings,
            World world,
            int pasteX,
            int pasteZ,
            List<SchematicFloorColumn> floorColumns
    ) {
        Material anchor = SchematicTerrainAnchor.resolveDominantSurface(world, pasteX, pasteZ, floorColumns);
        return new SchematicTerrainAdapter(
                SchematicMaterialSet.terrainNaturalTop(settings),
                SchematicMaterialSet.terrainRemovable(settings),
                anchor,
                SchematicTerrainAnchor.subsurfaceFor(anchor)
        );
    }

    public static boolean needsPerimeterAdapt(SchematicPlacementSettings placement) {
        return placement.terrainAdaptBlocks > 0;
    }

    public static boolean needsApproachAdapt(SchematicPlacementSettings placement) {
        int approachRing = Math.max(0, placement.terrainApproachRing);
        int frontDepth = Math.max(0, placement.terrainApproachFrontDepth);
        if (approachRing <= 0 && frontDepth <= 0) {
            return false;
        }
        return placement.terrainAdaptBlocks > 0 || placement.terrainApproachTrimOnly;
    }

    public boolean canAdaptAll(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement
    ) {
        int limit = Math.max(1, placement.terrainAdaptBlocks);
        if (needsPerimeterAdapt(placement)) {
            for (SchematicFloorColumn column : SchematicFloorSupport.footprintAdaptColumns(metadata.floorColumns())) {
                if (!canAdaptColumn(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz(),
                        pasteY + column.floorDy(),
                        limit
                )) {
                    return false;
                }
            }
        }
        if (needsApproachAdapt(placement)) {
            int approachRing = Math.max(0, placement.terrainApproachRing);
            int frontDepth = Math.max(0, placement.terrainApproachFrontDepth);
            for (SchematicApproachColumn column : SchematicFloorSupport.approachAdaptColumns(
                    metadata.floorColumns(),
                    metadata,
                    approachRing,
                    frontDepth,
                    placement.approachFrontFacing
            )) {
                if (!canAdaptApproachColumn(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz(),
                        pasteY,
                        column,
                        SchematicFloorSupport.approachAdaptRingDepth(column, approachRing, frontDepth),
                        limit,
                        placement,
                        pasteX,
                        pasteZ
                )) {
                    return false;
                }
            }
        }
        return true;
    }

    public int adapt(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement
    ) {
        int limit = Math.max(1, placement.terrainAdaptBlocks);
        int changed = 0;
        if (needsPerimeterAdapt(placement)) {
            for (SchematicFloorColumn column : SchematicFloorSupport.footprintAdaptColumns(metadata.floorColumns())) {
                changed += adaptColumn(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz(),
                        pasteY + column.floorDy(),
                        limit
                );
            }
        }
        if (needsApproachAdapt(placement)) {
            int approachRing = Math.max(0, placement.terrainApproachRing);
            int frontDepth = Math.max(0, placement.terrainApproachFrontDepth);
            for (SchematicApproachColumn column : SchematicFloorSupport.approachAdaptColumns(
                    metadata.floorColumns(),
                    metadata,
                    approachRing,
                    frontDepth,
                    placement.approachFrontFacing
            )) {
                changed += adaptApproachColumn(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz(),
                        pasteY,
                        column,
                        SchematicFloorSupport.approachAdaptRingDepth(column, approachRing, frontDepth),
                        limit,
                        placement,
                        pasteX,
                        pasteZ
                );
            }
        }
        return changed;
    }

    public int adaptApproachColumn(
            World world,
            int x,
            int z,
            int pasteY,
            SchematicApproachColumn column,
            int ringDepth,
            int limit,
            SchematicPlacementSettings placement,
            int pasteSeedX,
            int pasteSeedZ
    ) {
        int edgeWorldY = pasteY + column.edgeReferenceDy();
        int naturalY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        int targetWorldY = approachTargetY(edgeWorldY, naturalY, column.ringDistance(), ringDepth);
        int approachLimit = approachAdaptLimit(limit, placement);
        if (naturalY < targetWorldY) {
            int delta = targetWorldY - naturalY;
            if (delta > approachLimit) {
                return 0;
            }
            return fillUp(world, x, z, naturalY, targetWorldY);
        }
        if (placement.terrainApproachTrimOnly) {
            return raggedTrimExcess(world, x, z, targetWorldY, naturalY, placement.terrainApproachRaggedDensity, pasteSeedX, pasteSeedZ);
        }
        if (naturalY > targetWorldY) {
            int delta = naturalY - targetWorldY;
            if (delta > approachLimit) {
                return 0;
            }
            int changed = shaveDown(world, x, z, targetWorldY);
            int after = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
            if (after > targetWorldY) {
                changed += capSurfaceDown(world, x, z, targetWorldY, after);
            }
            return changed;
        }
        return 0;
    }

    private int raggedTrimExcess(
            World world,
            int x,
            int z,
            int targetY,
            int naturalY,
            float raggedDensity,
            int pasteSeedX,
            int pasteSeedZ
    ) {
        if (naturalY != targetY + 1) {
            return 0;
        }
        if (!shouldRaggedTrim(x, z, pasteSeedX, pasteSeedZ, raggedDensity)) {
            return 0;
        }
        Block bump = world.getBlockAt(x, naturalY, z);
        Material type = bump.getType();
        if (!isCarvable(type) && !isSoftSurface(type)) {
            return 0;
        }
        bump.setType(Material.AIR, false);
        return 1;
    }

    private static boolean shouldRaggedTrim(int x, int z, int seedX, int seedZ, float density) {
        float clamped = Math.max(0.05f, Math.min(0.95f, density));
        long hash = (x * 734287L) ^ (z * 912271L) ^ (seedX * 48271L) ^ (seedZ * 91823L);
        hash = hash * 6364136223846793005L + 1442695040888963407L;
        double roll = ((hash >>> 11) & 0xFFFFL) / 65535.0;
        return roll < clamped;
    }

    public int adaptColumn(World world, int x, int z, int targetFloorY, int limit) {
        int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        if (surfaceY < targetFloorY) {
            int delta = targetFloorY - surfaceY;
            if (delta > limit) {
                return 0;
            }
            return fillUp(world, x, z, surfaceY, targetFloorY);
        }
        if (surfaceY > targetFloorY) {
            int delta = surfaceY - targetFloorY;
            if (delta > limit) {
                return 0;
            }
            int changed = shaveDown(world, x, z, targetFloorY);
            int after = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
            if (after > targetFloorY) {
                changed += capSurfaceDown(world, x, z, targetFloorY, after);
            }
            return changed;
        }
        return 0;
    }

    private int capSurfaceDown(World world, int x, int z, int targetSurfaceY, int currentSurfaceY) {
        Material surface = surfaceFillMaterial(sampleNaturalTop(world, x, z));
        int changed = 0;
        for (int y = targetSurfaceY + 1; y <= currentSurfaceY; y++) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (type.isAir() || isSoftSurface(type)) {
                if (!type.isAir()) {
                    block.setType(Material.AIR, false);
                    changed++;
                }
                continue;
            }
            break;
        }
        Block floor = world.getBlockAt(x, targetSurfaceY, z);
        if (floor.getType() != surface) {
            floor.setType(surface, false);
            changed++;
        }
        return changed;
    }

    private boolean isSoftSurface(Material type) {
        return naturalTop.contains(type) || type == Material.SNOW;
    }

    private boolean canAdaptColumn(World world, int x, int z, int targetFloorY, int limit) {
        int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        return Math.abs(surfaceY - targetFloorY) <= limit;
    }

    private boolean canAdaptApproachColumn(
            World world,
            int x,
            int z,
            int pasteY,
            SchematicApproachColumn column,
            int ringDepth,
            int limit,
            SchematicPlacementSettings placement,
            int pasteSeedX,
            int pasteSeedZ
    ) {
        int edgeWorldY = pasteY + column.edgeReferenceDy();
        int naturalY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        int targetWorldY = approachTargetY(edgeWorldY, naturalY, column.ringDistance(), ringDepth);
        int approachLimit = approachAdaptLimit(limit, placement);
        if (naturalY < targetWorldY) {
            return targetWorldY - naturalY <= approachLimit;
        }
        if (placement.terrainApproachTrimOnly) {
            return naturalY <= targetWorldY + 1;
        }
        return Math.abs(naturalY - targetWorldY) <= approachLimit;
    }

    private static int approachAdaptLimit(int terrainAdaptBlocks, SchematicPlacementSettings placement) {
        int base = Math.max(1, terrainAdaptBlocks);
        int front = Math.max(0, placement.terrainApproachFrontDepth);
        return Math.max(base, front + APPROACH_TERRACE_DEPTH);
    }

    private static int approachTargetY(int edgeWorldY, int naturalY, int ringDistance, int ringDepth) {
        int terraceDepth = Math.min(APPROACH_TERRACE_DEPTH, ringDepth);
        if (ringDistance <= terraceDepth) {
            return edgeWorldY;
        }
        int blendSpan = ringDepth - terraceDepth;
        if (blendSpan <= 0) {
            return edgeWorldY;
        }
        float blend = (float) (ringDistance - terraceDepth) / (blendSpan + 1);
        return Math.round(edgeWorldY + (naturalY - edgeWorldY) * blend);
    }

    private int fillUp(World world, int x, int z, int surfaceY, int targetFloorY) {
        Material sampled = sampleNaturalTop(world, x, z);
        Material surface = surfaceFillMaterial(sampled);
        Material subsurface = subsurfaceFillMaterial(sampled);
        int changed = 0;
        for (int y = surfaceY + 1; y <= targetFloorY; y++) {
            Material material = y == targetFloorY ? surface : subsurface;
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != material) {
                block.setType(material, false);
                changed++;
            }
        }
        return changed;
    }

    private int shaveDown(World world, int x, int z, int targetSurfaceY) {
        int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        if (surfaceY <= targetSurfaceY) {
            return 0;
        }
        Material surface = surfaceFillMaterial(sampleNaturalTop(world, x, z));
        int changed = 0;
        for (int y = surfaceY; y > targetSurfaceY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (type.isAir()) {
                continue;
            }
            if (!isCarvable(type)) {
                break;
            }
            block.setType(Material.AIR, false);
            changed++;
        }
        Block floor = world.getBlockAt(x, targetSurfaceY, z);
        if (floor.isPassable() || floor.isEmpty() || isCarvable(floor.getType())) {
            if (floor.getType() != surface) {
                floor.setType(surface, false);
                changed++;
            }
        }
        return changed;
    }

    private boolean isCarvable(Material type) {
        if (removable.contains(type)) {
            return true;
        }
        return anchorSurface != null && (type == anchorSurface || type == anchorSubsurface);
    }

    Material sampleNaturalTop(World world, int x, int z) {
        int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        Material current = world.getBlockAt(x, surfaceY, z).getType();
        if (naturalTop.contains(current) || removable.contains(current)
                || (anchorSurface != null && (current == anchorSurface || current == anchorSubsurface))) {
            return current;
        }
        for (int ox = -3; ox <= 3; ox++) {
            for (int oz = -3; oz <= 3; oz++) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int y = highestSolidY(world, x + ox, z + oz);
                Material material = world.getBlockAt(x + ox, y, z + oz).getType();
                if (naturalTop.contains(material)) {
                    return material;
                }
            }
        }
        if (anchorSurface != null) {
            return anchorSurface;
        }
        return Material.GRASS_BLOCK;
    }

    Material topMaterial(Material reference) {
        if (reference == Material.DIRT || reference == Material.COARSE_DIRT || reference == Material.ROOTED_DIRT) {
            return anchorSurface != null ? anchorSurface : Material.GRASS_BLOCK;
        }
        return reference;
    }

    Material surfaceFillMaterial(Material sampled) {
        if (sampled != null && !sampled.isAir()) {
            return topMaterial(sampled);
        }
        return anchorSurface != null ? anchorSurface : Material.GRASS_BLOCK;
    }

    private Material subsurfaceFillMaterial(Material sampled) {
        if (sampled != null && !sampled.isAir()) {
            return SchematicTerrainAnchor.subsurfaceFor(topMaterial(sampled));
        }
        return anchorSubsurface != null ? anchorSubsurface : Material.DIRT;
    }

    int highestSolidY(World world, int x, int z) {
        return NaturalSurfaceResolver.groundY(world, x, z);
    }
}
