package bm.b0b0b0.soulevents.airdrop.integration;

import bm.b0b0b0.soulevents.airdrop.config.settings.ArenaWorldGuardSettings;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
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
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class AirDropArenaRegionService implements ArenaRegionService {

    private final Plugin plugin;
    private final Map<UUID, String> regionIds = new HashMap<>();

    public AirDropArenaRegionService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void create(UUID sessionId, Location center, int horizontalRadius, ArenaWorldGuardSettings settings) {
        if (!settings.createTempRegion || !isWorldGuardPresent()) {
            return;
        }
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return;
        }
        String regionId = WorldGuardConstants.AIRDROP_REGION_PREFIX + sessionId.toString().substring(0, 8);
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        int vertical = Math.max(8, settings.verticalRadius);
        BlockVector3 min = BlockVector3.at(
                x - horizontalRadius,
                Math.max(world.getMinHeight(), y - vertical),
                z - horizontalRadius
        );
        BlockVector3 max = BlockVector3.at(
                x + horizontalRadius,
                Math.min(world.getMaxHeight() - 1, y + vertical),
                z + horizontalRadius
        );
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
        region.setPriority(settings.regionPriority);
        applyArenaFlags(region);
        manager.addRegion(region);
        regionIds.put(sessionId, regionId);
        saveAsync(manager);
    }

    public void remove(UUID sessionId) {
        String regionId = regionIds.remove(sessionId);
        if (regionId == null || !isWorldGuardPresent()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
            if (manager == null) {
                continue;
            }
            ProtectedRegion region = manager.getRegion(regionId);
            if (region == null) {
                continue;
            }
            manager.removeRegion(regionId);
            saveAsync(manager);
            return;
        }
    }

    public void shutdown() {
        for (UUID sessionId : regionIds.keySet().stream().toList()) {
            remove(sessionId);
        }
    }

    private void applyArenaFlags(ProtectedRegion region) {
        allow(region, Flags.PVP);
        allow(region, Flags.TNT);
        allow(region, Flags.CREEPER_EXPLOSION);
        allow(region, Flags.OTHER_EXPLOSION);
        allow(region, Flags.FIRE_SPREAD);
        allow(region, Flags.LAVA_FIRE);
        allow(region, Flags.LIGHTER);
        allow(region, Flags.GHAST_FIREBALL);
        allow(region, Flags.BLOCK_BREAK);
        allow(region, Flags.BLOCK_PLACE);
        allow(region, Flags.MOB_DAMAGE);
        allow(region, Flags.ITEM_PICKUP);
        allow(region, Flags.ITEM_DROP);
        allow(region, Flags.USE);
        allow(region, Flags.INTERACT);
        allow(region, Flags.CHEST_ACCESS);
        allow(region, Flags.INVINCIBILITY, StateFlag.State.DENY);
    }

    private static void allow(ProtectedRegion region, StateFlag flag) {
        region.setFlag(flag, StateFlag.State.ALLOW);
    }

    private static void allow(ProtectedRegion region, StateFlag flag, StateFlag.State state) {
        region.setFlag(flag, state);
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
}
