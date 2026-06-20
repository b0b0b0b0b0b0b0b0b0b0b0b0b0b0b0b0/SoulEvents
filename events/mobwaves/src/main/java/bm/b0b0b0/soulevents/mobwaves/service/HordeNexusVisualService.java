package bm.b0b0b0.soulevents.mobwaves.service;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class HordeNexusVisualService {

    private final Plugin plugin;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public HordeNexusVisualService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start(UUID sessionId, Location anchor) {
        stop(sessionId);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> pulse(sessionId, anchor),
                0L,
                8L
        );
        tasks.put(sessionId, task);
    }

    public void stop(UUID sessionId) {
        BukkitTask task = tasks.remove(sessionId);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    private void pulse(UUID sessionId, Location anchor) {
        if (!tasks.containsKey(sessionId)) {
            return;
        }
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        Location center = anchor.clone().add(0.0, 0.5, 0.0);
        world.spawnParticle(Particle.PORTAL, center, 16, 0.45, 0.6, 0.45, 0.02);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 6, 0.25, 0.35, 0.25, 0.01);
        if (ThreadLocalRandom.current().nextInt(4) == 0) {
            world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.35f, 0.7f + ThreadLocalRandom.current().nextFloat() * 0.3f);
        }
    }
}
