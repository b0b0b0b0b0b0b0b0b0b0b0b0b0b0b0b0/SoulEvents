package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.volcano.config.VolcanoTypeDefinition;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoVentHeatSettings;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPermissions;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoAmbientSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoEruptionSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoVisualSettings;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class VolcanoRuntimeEffects {

    private final Plugin plugin;
    private final VolcanoMessageService messages;
    private final VolcanoSessionRegistry sessionRegistry;
    private final Map<UUID, List<Location>> magmaSmokePoints = new ConcurrentHashMap<>();
    private final Map<UUID, List<Location>> bedrockGlowPoints = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> magmaSmokeCursor = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> magmaSmokePulse = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> magmaSmokeScanAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bedrockGlowScanAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> nextRumblePulse = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> nextLightningPulse = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lootSlopeRollTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Long>> ventHeatBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ventHeatScanAttempts = new ConcurrentHashMap<>();

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
        bedrockGlowPoints.remove(sessionId);
        magmaSmokeCursor.remove(sessionId);
        magmaSmokePulse.remove(sessionId);
        magmaSmokeScanAttempts.remove(sessionId);
        bedrockGlowScanAttempts.remove(sessionId);
        ventHeatBlocks.remove(sessionId);
        ventHeatScanAttempts.remove(sessionId);
        nextRumblePulse.remove(sessionId);
        nextLightningPulse.remove(sessionId);
        VolcanoAmbientSettings ambient = definition.settings().visual.ambient;
        nextRumblePulse.put(sessionId, randomInterval(ambient.rumbleIntervalMinTicks, ambient.rumbleIntervalMaxTicks));
        nextLightningPulse.put(sessionId, randomInterval(ambient.lightningIntervalMinTicks, ambient.lightningIntervalMaxTicks));
        var task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(sessionId, definition), 1L, 1L);
        sessionRegistry.assignEffectsTask(sessionId, task);
    }

    public void stop(UUID sessionId) {
        magmaSmokePoints.remove(sessionId);
        bedrockGlowPoints.remove(sessionId);
        magmaSmokeCursor.remove(sessionId);
        magmaSmokePulse.remove(sessionId);
        magmaSmokeScanAttempts.remove(sessionId);
        bedrockGlowScanAttempts.remove(sessionId);
        ventHeatBlocks.remove(sessionId);
        ventHeatScanAttempts.remove(sessionId);
        nextRumblePulse.remove(sessionId);
        nextLightningPulse.remove(sessionId);
        sessionRegistry.find(sessionId).ifPresent(record -> {
            for (VolcanoSessionRegistry.LootItem lootItem : record.lootItems()) {
                lootSlopeRollTicks.remove(lootItem.entityId());
            }
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

    public record LootLaunch(Item entity, double angleRadians) {
    }

    public LootLaunch launchLootItem(
            Location vent,
            ItemStack stack,
            VolcanoEruptionSettings eruption,
            Double lastLaunchAngleRad
    ) {
        World world = vent.getWorld();
        if (world == null) {
            return null;
        }
        Location spawn = vent.clone().add(0.0, 1.15, 0.0);
        Item entity = world.dropItem(spawn, stack);
        int pickupDelay = Math.max(0, eruption.lootLaunchPickupDelayTicks);
        entity.setPickupDelay(pickupDelay);
        entity.setCanMobPickup(false);
        entity.setGravity(true);
        double angle = pickLaunchAngle(lastLaunchAngleRad, eruption);
        entity.setVelocity(velocityFromAngle(angle, eruption));
        entity.setPersistent(false);
        return new LootLaunch(entity, angle);
    }

    public void playLootRefillThunder(Location vent) {
        World world = vent.getWorld();
        if (world == null) {
            return;
        }
        Location center = vent.clone().add(0.5, 1.0, 0.5);
        world.strikeLightningEffect(center);
        world.playSound(
                center,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.85f,
                0.72f + ThreadLocalRandom.current().nextFloat() * 0.18f
        );
    }

    private void tick(UUID sessionId, VolcanoTypeDefinition definition) {
        sessionRegistry.find(sessionId).ifPresent(record -> {
            VolcanoVisualSettings visual = definition.settings().visual;
            VolcanoEruptionSettings eruption = definition.settings().eruption;
            int pulse = magmaSmokePulse.merge(sessionId, 1, Integer::sum);
            if (visual.ventSmokeEnabled && pulse % Math.max(1, visual.ventSmokeIntervalTicks) == 0) {
                spawnVentSmoke(record.ventAnchor(), visual);
            }
            if (visual.magmaSmokeEnabled && pulse % Math.max(1, visual.magmaSmokeIntervalTicks) == 0) {
                spawnMagmaSmoke(sessionId, record, visual);
            }
            if (visual.bedrockGlowEnabled) {
                updateBedrockGlows(sessionId, record, eruption, visual);
            }
            tickVentHeat(sessionId, record, visual.ventHeat, pulse);
            if (visual.ambient.enabled) {
                tickAmbient(sessionId, record, visual.ambient, pulse);
            }
            updateLootVisuals(record, eruption, visual, pulse);
        });
    }

    private void spawnVentSmoke(Location vent, VolcanoVisualSettings visual) {
        if (!visual.ventSmokeEnabled || vent.getWorld() == null) {
            return;
        }
        Particle dark = resolveSmokeParticle(visual.magmaSmokeDarkParticle, Particle.CAMPFIRE_SIGNAL_SMOKE);
        Location center = vent.clone().add(0.5, 1.0, 0.5);
        World world = vent.getWorld();
        double spread = clampSpread(visual.ventSmokeSpread, 0.14);
        double drift = clampSpread(visual.ventSmokeVerticalSpread, 0.08);
        double rise = clampRise(visual.ventSmokeRiseSpeed, 0.06);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < visual.ventSmokeCount; index++) {
            double offsetX = (random.nextDouble() - 0.5) * spread;
            double offsetZ = (random.nextDouble() - 0.5) * spread;
            Particle particle = index == visual.ventSmokeCount - 1
                    ? resolveSmokeParticle(visual.magmaSmokeParticle, Particle.LARGE_SMOKE)
                    : dark;
            spawnSmokeCluster(
                    world,
                    particle,
                    center.getX() + offsetX,
                    center.getY(),
                    center.getZ() + offsetZ,
                    1,
                    spread * 0.25,
                    drift,
                    rise
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
        Particle puff = resolveSmokeParticle(visual.magmaSmokeParticle, Particle.LARGE_SMOKE);
        Particle dark = resolveSmokeParticle(visual.magmaSmokeDarkParticle, Particle.CAMPFIRE_SIGNAL_SMOKE);
        List<Location> batch = selectMagmaSmokeBatch(sessionId, points, visual);
        for (Location point : batch) {
            spawnMagmaSmokePlume(world, point, visual, puff, dark);
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
                maxPoints = points.size();
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
            Particle puff,
            Particle dark
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double half = Math.max(0.02, visual.magmaSmokeCubeHalfSize);
        double horizontal = clampSpread(visual.magmaSmokeSpread, 0.09);
        double drift = clampSpread(visual.magmaSmokeVerticalSpread, 0.07);
        double rise = clampRise(visual.magmaSmokeRiseSpeed, 0.04);
        double stackStep = Math.max(0.35, visual.magmaSmokeStackStep);
        int stacks = Math.max(1, visual.magmaSmokeVerticalLayers);
        int darkPerLayer = Math.max(1, visual.magmaSmokeParticlesPerPlume);
        double x = base.getX() + (random.nextDouble() - 0.5) * half * 2.0;
        double z = base.getZ() + (random.nextDouble() - 0.5) * half * 2.0;
        double baseY = base.getY() + visual.magmaSmokeSpawnOffsetY;
        spawnSmokeCluster(world, dark, x, baseY, z, darkPerLayer + 1, horizontal * 0.6, drift * 0.5, rise * 0.7);
        spawnSmokeCluster(world, Particle.SMOKE, x, baseY + 0.05, z, 2, horizontal * 0.5, drift * 0.4, rise * 0.65);
        for (int stack = 0; stack < stacks; stack++) {
            double y = baseY + 0.12 + stack * stackStep;
            spawnSmokeCluster(world, dark, x, y, z, darkPerLayer, horizontal, drift, rise);
            if (stack >= stacks - 1) {
                spawnSmokeCluster(world, puff, x, y, z, 1, horizontal * 0.85, drift, rise * 0.95);
            }
        }
    }

    private static double clampSpread(double value, double fallback) {
        if (Double.isNaN(value) || value <= 0.0) {
            return fallback;
        }
        return Math.min(0.2, value);
    }

    private static double clampRise(double value, double fallback) {
        if (Double.isNaN(value) || value <= 0.0) {
            return fallback;
        }
        return Math.min(0.1, value);
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
        if (count <= 0) {
            return;
        }
        double speed = Math.max(minSmokeSpeed(particle), riseSpeed);
        world.spawnParticle(
                particle,
                x,
                y,
                z,
                count,
                spreadHorizontal,
                spreadVertical,
                spreadHorizontal,
                speed
        );
    }

    private static double minSmokeSpeed(Particle particle) {
        return switch (particle) {
            case SMOKE, LARGE_SMOKE -> 0.08;
            case CAMPFIRE_SIGNAL_SMOKE -> 0.04;
            default -> 0.05;
        };
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

    private void updateBedrockGlows(
            UUID sessionId,
            VolcanoSessionRegistry.SessionRecord record,
            VolcanoEruptionSettings eruption,
            VolcanoVisualSettings visual
    ) {
        World world = record.ventAnchor().getWorld();
        if (world == null) {
            return;
        }
        List<Location> points = bedrockGlowPoints.get(sessionId);
        if (points == null || points.isEmpty()) {
            if (record.schematicBounds().isEmpty()) {
                return;
            }
            points = scanBedrockGlowPoints(world, record.schematicBounds().get());
            if (!points.isEmpty()) {
                bedrockGlowPoints.put(sessionId, points);
                bedrockGlowScanAttempts.remove(sessionId);
            } else {
                int attempts = bedrockGlowScanAttempts.merge(sessionId, 1, Integer::sum);
                if (attempts >= 200) {
                    bedrockGlowPoints.put(sessionId, List.of());
                }
            }
            return;
        }
        for (Location point : points) {
            spawnGroundLootGlow(world, point.getX(), point.getY(), point.getZ(), eruption, visual.bedrockGlowOffsetY);
        }
    }

    private void tickAmbient(
            UUID sessionId,
            VolcanoSessionRegistry.SessionRecord record,
            VolcanoAmbientSettings ambient,
            int pulse
    ) {
        Location vent = record.ventAnchor();
        World world = vent.getWorld();
        if (world == null) {
            return;
        }
        Integer rumbleAt = nextRumblePulse.get(sessionId);
        if (rumbleAt != null && pulse >= rumbleAt) {
            playAmbientRumble(world, vent, ambient);
            nextRumblePulse.put(sessionId, pulse + randomInterval(ambient.rumbleIntervalMinTicks, ambient.rumbleIntervalMaxTicks));
        }
        if (!ambient.lightningEnabled) {
            return;
        }
        Integer lightningAt = nextLightningPulse.get(sessionId);
        if (lightningAt == null || pulse < lightningAt) {
            return;
        }
        nextLightningPulse.put(sessionId, pulse + randomInterval(ambient.lightningIntervalMinTicks, ambient.lightningIntervalMaxTicks));
        if (ThreadLocalRandom.current().nextDouble() > ambient.lightningChance) {
            return;
        }
        strikeCosmeticLightning(world, vent, ambient);
    }

    private void playAmbientRumble(World world, Location vent, VolcanoAmbientSettings ambient) {
        Location center = vent.clone().add(0.5, 0.5, 0.5);
        double radiusSquared = ambient.radiusBlocks * ambient.radiusBlocks;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float pitch = 0.55f + random.nextFloat() * 0.25f;
        float volume = ambient.rumbleVolume * (0.75f + random.nextFloat() * 0.25f);
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSquared) {
                player.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, volume, pitch);
            }
        }
    }

    private static void strikeCosmeticLightning(World world, Location vent, VolcanoAmbientSettings ambient) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radius = Math.max(2.0, ambient.lightningRadiusBlocks);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 2.0 + random.nextDouble() * radius;
        Location strike = vent.clone().add(
                Math.cos(angle) * distance,
                ambient.lightningHeightBlocks,
                Math.sin(angle) * distance
        );
        world.strikeLightningEffect(strike);
        world.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.65f, 0.75f + random.nextFloat() * 0.15f);
    }

    private static int randomInterval(int minTicks, int maxTicks) {
        int min = Math.max(1, minTicks);
        int max = Math.max(min, maxTicks);
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void tickVentHeat(
            UUID sessionId,
            VolcanoSessionRegistry.SessionRecord record,
            VolcanoVentHeatSettings heat,
            int pulse
    ) {
        if (!heat.enabled || !record.erupted() || Instant.now().isAfter(record.extinguishAt())) {
            return;
        }
        if (pulse % Math.max(1, heat.intervalTicks) != 0) {
            return;
        }
        World world = record.ventAnchor().getWorld();
        if (world == null || record.schematicBounds().isEmpty()) {
            return;
        }

        Set<Long> blocks = ventHeatBlocks.get(sessionId);
        if (blocks == null) {
            blocks = scanVentHeatBlocks(world, record.schematicBounds().get(), heat);
            if (blocks.isEmpty()) {
                int attempts = ventHeatScanAttempts.merge(sessionId, 1, Integer::sum);
                if (attempts >= 200) {
                    ventHeatBlocks.put(sessionId, Set.of());
                }
                return;
            }
            ventHeatBlocks.put(sessionId, blocks);
            ventHeatScanAttempts.remove(sessionId);
        }
        if (blocks.isEmpty()) {
            return;
        }

        Set<Material> hotMaterials = resolveHotMaterials(heat);
        Location vent = record.ventAnchor();
        SchematicWorldBounds bounds = record.schematicBounds().get();
        double radiusSquared = heat.ventRadiusBlocks * heat.ventRadiusBlocks;
        double ventX = vent.getX() + 0.5;
        double ventZ = vent.getZ() + 0.5;

        for (Player player : world.getPlayers()) {
            if (!player.isValid() || player.isDead()) {
                continue;
            }
            if (player.hasPermission(VolcanoPermissions.BYPASS)) {
                continue;
            }
            GameMode mode = player.getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                continue;
            }
            if (!isOnVentHeat(player, blocks, hotMaterials, bounds, ventX, ventZ, radiusSquared)) {
                continue;
            }

            player.damage(heat.damage, DamageSource.builder(DamageType.HOT_FLOOR).build());
            if (heat.burnTicks > 0) {
                player.setFireTicks(Math.max(player.getFireTicks(), heat.burnTicks));
            }
            if (heat.knockback > 0.0) {
                Vector away = player.getLocation().toVector().subtract(new Vector(ventX, player.getY(), ventZ));
                away.setY(0.0);
                if (away.lengthSquared() > 0.01) {
                    away.normalize().multiply(heat.knockback).setY(0.12);
                    player.setVelocity(player.getVelocity().add(away));
                }
            }
            world.spawnParticle(
                    Particle.FLAME,
                    player.getLocation().add(0.0, 0.15, 0.0),
                    3,
                    0.18,
                    0.08,
                    0.18,
                    0.01
            );
        }
    }

    private static boolean isOnVentHeat(
            Player player,
            Set<Long> heatBlocks,
            Set<Material> hotMaterials,
            SchematicWorldBounds bounds,
            double ventX,
            double ventZ,
            double radiusSquared
    ) {
        Block below = blockUnderFeet(player);
        if (heatBlocks.contains(blockKey(below.getX(), below.getY(), below.getZ()))) {
            return true;
        }
        if (!hotMaterials.contains(below.getType())) {
            return false;
        }
        if (!withinBounds(below.getX(), below.getY(), below.getZ(), bounds)) {
            return false;
        }
        double dx = player.getX() - ventX;
        double dz = player.getZ() - ventZ;
        return dx * dx + dz * dz <= radiusSquared;
    }

    private static Block blockUnderFeet(Player player) {
        return player.getLocation().clone().subtract(0.0, 0.05, 0.0).getBlock();
    }

    private static boolean withinBounds(int x, int y, int z, SchematicWorldBounds bounds) {
        return x >= bounds.minX() && x <= bounds.maxX()
                && y >= bounds.minY() && y <= bounds.maxY()
                && z >= bounds.minZ() && z <= bounds.maxZ();
    }

    private static Set<Long> scanVentHeatBlocks(
            World world,
            SchematicWorldBounds bounds,
            VolcanoVentHeatSettings heat
    ) {
        Set<Material> hotMaterials = resolveHotMaterials(heat);
        Set<Long> blocks = new HashSet<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    if (hotMaterials.contains(world.getBlockAt(x, y, z).getType())) {
                        blocks.add(blockKey(x, y, z));
                    }
                }
            }
        }
        return Set.copyOf(blocks);
    }

    private static Set<Material> resolveHotMaterials(VolcanoVentHeatSettings heat) {
        Set<Material> materials = new HashSet<>();
        for (String name : heat.blockMaterials) {
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                materials.add(Material.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (materials.isEmpty()) {
            materials.add(Material.BEDROCK);
            materials.add(Material.COAL_BLOCK);
        }
        return materials;
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
    }

    private List<Location> scanBedrockGlowPoints(World world, SchematicWorldBounds bounds) {
        List<Location> points = new ArrayList<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.BEDROCK) {
                        continue;
                    }
                    points.add(new Location(world, x + 0.5, y + 1.0, z + 0.5));
                }
            }
        }
        return List.copyOf(points);
    }

    private static void spawnGroundLootGlow(
            World world,
            double x,
            double y,
            double z,
            VolcanoEruptionSettings eruption,
            double yOffset
    ) {
        Particle primary = resolveParticle(eruption.trailParticle, Particle.FLAME);
        Particle secondary = resolveParticle(eruption.trailSecondaryParticle, Particle.LAVA);
        world.spawnParticle(
                primary,
                x,
                y + yOffset,
                z,
                eruption.trailParticleCount,
                0.08,
                0.05,
                0.08,
                0.01
        );
        if (eruption.trailSecondaryCount > 0) {
            world.spawnParticle(
                    secondary,
                    x,
                    y + yOffset - 0.05,
                    z,
                    eruption.trailSecondaryCount,
                    0.05,
                    0.03,
                    0.05,
                    0.0
            );
        }
    }

    private void updateLootVisuals(
            VolcanoSessionRegistry.SessionRecord record,
            VolcanoEruptionSettings eruption,
            VolcanoVisualSettings visual,
            int pulse
    ) {
        World world = record.ventAnchor().getWorld();
        if (world == null) {
            return;
        }
        double labelOffset = visual.itemLabelOffsetY;
        for (VolcanoSessionRegistry.LootItem lootItem : record.lootItems()) {
            if (lootItem.claimed()) {
                continue;
            }
            Item entity = findItem(world, lootItem.entityId());
            if (entity == null || entity.isDead()) {
                continue;
            }
            applyLootSlopeRoll(entity, eruption, pulse);
            Location location = entity.getLocation();
            spawnGroundLootGlow(
                    world,
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    eruption,
                    0.15
            );
            if (lootItem.labelId() != null) {
                TextDisplay label = findLabel(world, lootItem.labelId());
                if (label != null && !label.isDead()) {
                    label.teleport(location.clone().add(0, labelOffset, 0));
                }
            }
        }
    }

    private void applyLootSlopeRoll(Item entity, VolcanoEruptionSettings eruption, int pulse) {
        if (!eruption.lootSlopeRollEnabled) {
            return;
        }
        int interval = Math.max(1, eruption.lootSlopeRollIntervalTicks);
        if (pulse % interval != 0) {
            return;
        }

        UUID entityId = entity.getUniqueId();
        int rollTicks = lootSlopeRollTicks.merge(entityId, 1, Integer::sum);
        if (eruption.lootSlopeRollMaxTicks > 0 && rollTicks > eruption.lootSlopeRollMaxTicks) {
            return;
        }

        Location location = entity.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        int blockX = location.getBlockX();
        int blockZ = location.getBlockZ();
        int surfaceY = motionSurfaceY(world, blockX, blockZ);
        if (location.getY() - surfaceY > 1.35 && !entity.isOnGround()) {
            return;
        }

        Vector velocity = entity.getVelocity();
        if (velocity.getY() > 0.12 || velocity.lengthSquared() > 0.16) {
            return;
        }

        int minDrop = Math.max(1, eruption.lootSlopeRollMinDrop);
        int currentY = motionSurfaceY(world, blockX, blockZ);
        int bestDrop = 0;
        double bestDx = 0.0;
        double bestDz = 0.0;

        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (int[] direction : directions) {
            int neighborX = blockX + direction[0];
            int neighborZ = blockZ + direction[1];
            int drop = currentY - motionSurfaceY(world, neighborX, neighborZ);
            if (drop >= minDrop && drop > bestDrop) {
                bestDrop = drop;
                bestDx = direction[0];
                bestDz = direction[1];
            }
        }
        if (bestDrop < minDrop) {
            return;
        }

        double length = Math.sqrt((bestDx * bestDx) + (bestDz * bestDz));
        if (length <= 0.0) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double push = eruption.lootSlopeRollPush * (0.82 + random.nextDouble() * 0.28);
        entity.setVelocity(new Vector(
                (bestDx / length) * push,
                -0.015 - random.nextDouble() * 0.02,
                (bestDz / length) * push
        ));
    }

    private static int motionSurfaceY(World world, int x, int z) {
        return world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
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

    private static double pickLaunchAngle(Double lastLaunchAngleRad, VolcanoEruptionSettings eruption) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double minSeparation = Math.toRadians(Math.max(15, eruption.launchAngleMinSeparationDegrees));
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            if (lastLaunchAngleRad == null || Double.isNaN(lastLaunchAngleRad)) {
                return angle;
            }
            double delta = Math.abs(Math.atan2(
                    Math.sin(angle - lastLaunchAngleRad),
                    Math.cos(angle - lastLaunchAngleRad)
            ));
            if (delta >= minSeparation) {
                return angle;
            }
        }
        return random.nextDouble() * Math.PI * 2.0;
    }

    private static Vector velocityFromAngle(double angle, VolcanoEruptionSettings eruption) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
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

    private static Particle resolveSmokeParticle(String name, Particle fallback) {
        Particle particle = resolveParticle(name, fallback);
        if (particle == Particle.CAMPFIRE_COSY_SMOKE) {
            return Particle.CAMPFIRE_SIGNAL_SMOKE;
        }
        return particle;
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
