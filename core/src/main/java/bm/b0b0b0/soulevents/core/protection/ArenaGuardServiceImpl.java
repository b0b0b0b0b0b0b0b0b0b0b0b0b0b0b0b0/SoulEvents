package bm.b0b0b0.soulevents.core.protection;

import bm.b0b0b0.soulevents.api.protection.ArenaGuardService;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ArenaGuardServiceImpl implements ArenaGuardService {

    private final Map<UUID, ProtectedArena> arenas = new HashMap<>();
    private boolean blockFluidGrief;
    private boolean blockBlockGrief;

    public ArenaGuardServiceImpl(PluginConfig config) {
        apply(config);
    }

    @Override
    public boolean canModifyBlock(UUID sessionId, Block block, Player actor) {
        if (!blockBlockGrief) {
            return true;
        }
        ProtectedArena arena = arenas.get(sessionId);
        return arena == null || !arena.contains(block.getLocation());
    }

    @Override
    public boolean canPlaceFluid(UUID sessionId, Location location, Player actor) {
        if (!blockFluidGrief) {
            return true;
        }
        ProtectedArena arena = arenas.get(sessionId);
        return arena == null || !arena.contains(location);
    }

    @Override
    public void protect(UUID sessionId, Location center, int radius) {
        arenas.put(sessionId, new ProtectedArena(center.clone(), radius));
    }

    @Override
    public void unprotect(UUID sessionId) {
        arenas.remove(sessionId);
    }

    @Override
    public void reload() {
    }

    public void reload(PluginConfig config) {
        apply(config);
    }

    private void apply(PluginConfig config) {
        this.blockFluidGrief = config.protection().blockFluidGrief;
        this.blockBlockGrief = config.protection().blockBlockGrief;
    }

    private record ProtectedArena(Location center, int radius) {

        private boolean contains(Location location) {
            if (!center.getWorld().equals(location.getWorld())) {
                return false;
            }
            return center.distanceSquared(location) <= radius * radius;
        }
    }
}
