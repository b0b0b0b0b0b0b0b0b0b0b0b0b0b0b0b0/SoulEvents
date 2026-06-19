package bm.b0b0b0.soulevents.core.listener;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProtectionListener implements Listener {

    private static final int ARENA_LOOKUP_RANGE_SQ = 64 * 64;

    private final SoulEventsApi api;

    public ProtectionListener(SoulEventsApi api) {
        this.api = api;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (isBuildProtected(event.getBlock(), events)) {
            event.setCancelled(true);
            api.messages().send(event.getPlayer(), "protection.arena.no-build", Map.of());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (isBuildProtected(event.getBlock(), events)) {
            event.setCancelled(true);
            api.messages().send(event.getPlayer(), "protection.arena.no-build", Map.of());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (isFluidProtected(event.getBlock().getLocation(), events)) {
            event.setCancelled(true);
            api.messages().send(event.getPlayer(), "protection.arena.no-fluid", Map.of());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (isFluidProtected(event.getToBlock().getLocation(), events)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        event.blockList().removeIf(block -> isBuildProtected(block, events));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        event.blockList().removeIf(block -> isBuildProtected(block, events));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (anyBuildProtected(event.getBlocks(), events)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (anyBuildProtected(event.getBlocks(), events)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (isBuildProtected(event.getBlock(), events)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (isBuildProtected(event.getBlock(), events)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Collection<ActiveEvent> events = api.modules().activeEvents();
        if (events.isEmpty()) {
            return;
        }
        if (isBuildProtected(event.getBlock(), events)) {
            event.setCancelled(true);
        }
    }

    private boolean anyBuildProtected(List<Block> blocks, Collection<ActiveEvent> events) {
        for (Block block : blocks) {
            if (isBuildProtected(block, events)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBuildProtected(Block block, Collection<ActiveEvent> events) {
        UUID sessionId = findSession(block.getLocation(), events);
        return sessionId != null && !api.protection().arena().canModifyBlock(sessionId, block, null);
    }

    private boolean isFluidProtected(Location location, Collection<ActiveEvent> events) {
        UUID sessionId = findSession(location, events);
        return sessionId != null && !api.protection().arena().canPlaceFluid(sessionId, location, null);
    }

    private UUID findSession(Location location, Collection<ActiveEvent> events) {
        for (ActiveEvent event : events) {
            Location anchor = event.anchor();
            if (anchor.getWorld().equals(location.getWorld())
                    && anchor.distanceSquared(location) <= ARENA_LOOKUP_RANGE_SQ) {
                return event.sessionId();
            }
        }
        return null;
    }
}
