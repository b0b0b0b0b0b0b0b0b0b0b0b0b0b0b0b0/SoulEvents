package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.mobwave.MobWaveAttachRequest;
import bm.b0b0b0.soulevents.api.mobwave.MobWaveStatus;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import bm.b0b0b0.soulevents.api.module.EventPhase;
import bm.b0b0b0.soulevents.api.protection.LootGuardService;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOptions;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.api.world.WorldPlacementDenial;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
import bm.b0b0b0.soulevents.mobwaves.MobWavesPermissions;
import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeBroadcastSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeBuiltinNexusSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeLifecycleSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeLootVisualSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeMobLootSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WorldPlacementSettings;
import bm.b0b0b0.soulevents.mobwaves.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.mobwaves.integration.ArenaRegionService;
import bm.b0b0b0.soulevents.mobwaves.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.message.MobWavesRuntimeLog;
import bm.b0b0b0.soulevents.mobwaves.module.MobWavesModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class MobHordeService {

    private final SoulEventsApi api;
    private final Plugin plugin;
    private MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWaveService waveService;
    private final MobHordeSessionRegistry sessionRegistry;
    private final MobLootDropService lootDropService;
    private final LootRollService lootRollService;
    private final HordeNexusVisualService nexusVisualService;
    private final ArenaRegionService arenaRegionService;
    private final HordeDespawnBossBarService despawnBossBarService;
    private final SpawnWorldResolver spawnWorldResolver;
    private Map<String, WorldPlacementGate> gates = Map.of();

    public MobHordeService(
            SoulEventsApi api,
            Plugin plugin,
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobWaveService waveService,
            MobLootDropService lootDropService
    ) {
        this.api = api;
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.waveService = waveService;
        this.lootDropService = lootDropService;
        this.sessionRegistry = new MobHordeSessionRegistry();
        this.lootRollService = new LootRollService();
        this.nexusVisualService = new HordeNexusVisualService(plugin);
        this.arenaRegionService = WorldGuardIntegrations.createArenaRegionService(plugin);
        this.despawnBossBarService = new HordeDespawnBossBarService(plugin, messages);
        this.spawnWorldResolver = new SpawnWorldResolver(api.placement(), api.schematics(), api);
        wireBossBar();
        rebuildGates();
    }

    public void reload(MobWavesPluginConfig config) {
        this.config = config;
        rebuildGates();
        wireBossBar();
    }

    public HordeNexusVisualService nexusVisualService() {
        return nexusVisualService;
    }

    public MobWavesPluginConfig config() {
        return config;
    }

    public MobHordeSessionRegistry sessions() {
        return sessionRegistry;
    }

    public ArenaRegionService arenaRegions() {
        return arenaRegionService;
    }

    public Optional<ActiveEvent> activeEvent(UUID sessionId) {
        return api.modules().activeEvents(MobWavesModule.MODULE_ID).stream()
                .filter(event -> event.sessionId().equals(sessionId))
                .findFirst();
    }

    public LootGuardService lootGuard() {
        return api.protection().loot();
    }

    public void spawnScheduled(String typeId) {
        spawnInConfiguredWorldAsync(null, typeId, "scheduler", false);
    }

    public void spawnAdminAsync(CommandSender sender, String typeId) {
        spawnInConfiguredWorldAsync(sender, typeId, "admin", hasBypass(sender));
    }

    private static boolean hasBypass(CommandSender sender) {
        return sender instanceof Player player && player.hasPermission(MobWavesPermissions.BYPASS);
    }

    public void despawnAdmin(Player player, String typeId) {
        List<UUID> sessionIds = api.modules().activeEvents(MobWavesModule.MODULE_ID).stream()
                .filter(event -> event.typeId().equalsIgnoreCase(typeId))
                .map(ActiveEvent::sessionId)
                .toList();
        if (sessionIds.isEmpty()) {
            messages.send(player, "mobwaves.despawn-none", Map.of("type", typeId));
            return;
        }
        for (UUID sessionId : sessionIds) {
            endSession(sessionId, "ADMIN");
        }
        messages.send(player, "mobwaves.despawn-done", Map.of(
                "type", typeId,
                "count", Integer.toString(sessionIds.size())
        ));
    }

    public void teleportToActive(Player player, String typeId) {
        List<ActiveEvent> events = api.modules().activeEvents(MobWavesModule.MODULE_ID).stream()
                .filter(event -> event.typeId().equalsIgnoreCase(typeId))
                .toList();
        if (events.isEmpty()) {
            messages.send(player, "mobwaves.teleport-none", Map.of("type", typeId));
            return;
        }
        Location playerLocation = player.getLocation();
        ActiveEvent target = events.stream()
                .min(Comparator.comparingDouble(event -> event.anchor().distanceSquared(playerLocation)))
                .orElse(events.getFirst());
        Location teleportAnchor = sessionRegistry.find(target.sessionId())
                .map(MobHordeSessionRegistry.SessionRecord::waveAnchor)
                .orElse(target.anchor());
        Location destination = teleportAnchor.clone().add(0.5, 1.0, 0.5);
        destination.setPitch(playerLocation.getPitch());
        destination.setYaw(playerLocation.getYaw());
        player.teleport(destination);
        config.type(typeId).ifPresent(definition ->
                messages.send(player, "mobwaves.teleported", placeholders(typeId, definition, teleportAnchor))
        );
    }

    public int countActive(String typeId) {
        return (int) api.modules().activeEvents(MobWavesModule.MODULE_ID).stream()
                .filter(event -> event.typeId().equalsIgnoreCase(typeId))
                .count();
    }

    private void spawnInConfiguredWorldAsync(
            CommandSender feedback,
            String typeId,
            String source,
            boolean bypassLimits
    ) {
        Optional<HordeTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty()) {
            if (feedback != null) {
                messages.send(feedback, "mobwaves.unknown-type", Map.of("type", typeId));
            }
            return;
        }
        HordeTypeDefinition definition = typeOptional.get();
        WorldPlacementGate gate = gate(typeId);
        String spawnWorldName = spawnWorldResolver.configuredWorldName(definition.settings());
        World spawnWorld = Bukkit.getWorld(spawnWorldName);
        SpawnSearchDebug debug = new SpawnSearchDebug(
                plugin,
                config.module().spawnDebugEnabled,
                typeId
        );
        if (spawnWorld == null) {
            debug.finishFailedWorld(spawnWorldName, "world not loaded");
            plugin.getLogger().warning(
                    "Horde spawn failed type=" + typeId + " source=" + source + ": " + debug.failureMessage()
            );
            if (feedback != null) {
                messages.send(feedback, "mobwaves.world-not-found", Map.of("world", spawnWorldName));
            }
            return;
        }
        WorldPlacementResult worldCheck = gate.checkWorld(spawnWorld);
        if (!worldCheck.allowed()) {
            debug.finishFailedWorld(
                    spawnWorldName,
                    SpawnSearchDebug.gateReason(worldCheck.denial().name(), worldCheck.regionName())
            );
            plugin.getLogger().warning(
                    "Horde spawn failed type=" + typeId + " source=" + source + ": " + debug.failureMessage()
            );
            if (feedback != null) {
                sendPlacementError(feedback, worldCheck);
            }
            return;
        }
        plugin.getLogger().info(
                "Horde spawn search type=" + typeId + " world=" + spawnWorldName + " source=" + source
        );
        spawnWorldResolver.resolveAsync(plugin, definition.settings(), gate, debug, location -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (location.isEmpty()) {
                plugin.getLogger().warning(
                        "Horde spawn failed type=" + typeId + " source=" + source + ": " + debug.failureMessage()
                );
                if (feedback != null) {
                    messages.send(feedback, "mobwaves.spawn-failed", Map.of("type", typeId));
                }
                return;
            }
            Location surface = groundPasteOrigin(location.get());
            if (!canSpawn(typeId, definition.settings().maxConcurrent, bypassLimits)) {
                if (feedback != null) {
                    messages.send(feedback, "mobwaves.limit-reached", Map.of("type", typeId));
                }
                return;
            }
            startHorde(typeId, definition, surface, source.toUpperCase(Locale.ROOT));
            if (feedback != null) {
                messages.send(feedback, "mobwaves.admin-summoned", placeholders(typeId, definition, surface));
            }
        }));
    }

    private void startHorde(String typeId, HordeTypeDefinition definition, Location surfaceOrigin, String source) {
        Location pasteOrigin = groundPasteOrigin(blockAnchor(surfaceOrigin));
        if (definition.settings().usesSchematic()) {
            startHordeWithSchematic(typeId, definition, pasteOrigin, source);
            return;
        }
        startHordeWithBuiltinNexus(typeId, definition, pasteOrigin, source);
    }

    private void startHordeWithBuiltinNexus(
            String typeId,
            HordeTypeDefinition definition,
            Location pasteOrigin,
            String source
    ) {
        HordeBuiltinNexusSettings nexusSettings = definition.settings().builtinNexus;
        HordeBuiltinNexusBuilder.BuiltNexus builtNexus = null;
        Location groundOrigin = groundPasteOrigin(pasteOrigin);
        Location waveAnchor = groundOrigin.clone().add(0.0, 1.0, 0.0);
        Location visualAnchor = new Location(
                groundOrigin.getWorld(),
                groundOrigin.getBlockX() + 0.5,
                groundOrigin.getBlockY() + 1.0,
                groundOrigin.getBlockZ() + 0.5
        );
        if (nexusSettings.enabled) {
            clearNexusFooting(definition, groundOrigin, nexusSettings);
            Optional<HordeBuiltinNexusBuilder.BuiltNexus> builtOptional =
                    HordeBuiltinNexusBuilder.build(groundOrigin, nexusSettings);
            if (builtOptional.isPresent()) {
                builtNexus = builtOptional.get();
                waveAnchor = builtNexus.waveAnchor();
                visualAnchor = builtNexus.visualAnchor();
            }
        }
        Location sessionAnchor = blockAnchor(waveAnchor);
        ActiveEvent session = api.sessions().start(MobWavesModule.MODULE_ID, typeId, sessionAnchor);
        UUID sessionId = session.sessionId();
        sessionRegistry.register(new MobHordeSessionRegistry.SessionRecord(
                sessionId,
                typeId,
                groundOrigin,
                sessionAnchor,
                builtNexus,
                false
        ));
        arenaRegionService.create(sessionId, sessionAnchor, definition.settings().arenaWorldGuard, Optional.empty());
        finalizeHordeStart(typeId, definition, sessionId, sessionAnchor, visualAnchor, source);
    }

    private void startHordeWithSchematic(
            String typeId,
            HordeTypeDefinition definition,
            Location pasteOrigin,
            String source
    ) {
        String schematicId = definition.settings().schematicId();
        Location predictedAnchor = api.schematics().resolveChestAnchor(pasteOrigin, schematicId)
                .orElse(pasteOrigin.clone().add(0.0, 1.0, 0.0));
        ActiveEvent session = api.sessions().start(MobWavesModule.MODULE_ID, typeId, blockAnchor(predictedAnchor));
        UUID sessionId = session.sessionId();
        Optional<SchematicWorldBounds> schematicBounds = api.schematics().worldBounds(schematicId, pasteOrigin);
        sessionRegistry.register(new MobHordeSessionRegistry.SessionRecord(
                sessionId,
                typeId,
                pasteOrigin,
                blockAnchor(predictedAnchor),
                null,
                true
        ));
        arenaRegionService.create(sessionId, blockAnchor(predictedAnchor), definition.settings().arenaWorldGuard, schematicBounds);
        api.sessions().setPhase(sessionId, EventPhase.PRE_OPEN);
        SchematicPasteOptions options = SchematicPasteOptions.of(sessionId);
        api.schematics().paste(
                schematicId,
                pasteOrigin,
                options,
                HordeSchematicSpawnOverridesFactory.from(definition.settings().schematic)
        ).thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!result.success()) {
                plugin.getLogger().warning(
                        "Horde schematic paste failed type=" + typeId
                                + " schematic=" + schematicId
                                + " at " + pasteOrigin.getBlockX() + ", "
                                + pasteOrigin.getBlockY() + ", "
                                + pasteOrigin.getBlockZ()
                );
                endSession(sessionId, "SCHEMATIC_FAILED");
                return;
            }
            Location waveAnchor = result.chestAnchor() != null
                    ? blockAnchor(result.chestAnchor())
                    : blockAnchor(predictedAnchor);
            sessionRegistry.remove(sessionId);
            sessionRegistry.register(new MobHordeSessionRegistry.SessionRecord(
                    sessionId,
                    typeId,
                    pasteOrigin,
                    waveAnchor,
                    null,
                    true
            ));
            arenaRegionService.remove(sessionId);
            arenaRegionService.create(sessionId, waveAnchor, definition.settings().arenaWorldGuard, schematicBounds);
            finalizeHordeStart(typeId, definition, sessionId, waveAnchor, waveAnchor, source);
        }));
    }

    private void finalizeHordeStart(
            String typeId,
            HordeTypeDefinition definition,
            UUID sessionId,
            Location waveAnchor,
            Location visualAnchor,
            String source
    ) {
        api.sessions().setPhase(sessionId, EventPhase.PRE_OPEN);
        if (definition.settings().broadcast.enabled && definition.settings().broadcast.spawnEnabled) {
            messages.broadcast(
                    definition.settings().broadcast.messageKey,
                    placeholders(typeId, definition, waveAnchor)
            );
        }
        nexusVisualService.start(sessionId, visualAnchor);
        scheduleAbandonedHordeCleanup(sessionId, definition);
        scheduleWaveActivation(typeId, definition, sessionId, waveAnchor);
        plugin.getLogger().info("MobHorde started type=" + typeId + " session=" + sessionId + " source=" + source);
    }

    private void scheduleAbandonedHordeCleanup(UUID sessionId, HordeTypeDefinition definition) {
        int seconds = definition.settings().lifecycle.maxActiveSeconds;
        if (seconds <= 0) {
            return;
        }
        scheduleSessionEnd(sessionId, Instant.now().plusSeconds(seconds), "EXPIRED");
    }

    private void scheduleHordeExpiry(UUID sessionId, HordeTypeDefinition definition) {
        int seconds = definition.settings().lifecycle.maxActiveSeconds;
        if (seconds <= 0) {
            return;
        }
        Instant expireAt = Instant.now().plusSeconds(seconds);
        sessionRegistry.find(sessionId).ifPresent(record -> record.expireAt(expireAt));
        scheduleSessionEnd(sessionId, expireAt, "EXPIRED");
    }

    private void scheduleWaveActivation(
            String typeId,
            HordeTypeDefinition definition,
            UUID sessionId,
            Location waveAnchor
    ) {
        HordeLifecycleSettings lifecycle = definition.settings().lifecycle;
        if (!lifecycle.requirePlayerForWaves) {
            attachWaves(typeId, definition, sessionId, waveAnchor);
            return;
        }
        int radius = waveActivationRadius(lifecycle);
        if (hasPlayerNear(waveAnchor, radius)) {
            attachWaves(typeId, definition, sessionId, waveAnchor);
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Optional<MobHordeSessionRegistry.SessionRecord> record = sessionRegistry.find(sessionId);
            if (record.isEmpty()) {
                return;
            }
            if (record.get().wavesAttached()) {
                record.get().cancelActivationTask();
                return;
            }
            if (!hasPlayerNear(waveAnchor, radius)) {
                return;
            }
            attachWaves(typeId, definition, sessionId, waveAnchor);
        }, 20L, 20L);
        sessionRegistry.assignActivationTask(sessionId, task);
        plugin.getLogger().info(
                "MobHorde waiting for player type=" + typeId
                        + " session=" + sessionId
                        + " radius=" + radius
        );
    }

    private void attachWaves(
            String typeId,
            HordeTypeDefinition definition,
            UUID sessionId,
            Location waveAnchor
    ) {
        Optional<MobHordeSessionRegistry.SessionRecord> record = sessionRegistry.find(sessionId);
        if (record.isEmpty() || record.get().wavesAttached()) {
            return;
        }
        record.get().markWavesAttached();
        record.get().cancelActivationTask();
        api.sessions().setPhase(sessionId, EventPhase.ACTIVE);
        scheduleHordeExpiry(sessionId, definition);
        int spawnRadius = definition.settings().waveSpawnRadius;
        Location waveBlockAnchor = blockAnchor(waveAnchor);
        MobWavesRuntimeLog.horde(
                plugin,
                config,
                "waves attach session=" + sessionId
                        + " type=" + typeId
                        + " profile=" + definition.settings().waveProfileId
                        + " anchor=" + waveBlockAnchor.getBlockX() + "," + waveBlockAnchor.getBlockY() + ","
                        + waveBlockAnchor.getBlockZ() + "@" + waveBlockAnchor.getWorld().getName()
                        + " spawnRadius=" + (spawnRadius > 0 ? spawnRadius : config.module().defaultSpawnRadius)
        );
        waveService.attachStandalone(
                new MobWaveAttachRequest(
                        sessionId,
                        definition.settings().waveProfileId,
                        waveBlockAnchor,
                        spawnRadius
                ),
                hordeHooks(definition),
                waveActivationRadius(definition.settings().lifecycle)
        );
        plugin.getLogger().info("MobHorde waves started type=" + typeId + " session=" + sessionId);
    }

    private static int waveActivationRadius(HordeLifecycleSettings lifecycle) {
        if (lifecycle.requirePlayerRadiusBlocks > 0) {
            return lifecycle.requirePlayerRadiusBlocks;
        }
        if (lifecycle.bossBarRadius > 0) {
            return lifecycle.bossBarRadius;
        }
        return 48;
    }

    private static boolean hasPlayerNear(Location anchor, int radius) {
        World world = anchor.getWorld();
        if (world == null || radius <= 0) {
            return false;
        }
        double radiusSquared = (double) radius * radius;
        Location center = anchor.clone().add(0.5, 0.5, 0.5);
        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (player.getWorld().equals(world)
                    && player.getLocation().distanceSquared(center) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private MobWaveSessionHooks hordeHooks(HordeTypeDefinition definition) {
        return new MobWaveSessionHooks() {
            @Override
            public void onMobKilled(UUID waveSessionId, Location deathLocation, Player killer, int waveIndex, boolean superBoss) {
                sessionRegistry.find(waveSessionId).ifPresent(MobHordeSessionRegistry.SessionRecord::incrementSessionKills);
                dropLootForKill(waveSessionId, definition, deathLocation);
                if (killer != null && killer.isOnline() && !superBoss) {
                    sendMobKillActionBar(killer, waveSessionId, definition, waveIndex);
                }
            }

            @Override
            public void onBossKilled(UUID waveSessionId, int waveIndex, Player killer, int sessionKills) {
                sessionRegistry.find(waveSessionId).ifPresent(record -> {
                    if (killer != null) {
                        record.lastBossKiller(killer.getName());
                    }
                    Location strike = killer != null ? killer.getLocation() : record.waveAnchor();
                    nexusVisualService.playBossSlainThunder(record.waveAnchor(), strike);
                    if (killer != null && killer.isOnline()) {
                        sendBossKillActionBar(killer, waveSessionId, definition, waveIndex, sessionKills);
                    }
                });
            }

            @Override
            public void onAllWavesComplete(UUID waveSessionId) {
                onHordeWavesComplete(waveSessionId, definition);
            }

            @Override
            public void onWaveTimerExpired(UUID waveSessionId) {
                messages.broadcast("mobwaves.wave-timer-expired", placeholders(definition.id(), definition, null));
                endSession(waveSessionId, "WAVE_TIMER");
            }
        };
    }

    private void dropLootForKill(UUID sessionId, HordeTypeDefinition definition, Location deathLocation) {
        HordeMobLootSettings mobLoot = definition.settings().mobLoot;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int minRolls = Math.max(0, mobLoot.rollsPerKillMin);
        int maxRolls = Math.max(minRolls, mobLoot.rollsPerKillMax);
        int rolls = minRolls == maxRolls ? minRolls : random.nextInt(minRolls, maxRolls + 1);
        List<ItemStack> rolled = lootRollService.rollForEruption(definition.loot(), rolls);
        for (ItemStack real : rolled) {
            if (real == null || real.getType().isAir()) {
                continue;
            }
            int slot = sessionRegistry.nextSlot(sessionId);
            ItemStack mask = lootDropService.pickObfuscationMask(definition.loot());
            ItemStack obfuscated = lootGuard().obfuscate(real, sessionId, slot, mask);
            lootDropService.drop(sessionId, definition, deathLocation, obfuscated, slot, sessionRegistry);
        }
    }

    private void onHordeWavesComplete(UUID sessionId, HordeTypeDefinition definition) {
        api.sessions().setPhase(sessionId, EventPhase.LOOTABLE);
        HordeBroadcastSettings broadcast = definition.settings().broadcast;
        if (broadcast.enabled && broadcast.clearedEnabled) {
            sessionRegistry.find(sessionId).ifPresent(record ->
                    messages.broadcast(
                            broadcast.clearedMessageKey,
                            placeholders(definition.id(), definition, record.waveAnchor(), record)
                    )
            );
        }
        int seconds = Math.max(1, definition.settings().lifecycle.maxActiveSecondsAfterCleared);
        Instant endAt = Instant.now().plusSeconds(seconds);
        sessionRegistry.find(sessionId).ifPresent(record -> {
            record.markWavesVictory();
            record.endAt(endAt);
            record.cancelCleanupTask();
            nexusVisualService.beginVictoryFinale(sessionId, record.waveAnchor(), seconds);
        });
        scheduleSessionEnd(sessionId, endAt, "CLEARED");
    }

    public void handleItemPickup(Player player, Item itemEntity) {
        Optional<UUID> sessionIdOptional = sessionRegistry.sessionIdForEntity(itemEntity.getUniqueId());
        if (sessionIdOptional.isEmpty()) {
            return;
        }
        UUID sessionId = sessionIdOptional.get();
        MobHordeSessionRegistry.LootItem lootItem = sessionRegistry.find(sessionId)
                .flatMap(record -> record.lootItems().stream()
                        .filter(item -> item.entityId.equals(itemEntity.getUniqueId()))
                        .findFirst())
                .orElse(null);
        if (lootItem == null || lootItem.claimed) {
            return;
        }
        if (Bukkit.getCurrentTick() < lootItem.pickableAfterTick) {
            messages.send(player, "mobwaves.loot-pickup-delay", Map.of());
            return;
        }
        ItemStack stack = itemEntity.getItemStack();
        var refOptional = lootGuard().obfuscatedRef(stack);
        if (refOptional.isEmpty()) {
            return;
        }
        var ref = refOptional.get();
        if (!lootGuard().canTake(player, ref.sessionId(), ref.slotIndex())) {
            api.messages().send(player, "protection.loot.cooldown", Map.of());
            return;
        }
        if (!lootGuard().tryTakeObfuscated(player, stack, ref.sessionId(), ref.slotIndex())) {
            messages.send(player, "mobwaves.loot-pickup-failed", Map.of());
            return;
        }
        lootGuard().registerTake(player, ref.sessionId(), ref.slotIndex());
        lootDropService.removeLabel(itemEntity.getWorld(), lootItem.labelId);
        itemEntity.remove();
        sessionRegistry.markLootClaimed(sessionId, itemEntity.getUniqueId());
        activeEvent(sessionId).flatMap(event -> config.type(event.typeId()))
                .ifPresent(definition -> sendPickupActionBar(player, sessionId, definition));
    }

    private void sendPickupActionBar(Player player, UUID sessionId, HordeTypeDefinition definition) {
        HordeLootVisualSettings visual = definition.settings().lootVisual;
        if (!visual.pickupActionBarEnabled || visual.pickupActionBarKeys.isEmpty()) {
            return;
        }
        String key = visual.pickupActionBarKeys.get(
                ThreadLocalRandom.current().nextInt(visual.pickupActionBarKeys.size())
        );
        sessionRegistry.find(sessionId).ifPresent(record ->
                messages.sendActionBar(
                        player,
                        key,
                        placeholders(definition.id(), definition, record.waveAnchor(), record)
                )
        );
    }

    private void sendMobKillActionBar(Player killer, UUID sessionId, HordeTypeDefinition definition, int waveIndex) {
        HordeBroadcastSettings broadcast = definition.settings().broadcast;
        if (!broadcast.mobKillActionBarEnabled || broadcast.mobKillActionBarKeys.isEmpty()) {
            return;
        }
        String key = broadcast.mobKillActionBarKeys.get(
                ThreadLocalRandom.current().nextInt(broadcast.mobKillActionBarKeys.size())
        );
        sessionRegistry.find(sessionId).ifPresent(record ->
                messages.sendActionBar(
                        killer,
                        key,
                        killActionBarPlaceholders(definition, record, waveIndex)
                )
        );
    }

    private void sendBossKillActionBar(
            Player killer,
            UUID sessionId,
            HordeTypeDefinition definition,
            int waveIndex,
            int sessionKills
    ) {
        HordeBroadcastSettings broadcast = definition.settings().broadcast;
        if (!broadcast.bossKillActionBarEnabled || broadcast.bossKillActionBarKeys.isEmpty()) {
            return;
        }
        String key = broadcast.bossKillActionBarKeys.get(
                ThreadLocalRandom.current().nextInt(broadcast.bossKillActionBarKeys.size())
        );
        sessionRegistry.find(sessionId).ifPresent(record -> {
            Map<String, String> placeholders = killActionBarPlaceholders(definition, record, waveIndex);
            placeholders.put("kills", Integer.toString(sessionKills));
            placeholders.put("alive", Integer.toString(aliveCountNear(record)));
            placeholders.put("next_wave", Integer.toString(waveIndex + 1));
            messages.sendActionBar(killer, key, placeholders);
        });
    }

    private Map<String, String> killActionBarPlaceholders(
            HordeTypeDefinition definition,
            MobHordeSessionRegistry.SessionRecord record,
            int waveIndex
    ) {
        Map<String, String> placeholders = placeholders(definition.id(), definition, record.waveAnchor(), record);
        placeholders.put("wave", Integer.toString(waveIndex));
        int bonus = Math.max(0, config.module().hordeCombat.secondsAddedPerKill);
        placeholders.put("bonus", Integer.toString(bonus));
        return placeholders;
    }

    private int aliveCountNear(MobHordeSessionRegistry.SessionRecord record) {
        return waveService.status(record.sessionId())
                .map(MobWaveStatus::aliveMobs)
                .orElse(0);
    }

    public void shutdown() {
        despawnBossBarService.stop();
        arenaRegionService.shutdown();
        nexusVisualService.shutdown();
        for (ActiveEvent event : api.modules().activeEvents(MobWavesModule.MODULE_ID)) {
            endSession(event.sessionId(), "SHUTDOWN");
        }
        sessionRegistry.snapshot().forEach(record -> sessionRegistry.remove(record.sessionId()));
    }

    public void startBossBar() {
        despawnBossBarService.start();
    }

    private void endSession(UUID sessionId, String phase) {
        if (sessionRegistry.find(sessionId).isEmpty() && activeEvent(sessionId).isEmpty()) {
            return;
        }
        if (!"SHUTDOWN".equals(phase) && !"ADMIN".equals(phase) && !"CLEARED".equals(phase)) {
            Optional<ActiveEvent> active = activeEvent(sessionId);
            Optional<MobHordeSessionRegistry.SessionRecord> record = sessionRegistry.find(sessionId);
            if (active.isPresent() && record.isPresent()) {
                config.type(active.get().typeId()).ifPresent(definition -> {
                    HordeBroadcastSettings broadcast = definition.settings().broadcast;
                    if (broadcast.enabled && broadcast.removedEnabled) {
                        messages.broadcast(
                                broadcast.removedMessageKey,
                                placeholders(active.get().typeId(), definition, record.get().waveAnchor())
                        );
                    }
                });
            }
        }
        sessionRegistry.find(sessionId).ifPresent(MobHordeSessionRegistry.SessionRecord::cancelActivationTask);
        waveService.detach(sessionId);
        nexusVisualService.stop(sessionId);
        arenaRegionService.remove(sessionId);
        sessionRegistry.find(sessionId).ifPresent(record -> {
            for (MobHordeSessionRegistry.LootItem item : record.lootItems()) {
                var entity = Bukkit.getEntity(item.entityId);
                if (entity != null) {
                    entity.remove();
                }
                lootDropService.removeLabel(record.waveAnchor().getWorld(), item.labelId);
            }
            if (record.builtNexus() != null) {
                HordeBuiltinNexusBuilder.undo(record.builtNexus());
            }
            if (record.schematicPasted()) {
                api.schematics().undo(sessionId);
            }
        });
        sessionRegistry.remove(sessionId);
        api.protection().loot().clearSession(sessionId);
        api.sessions().end(sessionId);
    }

    private void scheduleSessionEnd(UUID sessionId, Instant endAt, String phase) {
        long delaySeconds = Math.max(0L, endAt.getEpochSecond() - Instant.now().getEpochSecond());
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> endSession(sessionId, phase),
                delaySeconds * 20L
        );
        sessionRegistry.assignCleanupTask(sessionId, task);
    }

    private boolean canSpawn(String typeId, int maxConcurrent, boolean bypass) {
        var active = api.modules().activeEvents(MobWavesModule.MODULE_ID);
        if (!bypass && active.size() >= config.module().maxConcurrentTotal) {
            return false;
        }
        if (bypass) {
            return true;
        }
        long typeActive = active.stream().filter(event -> event.typeId().equalsIgnoreCase(typeId)).count();
        return typeActive < maxConcurrent;
    }

    private WorldPlacementGate gate(String typeId) {
        WorldPlacementGate gate = gates.get(typeId);
        if (gate != null) {
            return gate;
        }
        Optional<HordeTypeDefinition> definition = config.type(typeId);
        WorldPlacementSettings placement = definition
                .map(value -> value.settings().worldPlacement)
                .orElseGet(WorldPlacementSettings::new);
        return new WorldPlacementGate(placement, WorldGuardIntegrations.createProbe());
    }

    private void rebuildGates() {
        Map<String, WorldPlacementGate> rebuilt = new HashMap<>();
        for (HordeTypeDefinition definition : config.types()) {
            rebuilt.put(
                    definition.id(),
                    new WorldPlacementGate(
                            definition.settings().worldPlacement,
                            WorldGuardIntegrations.createProbe()
                    )
            );
        }
        gates = Map.copyOf(rebuilt);
    }

    private void wireBossBar() {
        despawnBossBarService.wire(
                () -> sessionRegistry.snapshot().stream()
                        .collect(Collectors.toMap(MobHordeSessionRegistry.SessionRecord::sessionId, record -> record)),
                this::activeEvent,
                config::type,
                waveService::waveTimerSnapshot
        );
    }

    private void sendPlacementError(CommandSender sender, WorldPlacementResult result) {
        messages.send(sender, placementMessageKey(result), Map.of(
                "world", result.worldName(),
                "region", result.regionName(),
                "distance", result.regionName()
        ));
    }

    private static String placementMessageKey(WorldPlacementResult result) {
        if (result.allowed() || result.denial() == WorldPlacementDenial.NONE) {
            return "mobwaves.placement.invalid-location";
        }
        return "mobwaves.placement." + result.denial().name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static Location blockAnchor(Location anchor) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX(),
                anchor.getBlockY(),
                anchor.getBlockZ()
        );
    }

    private Location groundPasteOrigin(Location hint) {
        World world = hint.getWorld();
        if (world == null) {
            return hint;
        }
        int x = hint.getBlockX();
        int z = hint.getBlockZ();
        int groundY = api.placement().naturalGroundY(world, x, z);
        return new Location(world, x, groundY, z);
    }

    private void clearNexusFooting(
            HordeTypeDefinition definition,
            Location groundOrigin,
            HordeBuiltinNexusSettings nexusSettings
    ) {
        World world = groundOrigin.getWorld();
        if (world == null) {
            return;
        }
        int groundY = groundOrigin.getBlockY();
        int clearTop = groundY + nexusSettings.visibleHeight
                + Math.max(2, definition.settings().randomSpawn.flatMinAirAbove)
                + 12;
        for (FlatSurfaceOffset offset : HordeBuiltinNexusBuilder.footprintOffsets(nexusSettings)) {
            api.placement().clearObstructions(
                    world,
                    groundOrigin.getBlockX() + offset.dx(),
                    groundOrigin.getBlockZ() + offset.dz(),
                    groundY + 1,
                    clearTop
            );
        }
    }

    private Map<String, String> placeholders(String typeId, HordeTypeDefinition definition, Location anchor) {
        return placeholders(typeId, definition, anchor, null);
    }

    private Map<String, String> placeholders(
            String typeId,
            HordeTypeDefinition definition,
            Location anchor,
            MobHordeSessionRegistry.SessionRecord record
    ) {
        Map<String, String> values = new HashMap<>();
        values.put("type", typeId);
        values.put("type_name", messages.resolvePlain(definition.settings().displayNameKey, Map.of()));
        values.put(
                "structure_name",
                messages.resolvePlain(definition.settings().structureNameKey(), Map.of())
        );
        if (record != null) {
            values.put("kills", Integer.toString(record.sessionKills()));
            values.put("boss_killer", record.lastBossKiller());
        } else {
            values.put("kills", "0");
            values.put("boss_killer", "—");
        }
        if (anchor != null) {
            values.put("x", Integer.toString(anchor.getBlockX()));
            values.put("y", Integer.toString(anchor.getBlockY()));
            values.put("z", Integer.toString(anchor.getBlockZ()));
        }
        return values;
    }
}
