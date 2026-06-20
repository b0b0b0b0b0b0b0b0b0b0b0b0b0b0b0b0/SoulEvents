package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicTerrainMaterialsSettings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public final class SchematicTerrainAdapter {

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
            int approachRing = SchematicApproachSupport.resolveEffectiveApproachRing(
                    world, pasteX, pasteY, pasteZ, metadata.floorColumns(), placement
            );
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
                        approachRing,
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
            int approachRing = SchematicApproachSupport.resolveEffectiveApproachRing(
                    world, pasteX, pasteY, pasteZ, metadata.floorColumns(), placement
            );
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
                        approachRing,
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
            int effectiveApproachRing,
            int pasteSeedX,
            int pasteSeedZ
    ) {
        int edgeWorldY = pasteY + column.edgeReferenceDy();
        int naturalY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        int targetWorldY = SchematicApproachSupport.approachTargetY(
                edgeWorldY, naturalY, column.ringDistance(), ringDepth
        );
        int approachLimit = SchematicApproachSupport.approachAdaptLimit(limit, placement, effectiveApproachRing);
        if (naturalY < targetWorldY) {
            int delta = targetWorldY - naturalY;
            if (delta > approachLimit) {
                return 0;
            }
            return fillUp(world, x, z, naturalY, targetWorldY);
        }
        if (placement.terrainApproachTrimOnly) {
            if (naturalY <= targetWorldY || placement.terrainPerimeterRaggedTrim) {
                return 0;
            }
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
        int delta = naturalY - targetY;
        if (delta < 1 || delta > 2) {
            return 0;
        }
        int changed = 0;
        for (int y = naturalY; y > targetY; y--) {
            if (!SchematicRaggedEdgeSupport.shouldRaggedTrim(x, z, y, pasteSeedX, pasteSeedZ, raggedDensity)) {
                continue;
            }
            Block bump = world.getBlockAt(x, y, z);
            Material type = bump.getType();
            if (!isPerimeterRaggableMaterial(type)) {
                break;
            }
            bump.setType(Material.AIR, false);
            changed++;
        }
        return changed;
    }

    boolean isPerimeterRaggableMaterial(Material type) {
        if (type.isAir()) {
            return false;
        }
        if (isSoftSurface(type) || naturalTop.contains(type)) {
            return true;
        }
        return anchorSurface != null && (type == anchorSurface || type == anchorSubsurface);
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
            int effectiveApproachRing,
            int pasteSeedX,
            int pasteSeedZ
    ) {
        int edgeWorldY = pasteY + column.edgeReferenceDy();
        int naturalY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        int targetWorldY = SchematicApproachSupport.approachTargetY(
                edgeWorldY, naturalY, column.ringDistance(), ringDepth
        );
        int approachLimit = SchematicApproachSupport.approachAdaptLimit(limit, placement, effectiveApproachRing);
        if (naturalY < targetWorldY) {
            return targetWorldY - naturalY <= approachLimit;
        }
        if (placement.terrainApproachTrimOnly) {
            return naturalY <= targetWorldY + 2;
        }
        return Math.abs(naturalY - targetWorldY) <= approachLimit;
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
