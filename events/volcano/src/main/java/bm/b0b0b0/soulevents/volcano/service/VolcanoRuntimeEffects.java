package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.volcano.config.VolcanoTypeDefinition;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoEruptionSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoVisualSettings;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class VolcanoRuntimeEffects {

    private final Plugin plugin;
    private final VolcanoMessageService messages;
    private final VolcanoSessionRegistry sessionRegistry;
    private final Map<UUID, List<Location>> magmaSmokePoints = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> magmaSmokeCursor = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> magmaSmokePulse = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> magmaSmokeScanAttempts = new ConcurrentHashMap<>();

    public VolcanoRuntimeEffects(
            Plugin plugin,
            VolcanoMessageService messages,
            VolcanoSessionRegistry sessionRegistry
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.sessionRegistry = sessionRegistry;
    }

    public void start(UUID sessionId, VolcanoTypeDefinition definition, VolcanoSessionRegistry.SessionRecord record) {
        magmaSmokePoints.remove(sessionId);
        magmaSmokeCursor.remove(sessionId);
        magmaSmokePulse.remove(sessionId);
        magmaSmokeScanAttempts.remove(sessionId);
        var task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(sessionId, definition), 1L, 1L);
        sessionRegistry.assignEffectsTask(sessionId, task);
    }

    public void stop(UUID sessionId) {
        magmaSmokePoints.remove(sessionId);
        magmaSmokeCursor.remove(sessionId);
        magmaSmokePulse.remove(sessionId);
        magmaSmokeScanAttempts.remove(sessionId);
        sessionRegistry.find(sessionId).ifPresent(record -> {
            if (record.effectsTask() != null) {
                record.effectsTask().cancel();
            }
        });
    }

    public TextDisplay spawnItemLabel(
            World world,
            Location anchor,
            VolcanoVisualSettings visual,
            VolcanoTypeDefinition definition
    ) {
        if (!visual.itemLabelEnabled || visual.itemLabelKeys.isEmpty()) {
            return null;
        }
        String key = visual.itemLabelKeys.get(ThreadLocalRandom.current().nextInt(visual.itemLabelKeys.size()));
        Component text = messages.resolve(key, Map.of("type_name", resolveTypeName(definition)));
        Location labelLocation = anchor.clone().add(0, visual.itemLabelOffsetY, 0);
        return world.spawn(labelLocation, TextDisplay.class, display -> {
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(visual.itemLabelSeeThrough);
            display.setShadowed(true);
            display.setDefaultBackground(false);
        });
    }

    public Item launchLootItem(
            Location vent,
            ItemStack stack,
            VolcanoEruptionSettings eruption
    ) {
        World world = vent.getWorld();
        if (world == null) {
            return null;
        }
        Location spawn = vent.clone().add(0.5, 1.15, 0.5);
        Item entity = world.dropItem(spawn, stack);
        entity.setPickupDelay(0);
        entity.setCanMobPickup(false);
        entity.setGravity(true);
        entity.setVelocity(randomLaunchVelocity(eruption));
        entity.setPersistent(false);
        return entity;
    }

    private void tick(UUID sessionId, VolcanoTypeDefinition definition) {
        sessionRegistry.find(sessionId).ifPresent(record -> {
            VolcanoVisualSettings visual = definition.settings().visual;
            VolcanoEruptionSettings eruption = definition.settings().eruption;
            spawnVentSmoke(record.ventAnchor(), visual);
            if (visual.magmaSmokeEnabled) {
                spawnMagmaSmoke(sessionId, record, visual);
            }
            updateLootVisuals(record, eruption, visual);
        });
    }

    private void spawnVentSmoke(Location vent, VolcanoVisualSettings visual) {
        if (!visual.ventSmokeEnabled || vent.getWorld() == null) {
            return;
        }
        Particle particle = resolveParticle(visual.ventSmokeParticle, Particle.CAMPFIRE_COSY_SMOKE);
        Location center = vent.clone().add(0.5, 1.0, 0.5);
        World world = vent.getWorld();
        for (int index = 0; index < visual.ventSmokeCount; index++) {
            double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.35;
            double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.35;
            world.spawnParticle(
                    particle,
                    center.getX() + offsetX,
                    center.getY(),
                    center.getZ() + offsetZ,
                    1,
                    0.02,
                    0.08,
                    0.02,
                    0.01
            );
        }
    }

    private void spawnMagmaSmoke(UUID sessionId, VolcanoSessionRegistry.SessionRecord record, VolcanoVisualSettings visual) {
        if (record.ventAnchor().getWorld() == null) {
            return;
        }
        World world = record.ventAnchor().getWorld();
        List<Location> points = magmaSmokePoints.get(sessionId);
        if (points == null || points.isEmpty()) {
            if (record.schematicBounds().isPresent()) {
                points = scanMagmaSmokePoints(world, record.schematicBounds().get());
                if (!points.isEmpty()) {
                    magmaSmokePoints.put(sessionId, points);
                    magmaSmokeScanAttempts.remove(sessionId);
                } else {
                    int attempts = magmaSmokeScanAttempts.merge(sessionId, 1, Integer::sum);
                    if (attempts >= 200) {
                        magmaSmokePoints.put(sessionId, List.of());
                    }
                }
            }
        }
        if (points == null || points.isEmpty()) {
            return;
        }
        int interval = Math.max(1, visual.magmaSmokeIntervalTicks);
        int pulse = magmaSmokePulse.merge(sessionId, 1, Integer::sum);
        if (pulse % interval != 0) {
            return;
        }
        Particle primary = resolveParticle(visual.magmaSmokeParticle, Particle.CAMPFIRE_COSY_SMOKE);
        Particle secondary = resolveParticle(visual.magmaSmokeSecondaryParticle, Particle.LARGE_SMOKE);
        Particle dark = resolveParticle(visual.magmaSmokeDarkParticle, Particle.SMOKE);
        List<Location> batch = selectMagmaSmokeBatch(sessionId, points, visual);
        for (Location point : batch) {
            spawnMagmaSmokePlume(world, point, visual, primary, secondary, dark);
        }
    }

    private List<Location> selectMagmaSmokeBatch(
            UUID sessionId,
            List<Location> points,
            VolcanoVisualSettings visual
    ) {
        if (points.isEmpty()) {
            return List.of();
        }
        int sampleCount = visual.magmaSmokeCount;
        if (sampleCount <= 0 || sampleCount >= points.size()) {
            int maxPoints = visual.magmaSmokeMaxPointsPerTick;
            if (maxPoints <= 0) {
                maxPoints = Math.min(4, points.size());
            }
            if (maxPoints >= points.size()) {
                return points;
            }
            int cursor = magmaSmokeCursor.getOrDefault(sessionId, 0) % points.size();
            magmaSmokeCursor.put(sessionId, cursor + maxPoints);
            List<Location> batch = new ArrayList<>(maxPoints);
            for (int index = 0; index < maxPoints; index++) {
                batch.add(points.get((cursor + index) % points.size()));
            }
            return batch;
        }
        List<Location> batch = new ArrayList<>(sampleCount);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < sampleCount; index++) {
            batch.add(points.get(random.nextInt(points.size())));
        }
        return batch;
    }

    private static void spawnMagmaSmokePlume(
            World world,
            Location base,
            VolcanoVisualSettings visual,
            Particle primary,
            Particle secondary,
            Particle dark
    ) {
        double half = Math.max(0.05, visual.magmaSmokeCubeHalfSize);
        double horizontal = Math.max(0.05, visual.magmaSmokeSpread);
        double vertical = Math.max(0.2, visual.magmaSmokeVerticalSpread);
        double rise = visual.magmaSmokeRiseSpeed;
        int stacks = Math.max(1, visual.magmaSmokeVerticalLayers);
        int count = Math.max(1, visual.magmaSmokeParticlesPerPlume);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double offsetX = (random.nextDouble() - 0.5) * half;
        double offsetZ = (random.nextDouble() - 0.5) * half;
        for (int stack = 0; stack < stacks; stack++) {
            double x = base.getX() + offsetX;
            double z = base.getZ() + offsetZ;
            double y = base.getY() + visual.magmaSmokeSpawnOffsetY + stack * 0.45;
            spawnSmokeCluster(world, dark, x, y, z, count, horizontal, vertical, rise * 0.75);
            spawnSmokeCluster(world, secondary, x, y + 0.15, z, Math.max(1, count - 1), horizontal * 0.85, vertical * 1.1, rise);
            spawnSmokeCluster(
                    world,
                    primary,
                    x,
                    y + 0.3,
                    z,
                    Math.max(1, count / 2),
                    horizontal * 0.7,
                    vertical * 1.25,
                    rise * 1.2
            );
        }
    }

    private static void spawnSmokeCluster(
            World world,
            Particle particle,
            double x,
            double y,
            double z,
            int count,
            double spreadHorizontal,
            double spreadVertical,
            double riseSpeed
    ) {
        world.spawnParticle(
                particle,
                x,
                y,
                z,
                count,
                spreadHorizontal,
                spreadVertical,
                spreadHorizontal,
                riseSpeed
        );
    }

    private List<Location> scanMagmaSmokePoints(World world, SchematicWorldBounds bounds) {
        List<Location> points = new ArrayList<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (type == Material.MAGMA_BLOCK) {
                        points.add(new Location(world, x + 0.5, y + 1.05, z + 0.5));
                        continue;
                    }
                    if (type != Material.LAVA) {
                        continue;
                    }
                    Block above = world.getBlockAt(x, y + 1, z);
                    if (above.isPassable() || above.isEmpty()) {
                        points.add(new Location(world, x + 0.5, y + 1.05, z + 0.5));
                    }
                }
            }
        }
        return List.copyOf(points);
    }

    private void updateLootVisuals(
            VolcanoSessionRegistry.SessionRecord record,
            VolcanoEruptionSettings eruption,
            VolcanoVisualSettings visual
    ) {
        World world = record.ventAnchor().getWorld();
        if (world == null) {
            return;
        }
        Particle primary = resolveParticle(eruption.trailParticle, Particle.FLAME);
        Particle secondary = resolveParticle(eruption.trailSecondaryParticle, Particle.LAVA);
        double labelOffset = visual.itemLabelOffsetY;
        for (VolcanoSessionRegistry.LootItem lootItem : record.lootItems()) {
            if (lootItem.claimed()) {
                continue;
            }
            Item entity = findItem(world, lootItem.entityId());
            if (entity == null || entity.isDead()) {
                continue;
            }
            Location location = entity.getLocation();
            world.spawnParticle(
                    primary,
                    location.getX(),
                    location.getY() + 0.15,
                    location.getZ(),
                    eruption.trailParticleCount,
                    0.08,
                    0.05,
                    0.08,
                    0.01
            );
            if (eruption.trailSecondaryCount > 0) {
                world.spawnParticle(
                        secondary,
                        location.getX(),
                        location.getY() + 0.1,
                        location.getZ(),
                        eruption.trailSecondaryCount,
                        0.05,
                        0.03,
                        0.05,
                        0.0
                );
            }
            if (lootItem.labelId() != null) {
                TextDisplay label = findLabel(world, lootItem.labelId());
                if (label != null && !label.isDead()) {
                    label.teleport(location.clone().add(0, labelOffset, 0));
                }
            }
        }
    }

    private static Item findItem(World world, UUID entityId) {
        return world.getEntities().stream()
                .filter(entity -> entity.getUniqueId().equals(entityId))
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static TextDisplay findLabel(World world, UUID entityId) {
        return world.getEntities().stream()
                .filter(entity -> entity.getUniqueId().equals(entityId))
                .filter(TextDisplay.class::isInstance)
                .map(TextDisplay.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static Vector randomLaunchVelocity(VolcanoEruptionSettings eruption) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double reachMin = Math.max(0.0, eruption.horizontalReachMin);
        double reachMax = Math.max(reachMin, eruption.horizontalReachMax);
        double reach = random.nextDouble(reachMin, reachMax);
        double verticalMin = eruption.launchPowerMin;
        double verticalMax = Math.max(verticalMin, eruption.launchPowerMax);
        double vertical = random.nextDouble(verticalMin, verticalMax);
        double divisor = eruption.horizontalSpeedDivisor <= 0.0 ? 13.5 : eruption.horizontalSpeedDivisor;
        double horizontalSpeed = reach / divisor + eruption.horizontalSpeedBias;
        return new Vector(
                Math.cos(angle) * horizontalSpeed,
                vertical,
                Math.sin(angle) * horizontalSpeed
        );
    }

    private static Particle resolveParticle(String name, Particle fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Particle.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private String resolveTypeName(VolcanoTypeDefinition definition) {
        return messages.resolvePlain(definition.settings().displayNameKey, Map.of());
    }
}
