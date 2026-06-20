package bm.b0b0b0.soulevents.mobwaves.listener;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.message.MobWavesRuntimeLog;
import bm.b0b0b0.soulevents.mobwaves.service.MobWaveService;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntityTags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;
import java.util.UUID;

public final class MobWaveListener implements Listener {

    private final MobWavesPlugin plugin;
    private final MobWaveService service;
    private MobWavesPluginConfig config;

    public MobWaveListener(MobWavesPlugin plugin, MobWaveService service, MobWavesPluginConfig config) {
        this.plugin = plugin;
        this.service = service;
        this.config = config;
    }

    public void reload(MobWavesPluginConfig config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (MobWaveEntityTags.sessionId(plugin, event.getEntity()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        event.getEntity().setFireTicks(0);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        LivingEntity damager = resolveDamager(event.getDamager());
        if (!(event.getEntity() instanceof LivingEntity victim) || damager == null) {
            return;
        }
        Optional<UUID> victimSession = MobWaveEntityTags.sessionId(plugin, victim);
        Optional<UUID> damagerSession = MobWaveEntityTags.sessionId(plugin, damager);
        if (victimSession.isEmpty() || damagerSession.isEmpty()
                || !victimSession.get().equals(damagerSession.get())) {
            return;
        }
        event.setCancelled(true);
        MobWavesRuntimeLog.combat(
                plugin,
                config,
                "friendly-fire blocked session=" + victimSession.get()
                        + " victim=" + victim.getType().name()
                        + " damager=" + damager.getType().name()
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAllowHordeMobPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        LivingEntity damager = resolveDamager(event.getDamager());
        if (damager == null) {
            return;
        }
        Optional<UUID> sessionId = MobWaveEntityTags.sessionId(plugin, damager);
        if (sessionId.isEmpty()) {
            return;
        }
        if (!event.isCancelled()) {
            return;
        }
        event.setCancelled(false);
        MobWavesRuntimeLog.combat(
                plugin,
                config,
                "mob->player uncancelled session=" + sessionId.get()
                        + " mob=" + damager.getType().name()
                        + " player=" + player.getName()
                        + " raw=" + formatDamage(event.getDamage())
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCombatLog(EntityDamageByEntityEvent event) {
        LivingEntity damager = resolveDamager(event.getDamager());
        if (!(event.getEntity() instanceof LivingEntity victim) || damager == null) {
            return;
        }
        Optional<UUID> victimSession = MobWaveEntityTags.sessionId(plugin, victim);
        Optional<UUID> damagerSession = MobWaveEntityTags.sessionId(plugin, damager);

        if (damagerSession.isPresent() && victim instanceof Player player) {
            MobWavesRuntimeLog.combat(
                    plugin,
                    config,
                    "mob->player session=" + damagerSession.get()
                            + " mob=" + damager.getType().name()
                            + " player=" + player.getName()
                            + " raw=" + formatDamage(event.getDamage())
                            + " final=" + formatDamage(event.getFinalDamage())
                            + " cancelled=" + event.isCancelled()
            );
            return;
        }
        if (victimSession.isPresent() && damager instanceof Player player) {
            MobWavesRuntimeLog.combat(
                    plugin,
                    config,
                    "player->mob session=" + victimSession.get()
                            + " player=" + player.getName()
                            + " mob=" + victim.getType().name()
                            + " raw=" + formatDamage(event.getDamage())
                            + " final=" + formatDamage(event.getFinalDamage())
                            + " mobHpLeft=" + formatDamage(Math.max(0.0, victim.getHealth() - event.getFinalDamage()))
                            + " cancelled=" + event.isCancelled()
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Optional<UUID> sessionId = MobWaveEntityTags.sessionId(plugin, event.getEntity());
        if (sessionId.isEmpty()) {
            return;
        }
        var location = event.getEntity().getLocation();
        MobWavesRuntimeLog.wave(
                plugin,
                config,
                "mob death session=" + sessionId.get()
                        + " type=" + event.getEntityType().name()
                        + " at " + location.getBlockX() + ","
                        + location.getBlockY() + ","
                        + location.getBlockZ()
        );
        service.onMobDeath(sessionId.get(), event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        Optional<UUID> sessionId = MobWaveEntityTags.sessionId(plugin, event.getEntity());
        if (sessionId.isEmpty()) {
            return;
        }
        if (event.isCancelled()) {
            MobWavesRuntimeLog.warn(
                    plugin,
                    "spawn cancelled by another plugin session=" + sessionId.get()
                            + " type=" + event.getEntityType().name()
                            + " at " + event.getLocation().getBlockX() + ","
                            + event.getLocation().getBlockY() + ","
                            + event.getLocation().getBlockZ()
            );
        }
    }

    private static LivingEntity resolveDamager(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    private static String formatDamage(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
