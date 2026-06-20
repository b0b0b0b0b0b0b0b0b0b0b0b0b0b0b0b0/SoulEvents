package bm.b0b0b0.soulevents.mobwaves.listener;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.gate.ArenaRegionGuard;
import bm.b0b0b0.soulevents.mobwaves.integration.ArenaRegionService;
import bm.b0b0b0.soulevents.mobwaves.service.MobHordeService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public final class ArenaRegionEnforcementListener implements Listener {

    private final ArenaRegionGuard guard;

    public ArenaRegionEnforcementListener(
            MobHordeService service,
            MobWavesPluginConfig config,
            ArenaRegionService arenaRegions
    ) {
        this.guard = new ArenaRegionGuard(service, config, arenaRegions);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Material bucket = event.getBucket();
        if (bucket != Material.LAVA_BUCKET && bucket != Material.WATER_BUCKET) {
            return;
        }
        if (guard.deniesFluidBuckets(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Material type = block.getType();
        if (type == Material.TNT && guard.denies(block.getLocation(), "tnt")) {
            event.setCancelled(true);
            return;
        }
        if ((type == Material.LAVA || type == Material.WATER) && guard.deniesFluidBuckets(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onFluidFlow(BlockFromToEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.WATER && guard.denies(event.getToBlock().getLocation(), "water-flow")) {
            event.setCancelled(true);
            return;
        }
        if (type == Material.LAVA && guard.denies(event.getToBlock().getLocation(), "lava-flow")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.LAVA) {
            return;
        }
        if (guard.denies(event.getBlock().getLocation(), "lava-fire")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed)) {
            return;
        }
        if (guard.denies(event.getEntity().getLocation(), "tnt")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        String flag = explosionFlag(event.getEntity());
        if (flag == null) {
            return;
        }
        if (guard.denies(event.getLocation(), flag)) {
            event.setCancelled(true);
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEndermanGrief(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.ENDERMAN) {
            return;
        }
        if (guard.denies(event.getBlock().getLocation(), "enderman-grief")) {
            event.setCancelled(true);
        }
    }

    private static String explosionFlag(Entity entity) {
        if (entity instanceof TNTPrimed) {
            return "tnt";
        }
        if (entity instanceof Creeper) {
            return "creeper-explosion";
        }
        if (entity instanceof Fireball || entity.getType() == EntityType.DRAGON_FIREBALL) {
            return "ghast-fireball";
        }
        if (entity instanceof Wither || entity instanceof WitherSkull) {
            return "wither-damage";
        }
        if (entity instanceof EnderDragon) {
            return "enderdragon-block-damage";
        }
        if (entity.getType() == EntityType.WIND_CHARGE) {
            return "breeze-wind-charge";
        }
        return "other-explosion";
    }
}

