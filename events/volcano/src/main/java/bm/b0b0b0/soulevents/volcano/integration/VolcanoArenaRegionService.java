package bm.b0b0b0.soulevents.volcano.integration;

import bm.b0b0b0.soulevents.volcano.config.settings.ArenaWorldGuardSettings;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class VolcanoArenaRegionService implements ArenaRegionService {

    private final Plugin plugin;
    private final Map<UUID, RegionRef> regions = new HashMap<>();

    public VolcanoArenaRegionService(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void create(
            UUID sessionId,
            Location anchor,
            ArenaWorldGuardSettings settings,
            Optional<SchematicWorldBounds> schematicBounds
    ) {
        if (!settings.createTempRegion || !isWorldGuardPresent()) {
            return;
        }
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return;
        }

        String regionId = WorldGuardConstants.volcano_REGION_PREFIX + sessionId.toString().substring(0, 8);
        RegionRef existing = regions.get(sessionId);
        if (existing != null && existing.regionId().equals(regionId) && existing.worldName().equals(world.getName())) {
            ProtectedRegion current = manager.getRegion(regionId);
            if (current != null) {
                return;
            }
        } else if (existing != null) {
            remove(sessionId);
        }

        BlockVector3 min;
        BlockVector3 max;
        if (schematicBounds.isPresent()) {
            SchematicWorldBounds bounds = schematicBounds.get();
            int margin = Math.max(0, settings.marginWithSchematic);
            min = BlockVector3.at(bounds.minX() - margin, bounds.minY(), bounds.minZ() - margin);
            max = BlockVector3.at(bounds.maxX() + margin, bounds.maxY(), bounds.maxZ() + margin);
        } else {
            int margin = Math.max(1, settings.marginWithoutSchematic);
            int x = anchor.getBlockX();
            int y = anchor.getBlockY();
            int z = anchor.getBlockZ();
            min = BlockVector3.at(x - margin, y, z - margin);
            max = BlockVector3.at(x + margin, y, z + margin);
        }
        if (settings.expandVertical) {
            min = BlockVector3.at(min.x(), world.getMinHeight(), min.z());
            max = BlockVector3.at(max.x(), world.getMaxHeight() - 1, max.z());
        } else {
            int verticalMargin = Math.max(0, settings.verticalMargin);
            min = BlockVector3.at(
                    min.x(),
                    Math.max(world.getMinHeight(), min.y() - verticalMargin),
                    min.z()
            );
            max = BlockVector3.at(
                    max.x(),
                    Math.min(world.getMaxHeight() - 1, max.y() + verticalMargin),
                    max.z()
            );
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
        region.setPriority(settings.regionPriority);
        applyArenaFlags(region, settings);
        manager.addRegion(region);
        ArenaRegionBounds bounds = new ArenaRegionBounds(
                world.getName(),
                min.x(),
                min.y(),
                min.z(),
                max.x(),
                max.y(),
                max.z()
        );
        regions.put(sessionId, new RegionRef(world.getName(), regionId, bounds));
        saveAsync(manager);
    }

    @Override
    public Optional<UUID> sessionAt(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        for (var entry : regions.entrySet()) {
            if (entry.getValue().bounds().contains(location)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    @Override
    public void remove(UUID sessionId) {
        RegionRef ref = regions.remove(sessionId);
        if (ref == null || !isWorldGuardPresent()) {
            return;
        }
        World world = Bukkit.getWorld(ref.worldName());
        if (world == null) {
            return;
        }
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return;
        }
        ProtectedRegion region = manager.getRegion(ref.regionId());
        if (region == null) {
            return;
        }
        manager.removeRegion(ref.regionId());
        saveAsync(manager);
    }

    @Override
    public void shutdown() {
        for (UUID sessionId : regions.keySet().stream().toList()) {
            remove(sessionId);
        }
    }

    private void applyArenaFlags(ProtectedRegion region, ArenaWorldGuardSettings settings) {
        if (settings.allowFlags == null || settings.allowFlags.isEmpty()) {
            allowAllStateFlags(region);
        } else {
            applyStateFlags(region, settings.allowFlags, StateFlag.State.ALLOW);
        }
        applyStateFlags(region, settings.denyFlags, StateFlag.State.DENY);
        if (settings.allowFlags != null && !settings.allowFlags.isEmpty()) {
            applyStateFlags(region, settings.allowFlags, StateFlag.State.ALLOW);
        }
    }

    private static void allowAllStateFlags(ProtectedRegion region) {
        for (Flag<?> flag : WorldGuard.getInstance().getFlagRegistry()) {
            if (flag instanceof StateFlag stateFlag) {
                region.setFlag(stateFlag, StateFlag.State.ALLOW);
            }
        }
    }

    private static void applyStateFlags(ProtectedRegion region, List<String> flagNames, StateFlag.State state) {
        if (flagNames == null || flagNames.isEmpty()) {
            return;
        }
        for (String flagName : flagNames) {
            if (flagName == null || flagName.isBlank()) {
                continue;
            }
            Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(flagName.trim());
            if (flag instanceof StateFlag stateFlag) {
                region.setFlag(stateFlag, state);
            }
        }
    }

    private void saveAsync(RegionManager manager) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> save(manager));
    }

    private void save(RegionManager manager) {
        try {
            manager.saveChanges();
        } catch (StorageException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save WorldGuard region: " + exception.getMessage());
        }
    }

    private static boolean isWorldGuardPresent() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    private record RegionRef(String worldName, String regionId, ArenaRegionBounds bounds) {
    }
}

