package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.VisualSettings;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class AirDropVisualService {

    private final Plugin plugin;
    private final AirDropMessageService messages;
    private final Map<UUID, ActiveVisual> visuals = new HashMap<>();
    private Function<UUID, Boolean> chestIntactCheck = sessionId -> false;
    private Consumer<UUID> chestLostHandler = sessionId -> {
    };
    private Function<UUID, Optional<Instant>> cleanupAtProvider = sessionId -> Optional.empty();
    private Function<UUID, Boolean> lootedCheck = sessionId -> false;
    private Function<UUID, Boolean> chestGuiEmptyCheck = sessionId -> false;
    private Consumer<UUID> chestGuiEmptiedHandler = sessionId -> {
    };

    public AirDropVisualService(Plugin plugin, AirDropMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void setChestCallbacks(
            Function<UUID, Boolean> chestIntactCheck,
            Consumer<UUID> chestLostHandler,
            Function<UUID, Optional<Instant>> cleanupAtProvider,
            Function<UUID, Boolean> lootedCheck,
            Function<UUID, Boolean> chestGuiEmptyCheck,
            Consumer<UUID> chestGuiEmptiedHandler
    ) {
        this.chestIntactCheck = chestIntactCheck;
        this.chestLostHandler = chestLostHandler;
        this.cleanupAtProvider = cleanupAtProvider;
        this.lootedCheck = lootedCheck;
        this.chestGuiEmptyCheck = chestGuiEmptyCheck;
        this.chestGuiEmptiedHandler = chestGuiEmptiedHandler;
    }

    public void playSpawn(
            AirDropTypeDefinition definition,
            Location anchor,
            UUID sessionId,
            Instant lootableAt,
            Material chestMaterial
    ) {
        AirDropTypeSettings type = definition.settings();
        if (type.visual.spawnEffectsEnabled) {
            playSpawnBurst(anchor, type);
        }
        if (type.visual.hologramEnabled || type.visual.ambientEffectsEnabled) {
            spawnVisuals(definition, blockAnchor(anchor), sessionId, lootableAt);
        }
    }

    public void remove(UUID sessionId) {
        ActiveVisual visual = visuals.remove(sessionId);
        if (visual == null) {
            return;
        }
        visual.task().cancel();
        removeEntity(visual.entityId());
    }

    public void shutdown() {
        for (UUID sessionId : visuals.keySet().stream().toList()) {
            remove(sessionId);
        }
    }

    private void playSpawnBurst(Location anchor, AirDropTypeSettings type) {
        Particle particle = parseParticle(type.visual.spawnParticle);
        Location center = anchor.clone().add(0.5, 1.0, 0.5);
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(
                particle,
                center,
                type.visual.spawnParticleCount,
                0.6,
                0.8,
                0.6,
                0.02
        );
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 24, 0.45, 0.6, 0.45, 0.08);
        world.playSound(anchor, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.4f);
        world.playSound(anchor, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
    }

    private void spawnVisuals(
            AirDropTypeDefinition definition,
            Location blockAnchor,
            UUID sessionId,
            Instant lootableAt
    ) {
        remove(sessionId);
        HologramContext context = new HologramContext(sessionId, definition, blockAnchor.clone(), lootableAt);
        UUID entityId = null;
        if (definition.settings().visual.hologramEnabled) {
            TextDisplay display = createDisplay(context);
            if (display != null) {
                entityId = display.getUniqueId();
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickVisual(sessionId), 10L, 10L);
        visuals.put(sessionId, new ActiveVisual(entityId, task, context));
    }

    private void tickVisual(UUID sessionId) {
        ActiveVisual active = visuals.get(sessionId);
        if (active == null) {
            return;
        }
        if (!Boolean.TRUE.equals(chestIntactCheck.apply(sessionId))) {
            chestLostHandler.accept(sessionId);
        }
        if (!Boolean.TRUE.equals(lootedCheck.apply(sessionId))
                && Boolean.TRUE.equals(chestGuiEmptyCheck.apply(sessionId))) {
            chestGuiEmptiedHandler.accept(sessionId);
        }
        HologramContext context = active.context();
        VisualSettings visual = context.definition().settings().visual;
        Location blockAnchor = context.blockAnchor();
        updateHologram(active, context, visual, blockAnchor);
        if (visual.ambientEffectsEnabled && hasNearbyViewer(blockAnchor)) {
            try {
                playAmbientEffects(blockAnchor, visual);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "Ambient effects failed for airdrop session " + sessionId,
                        exception
                );
            }
        }
    }

    private void updateHologram(ActiveVisual active, HologramContext context, VisualSettings visual, Location blockAnchor) {
        if (!visual.hologramEnabled) {
            return;
        }
        UUID sessionId = context.sessionId();
        if (active.entityId() == null) {
            TextDisplay respawned = createDisplay(context);
            if (respawned != null) {
                visuals.put(sessionId, new ActiveVisual(respawned.getUniqueId(), active.task(), context));
            }
            return;
        }
        TextDisplay display = findDisplay(active.entityId());
        if (display == null || !display.isValid()) {
            TextDisplay respawned = createDisplay(context);
            if (respawned == null) {
                return;
            }
            visuals.put(sessionId, new ActiveVisual(respawned.getUniqueId(), active.task(), context));
            return;
        }
        display.teleport(hologramLocation(blockAnchor, visual.hologramOffsetY));
        display.text(buildHologramText(context));
    }

    private void playAmbientEffects(Location blockAnchor, VisualSettings visual) {
        World world = blockAnchor.getWorld();
        if (world == null || !blockAnchor.getChunk().isLoaded()) {
            return;
        }
        Location base = blockAnchor.clone().add(0.5, 0.85, 0.5);
        Particle ambient = parseParticle(visual.ambientParticle);
        world.spawnParticle(ambient, base.clone().add(0, 0.55, 0), visual.ambientParticleCount, 0.38, 0.28, 0.38, 0.01);
        world.spawnParticle(Particle.END_ROD, base.clone().add(0, 0.35, 0), 2, 0.12, 0.18, 0.12, 0.015);
        world.spawnParticle(Particle.ENCHANT, base.clone().add(0, 0.75, 0), 8, 0.42, 0.35, 0.42, 0.6);
    }

    private boolean hasNearbyViewer(Location blockAnchor) {
        World world = blockAnchor.getWorld();
        if (world == null) {
            return false;
        }
        double radiusSquared = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(blockAnchor) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private TextDisplay createDisplay(HologramContext context) {
        Location blockAnchor = context.blockAnchor();
        World world = blockAnchor.getWorld();
        if (world == null) {
            return null;
        }
        Chunk chunk = blockAnchor.getChunk();
        if (!chunk.isLoaded()) {
            return null;
        }
        double offsetY = context.definition().settings().visual.hologramOffsetY;
        return world.spawn(hologramLocation(blockAnchor, offsetY), TextDisplay.class, entity -> {
            entity.text(buildHologramText(context));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(true);
            entity.setShadowed(true);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(90, 0, 0, 0));
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setPersistent(true);
            entity.setInvulnerable(true);
            entity.setVisibleByDefault(true);
        });
    }

    private static Location hologramLocation(Location blockAnchor, double offsetY) {
        return blockAnchor.clone().add(0.5, offsetY, 0.5);
    }

    private static Location blockAnchor(Location anchor) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX(),
                anchor.getBlockY(),
                anchor.getBlockZ()
        );
    }

    private TextDisplay findDisplay(UUID entityId) {
        Entity entity = Bukkit.getEntity(entityId);
        if (entity instanceof TextDisplay display && display.isValid()) {
            return display;
        }
        return null;
    }

    private void removeEntity(UUID entityId) {
        if (entityId == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null) {
            entity.remove();
        }
    }

    private Component buildHologramText(HologramContext context) {
        AirDropTypeSettings type = context.definition().settings();
        VisualSettings visual = type.visual;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("type_name", resolveTypeNameTag(type.displayNameKey));
        if (Instant.now().isBefore(context.lootableAt())) {
            long seconds = Math.max(0L, Duration.between(Instant.now(), context.lootableAt()).toSeconds());
            placeholders.put("timer", formatTimer(seconds));
            return messages.resolve("airdrop.hologram.body-waiting", placeholders);
        }
        boolean looted = Boolean.TRUE.equals(lootedCheck.apply(context.sessionId()));
        boolean chestEmpty = Boolean.TRUE.equals(chestGuiEmptyCheck.apply(context.sessionId()));
        if (looted || chestEmpty) {
            Optional<Instant> cleanupAt = cleanupAtProvider.apply(context.sessionId());
            if (cleanupAt.isPresent()) {
                long seconds = Math.max(0L, Duration.between(Instant.now(), cleanupAt.get()).toSeconds());
                placeholders.put("despawn_timer", formatTimer(seconds));
                return messages.resolve(visual.hologramLootedDespawnKey, placeholders);
            }
            return messages.resolve(visual.hologramLootedKey, placeholders);
        }
        return messages.resolve(visual.hologramLootableKey, placeholders);
    }

    private String resolveTypeNameTag(String displayNameKey) {
        return messages.resolvePlain(displayNameKey, Map.of());
    }

    private static String formatTimer(long seconds) {
        long minutes = seconds / 60L;
        long rest = seconds % 60L;
        return String.format("%02d:%02d", minutes, rest);
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return Particle.WITCH;
        }
    }

    private record HologramContext(
            UUID sessionId,
            AirDropTypeDefinition definition,
            Location blockAnchor,
            Instant lootableAt
    ) {
    }

    private record ActiveVisual(UUID entityId, BukkitTask task, HologramContext context) {
    }
}
