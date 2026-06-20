package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.api.mobwave.MobWaveAttachRequest;
import bm.b0b0b0.soulevents.api.mobwave.MobWaveChestPhase;
import bm.b0b0b0.soulevents.api.mobwave.MobWaveStatus;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobWavesModuleSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveDefinitionSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveMobEntrySettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveProfileSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWavesRuntimeLog;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntitySupport;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntityTags;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class MobWaveService {

    private final Plugin plugin;
    private MobWavesPluginConfig config;
    private final MobHealthBarService healthBarService;

    private final Map<UUID, ActiveSession> sessions = new ConcurrentHashMap<>();

    public MobWaveService(Plugin plugin, MobWavesPluginConfig config, MobHealthBarService healthBarService) {
        this.plugin = plugin;
        this.config = config;
        this.healthBarService = healthBarService;
    }

    public void reload(MobWavesPluginConfig config) {
        this.config = config;
    }

    public void attach(MobWaveAttachRequest request) {
        attachInternal(request, null, true);
    }

    public void attachStandalone(MobWaveAttachRequest request, MobWaveSessionHooks hooks) {
        attachStandalone(request, hooks, 0);
    }

    public void attachStandalone(MobWaveAttachRequest request, MobWaveSessionHooks hooks, int playerPresenceRadius) {
        attachInternal(request, hooks, false, playerPresenceRadius);
    }

    private void attachInternal(MobWaveAttachRequest request, MobWaveSessionHooks hooks, boolean bridgeMode) {
        attachInternal(request, hooks, bridgeMode, 0);
    }

    private void attachInternal(
            MobWaveAttachRequest request,
            MobWaveSessionHooks hooks,
            boolean bridgeMode,
            int playerPresenceRadius
    ) {
        detach(request.sessionId());
        Optional<WaveProfileDefinition> profileOptional = config.profile(request.profileId());
        if (profileOptional.isEmpty()) {
            plugin.getLogger().warning("MobWaves profile not found: " + request.profileId());
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        if (profile.settings().waves.isEmpty()) {
            plugin.getLogger().warning("MobWaves profile has no waves: " + profile.id());
            return;
        }
        Location anchor = blockAnchor(request.anchor());
        ActiveSession session = new ActiveSession(
                request.sessionId(),
                profile,
                anchor,
                resolveSpawnRadius(request.spawnRadius(), profile.settings()),
                hooks,
                bridgeMode,
                bridgeMode ? 0 : Math.max(0, playerPresenceRadius),
                resolveEnforcementRadius(request.spawnRadius(), bridgeMode, playerPresenceRadius)
        );
        resolveTiming(session, profile.settings());
        sessions.put(request.sessionId(), session);
        MobWavesRuntimeLog.wave(
                plugin,
                config,
                "attach session=" + request.sessionId()
                        + " profile=" + profile.id()
                        + " bridge=" + bridgeMode
                        + " anchor=" + formatBlock(anchor)
                        + " spawnRadius=" + session.spawnRadius
        );
        Runnable start = () -> startWave(session);
        if (Bukkit.isPrimaryThread()) {
            start.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, start);
        }
    }

    public void detach(UUID sessionId) {
        ActiveSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        session.cancelTasks();
        for (UUID mobId : session.aliveMobs) {
            healthBarService.remove(mobId);
            Entity entity = Bukkit.getEntity(mobId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        session.aliveMobs.clear();
    }

    public void shutdown() {
        for (UUID sessionId : sessions.keySet().stream().toList()) {
            detach(sessionId);
        }
    }

    public void forEachAliveMob(java.util.function.Consumer<LivingEntity> consumer) {
        for (ActiveSession session : sessions.values()) {
            for (UUID mobId : session.aliveMobs) {
                Entity entity = Bukkit.getEntity(mobId);
                if (entity instanceof LivingEntity living && living.isValid() && !living.isDead()) {
                    consumer.accept(living);
                }
            }
        }
    }

    public boolean blocksChest(UUID sessionId) {
        ActiveSession session = sessions.get(sessionId);
        if (session == null || !session.bridgeMode) {
            return false;
        }
        return session.phase == MobWaveChestPhase.LOCKED;
    }

    public Optional<MobWaveStatus> status(UUID sessionId) {
        ActiveSession session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        return Optional.of(new MobWaveStatus(
                session.waveIndex + 1,
                session.profile.settings().waves.size(),
                session.aliveMobs.size(),
                session.spawnQueue.size(),
                session.phase
        ));
    }

    public void onMobDeath(UUID sessionId, UUID mobId, Location deathLocation) {
        ActiveSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        session.aliveMobs.remove(mobId);
        healthBarService.remove(mobId);
        if (session.hooks != null && deathLocation != null) {
            session.hooks.onMobKilled(sessionId, deathLocation);
        }
        checkWaveProgress(session);
    }

    private void startWave(ActiveSession session) {
        session.cancelTasks();
        session.phase = MobWaveChestPhase.LOCKED;
        session.spawnQueue.clear();
        session.aliveMobs.clear();
        if (session.waveIndex >= session.profile.settings().waves.size()) {
            session.phase = MobWaveChestPhase.COMPLETED;
            return;
        }
        WaveDefinitionSettings wave = session.profile.settings().waves.get(session.waveIndex);
        for (WaveMobEntrySettings entry : wave.entries) {
            EntityType type = MobWaveEntitySupport.resolveEntityType(entry.entityType);
            if (type == null) {
                MobWavesRuntimeLog.warn(
                        plugin,
                        "wave entry skipped session=" + session.sessionId
                                + " wave=" + (session.waveIndex + 1)
                                + " unknown type=" + entry.entityType
                );
                continue;
            }
            int count = Math.max(0, entry.count);
            MobCombatContext context = new MobCombatContext(false, entry.maxHealth);
            for (int index = 0; index < count; index++) {
                session.spawnQueue.add(new SpawnPlan(type, context));
            }
        }
        if (wave.superBossEnabled && wave.superBoss != null) {
            EntityType bossType = MobWaveEntitySupport.resolveEntityType(wave.superBoss.entityType);
            if (bossType != null) {
                MobCombatContext bossContext = MobCombatContext.superBoss(wave.superBoss.maxHealth);
                int bossCount = Math.max(1, wave.superBoss.count);
                for (int index = 0; index < bossCount; index++) {
                    session.spawnQueue.add(new SpawnPlan(bossType, bossContext));
                }
            }
        }
        if (session.spawnQueue.isEmpty()) {
            session.phase = MobWaveChestPhase.COMPLETED;
            MobWavesRuntimeLog.warn(
                    plugin,
                    "wave empty session=" + session.sessionId + " wave=" + (session.waveIndex + 1)
            );
            return;
        }
        MobWavesRuntimeLog.wave(
                plugin,
                config,
                "start session=" + session.sessionId
                        + " wave=" + (session.waveIndex + 1) + "/" + session.profile.settings().waves.size()
                        + " queue=" + session.spawnQueue.size()
                        + " batchSize=" + session.batchSize
                        + " intervalTicks=" + session.batchIntervalTicks
                        + " anchor=" + formatBlock(session.anchor)
        );
        spawnNextBatch(session);
        session.batchTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> spawnNextBatch(session),
                session.batchIntervalTicks,
                session.batchIntervalTicks
        );
    }

    private void spawnNextBatch(ActiveSession session) {
        if (session.phase != MobWaveChestPhase.LOCKED) {
            return;
        }
        int spawned = 0;
        while (spawned < session.batchSize && !session.spawnQueue.isEmpty()) {
            SpawnPlan plan = session.spawnQueue.poll();
            if (plan == null) {
                break;
            }
            SpawnAttempt attempt = spawnMob(session, plan);
            if (attempt.entity() != null) {
                session.aliveMobs.add(attempt.entity().getUniqueId());
                spawned++;
                MobWavesRuntimeLog.wave(
                        plugin,
                        config,
                        "spawned session=" + session.sessionId
                                + " type=" + plan.type().name()
                                + " boss=" + plan.context().superBoss()
                                + " uuid=" + attempt.entity().getUniqueId()
                                + " at " + formatBlock(attempt.entity().getLocation())
                                + " alive=" + session.aliveMobs.size()
                                + " queueLeft=" + session.spawnQueue.size()
                );
            } else {
                session.spawnQueue.addFirst(plan);
                MobWavesRuntimeLog.warn(
                        plugin,
                        "spawn failed session=" + session.sessionId
                                + " type=" + plan.type().name()
                                + " reason=" + attempt.failureReason()
                                + " anchor=" + formatBlock(session.anchor)
                                + " queueLeft=" + (session.spawnQueue.size() + 1)
                );
                break;
            }
        }
        if (spawned == 0 && !session.spawnQueue.isEmpty()) {
            MobWavesRuntimeLog.spawnVerbose(
                    plugin,
                    config,
                    "batch idle session=" + session.sessionId + " queue=" + session.spawnQueue.size()
            );
        }
        if (session.spawnQueue.isEmpty() && session.batchTask != null) {
            session.batchTask.cancel();
            session.batchTask = null;
        }
        checkWaveProgress(session);
    }

    private void checkWaveProgress(ActiveSession session) {
        if (session.phase != MobWaveChestPhase.LOCKED) {
            return;
        }
        if (!session.spawnQueue.isEmpty() || !session.aliveMobs.isEmpty()) {
            return;
        }
        onWaveCleared(session);
    }

    private void onWaveCleared(ActiveSession session) {
        MobWavesRuntimeLog.wave(
                plugin,
                config,
                "cleared session=" + session.sessionId + " wave=" + (session.waveIndex + 1)
        );
        int nextWaveIndex = session.waveIndex + 1;
        if (nextWaveIndex >= session.profile.settings().waves.size()) {
            session.phase = MobWaveChestPhase.COMPLETED;
            if (session.hooks != null) {
                session.hooks.onAllWavesComplete(session.sessionId);
            }
            return;
        }
        session.phase = MobWaveChestPhase.GRACE;
        session.graceTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (!sessions.containsKey(session.sessionId)) {
                        return;
                    }
                    session.waveIndex = nextWaveIndex;
                    startWave(session);
                },
                session.graceSeconds * 20L
        );
    }

    private SpawnAttempt spawnMob(ActiveSession session, SpawnPlan plan) {
        EntityType type = plan.type();
        World world = session.anchor.getWorld();
        if (world == null) {
            return SpawnAttempt.failed("world-null");
        }
        LocationResult locationResult = locateSpawn(world, session.anchor, session.spawnRadius);
        if (locationResult.location() == null) {
            return SpawnAttempt.failed(locationResult.failureReason());
        }
        Location spawn = locationResult.location();
        Class<? extends Entity> entityClass = type.getEntityClass();
        if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass)) {
            return SpawnAttempt.failed("unsupported-entity-class");
        }
        @SuppressWarnings("unchecked")
        Class<? extends LivingEntity> livingClass = (Class<? extends LivingEntity>) entityClass;
        LivingEntity living = world.spawn(
                spawn,
                livingClass,
                CreatureSpawnEvent.SpawnReason.CUSTOM,
                entity -> {
                    if (entity instanceof Mob mob) {
                        mob.setRemoveWhenFarAway(false);
                        mob.setPersistent(true);
                    }
                    entity.setCustomNameVisible(false);
                    MobWaveEntityTags.tagMob(plugin, entity, session.sessionId, plan.context().superBoss());
                }
        );
        if (living == null || living.isDead()) {
            return SpawnAttempt.failed("spawn-cancelled-or-dead");
        }
        applyHordeCombat(living, session.profile.settings().mobOverrides, plan.context());
        if (living instanceof Mob mob && config.module().hordeCombat.forceTargetPlayers) {
            HordeMobTargetingService.assignNearestPlayer(
                    mob,
                    config.module().hordeCombat.targetRadiusBlocks
            );
        }
        healthBarService.attach(living);
        if (!living.isValid() || living.isDead()) {
            return SpawnAttempt.failed("invalid-after-healthbar");
        }
        UUID entityId = living.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Entity tracked = Bukkit.getEntity(entityId);
            if (tracked == null || tracked.isDead()) {
                MobWavesRuntimeLog.warn(
                        plugin,
                        "mob vanished session=" + session.sessionId
                                + " type=" + type.name()
                                + " uuid=" + entityId
                );
            }
        }, 20L);
        return SpawnAttempt.success(living);
    }

    private record SpawnAttempt(LivingEntity entity, String failureReason) {
        static SpawnAttempt success(LivingEntity entity) {
            return new SpawnAttempt(entity, null);
        }

        static SpawnAttempt failed(String reason) {
            return new SpawnAttempt(null, reason);
        }
    }

    private record LocationResult(Location location, String failureReason) {
        static LocationResult ok(Location location) {
            return new LocationResult(location, null);
        }

        static LocationResult fail(String reason) {
            return new LocationResult(null, reason);
        }
    }

    private static final int SPAWN_LOCATION_ATTEMPTS = 24;
    private static final int MIN_SPAWN_DISTANCE_BLOCKS = 2;

    private LocationResult locateSpawn(World world, Location anchor, int radius) {
        int chunkX = anchor.getBlockX() >> 4;
        int chunkZ = anchor.getBlockZ() >> 4;
        ensureChunksLoaded(world, chunkX, chunkZ);
        int searchRadius = Math.max(4, radius);
        int baseX = anchor.getBlockX();
        int baseZ = anchor.getBlockZ();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String lastReason = "no-valid-location";

        for (int attempt = 0; attempt < SPAWN_LOCATION_ATTEMPTS; attempt++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double distance = MIN_SPAWN_DISTANCE_BLOCKS
                    + random.nextDouble(Math.max(1.0, searchRadius - MIN_SPAWN_DISTANCE_BLOCKS));
            int x = baseX + (int) Math.round(Math.cos(angle) * distance);
            int z = baseZ + (int) Math.round(Math.sin(angle) * distance);
            LocationResult result = tryColumnAtSurface(world, x, z);
            if (result.location() != null) {
                return result;
            }
            lastReason = result.failureReason();
        }
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                if (dx * dx + dz * dz < MIN_SPAWN_DISTANCE_BLOCKS * MIN_SPAWN_DISTANCE_BLOCKS) {
                    continue;
                }
                if (dx * dx + dz * dz > searchRadius * searchRadius) {
                    continue;
                }
                LocationResult result = tryColumnAtSurface(world, baseX + dx, baseZ + dz);
                if (result.location() != null) {
                    return result;
                }
                lastReason = result.failureReason();
            }
        }
        return LocationResult.fail(lastReason);
    }

    private void ensureChunksLoaded(World world, int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getChunkAt(chunkX + dx, chunkZ + dz);
            }
        }
    }

    private LocationResult tryColumnAtSurface(World world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return LocationResult.fail("chunk-unloaded:" + chunkX + "," + chunkZ);
        }
        int surfaceY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        Location surfaceSpawn = new Location(world, x + 0.5, surfaceY + 1.0, z + 0.5);
        String reject = validateMobSpawn(surfaceSpawn);
        if (reject == null) {
            return LocationResult.ok(surfaceSpawn);
        }
        return LocationResult.fail("surface-rejected:" + reject + "@" + x + "," + surfaceY + "," + z);
    }

    private static String validateMobSpawn(Location spawn) {
        var feet = spawn.getBlock();
        var below = feet.getRelative(0, -1, 0);
        var head = feet.getRelative(0, 1, 0);
        if (!below.getType().isSolid() || below.isLiquid()) {
            return "no-solid-below:" + below.getType().name();
        }
        Material belowType = below.getType();
        if (belowType == Material.MAGMA_BLOCK
                || belowType == Material.CACTUS
                || belowType == Material.SWEET_BERRY_BUSH
                || belowType == Material.POWDER_SNOW) {
            return "bad-below:" + belowType.name();
        }
        if (!feet.isPassable()) {
            return "feet-blocked:" + feet.getType().name();
        }
        if (!head.isPassable()) {
            return "head-blocked:" + head.getType().name();
        }
        return null;
    }

    private void applyHordeCombat(
            LivingEntity living,
            Map<String, MobTypeOverrideSettings> overrides,
            MobCombatContext context
    ) {
        MobTypeOverrideSettings typeOverride = overrides.get(living.getType().name());
        HordeMobCombatApplier.apply(living, typeOverride, config.module().hordeCombat, context);
    }

    public record EnforcementZone(Location anchor, int radiusBlocks) {
    }

    public List<EnforcementZone> enforcementZones() {
        List<EnforcementZone> zones = new ArrayList<>();
        for (ActiveSession session : sessions.values()) {
            int radius = session.enforcementRadius > 0
                    ? session.enforcementRadius
                    : Math.max(48, session.spawnRadius);
            zones.add(new EnforcementZone(session.anchor, radius));
        }
        return zones;
    }

    private record SpawnPlan(EntityType type, MobCombatContext context) {
    }

    private void resolveTiming(ActiveSession session, WaveProfileSettings profile) {
        MobWavesModuleSettings module = config.module();
        session.batchSize = profile.batchSize > 0 ? profile.batchSize : module.defaultBatchSize;
        session.batchIntervalTicks = profile.batchIntervalTicks > 0
                ? profile.batchIntervalTicks
                : module.defaultBatchIntervalTicks;
        session.graceSeconds = profile.graceAfterClearSeconds > 0
                ? profile.graceAfterClearSeconds
                : module.defaultGraceAfterClearSeconds;
        session.batchSize = Math.max(1, session.batchSize);
        session.batchIntervalTicks = Math.max(5, session.batchIntervalTicks);
        session.graceSeconds = Math.max(1, session.graceSeconds);
    }

    private int resolveEnforcementRadius(int requestRadius, boolean bridgeMode, int playerPresenceRadius) {
        if (playerPresenceRadius > 0) {
            return playerPresenceRadius;
        }
        if (bridgeMode && requestRadius > 0) {
            return Math.max(48, requestRadius);
        }
        return 48;
    }

    private int resolveSpawnRadius(int requestRadius, WaveProfileSettings profile) {
        if (requestRadius > 0) {
            return requestRadius;
        }
        if (profile.spawnRadius > 0) {
            return profile.spawnRadius;
        }
        return Math.max(4, config.module().defaultSpawnRadius);
    }

    private static String formatBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()
                + "@" + location.getWorld().getName();
    }

    private static Location blockAnchor(Location anchor) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX(),
                anchor.getBlockY(),
                anchor.getBlockZ()
        );
    }

    private static final class ActiveSession {
        private final UUID sessionId;
        private final WaveProfileDefinition profile;
        private final Location anchor;
        private final int spawnRadius;
        private final ArrayDeque<SpawnPlan> spawnQueue = new ArrayDeque<>();
        private final Set<UUID> aliveMobs = new HashSet<>();

        private final MobWaveSessionHooks hooks;
        private final boolean bridgeMode;
        private final int playerPresenceRadius;
        private final int enforcementRadius;

        private int waveIndex;
        private MobWaveChestPhase phase = MobWaveChestPhase.LOCKED;
        private int batchSize;
        private int batchIntervalTicks;
        private int graceSeconds;
        private BukkitTask batchTask;
        private BukkitTask graceTask;

        private ActiveSession(
                UUID sessionId,
                WaveProfileDefinition profile,
                Location anchor,
                int spawnRadius,
                MobWaveSessionHooks hooks,
                boolean bridgeMode,
                int playerPresenceRadius,
                int enforcementRadius
        ) {
            this.sessionId = sessionId;
            this.profile = profile;
            this.anchor = anchor;
            this.spawnRadius = spawnRadius;
            this.hooks = hooks;
            this.bridgeMode = bridgeMode;
            this.playerPresenceRadius = playerPresenceRadius;
            this.enforcementRadius = enforcementRadius;
        }

        private void cancelTasks() {
            if (batchTask != null) {
                batchTask.cancel();
                batchTask = null;
            }
            if (graceTask != null) {
                graceTask.cancel();
                graceTask = null;
            }
        }
    }
}
