package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import bm.b0b0b0.soulevents.api.module.EventPhase;
import bm.b0b0b0.soulevents.api.protection.LootGuardService;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOptions;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPermissions;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPluginConfig;
import bm.b0b0b0.soulevents.volcano.config.VolcanoTypeDefinition;
import bm.b0b0b0.soulevents.volcano.config.settings.BroadcastSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoEruptionSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoTypeSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoVisualSettings;
import bm.b0b0b0.soulevents.volcano.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.volcano.integration.ArenaRegionService;
import bm.b0b0b0.soulevents.volcano.integration.VaultEconomyService;
import bm.b0b0b0.soulevents.volcano.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import bm.b0b0b0.soulevents.volcano.module.VolcanoModule;
import bm.b0b0b0.soulevents.volcano.util.SerializedItemStackCodec;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class VolcanoService {

    private final SoulEventsApi api;
    private final Plugin plugin;
    private VolcanoPluginConfig config;
    private final VolcanoMessageService messages;
    private final SpawnWorldResolver spawnWorldResolver;
    private final LootRollService lootRollService = new LootRollService();
    private final VolcanoSessionRegistry sessionRegistry = new VolcanoSessionRegistry();
    private final VolcanoRuntimeEffects runtimeEffects;
    private final ArenaRegionService arenaRegionService;
    private VolcanoDespawnBossBarService despawnBossBarService;
    private VaultEconomyService vaultEconomy;
    private Map<String, WorldPlacementGate> gates = Map.of();

    public VolcanoService(SoulEventsApi api, Plugin plugin, VolcanoPluginConfig config, VolcanoMessageService messages) {
        this.api = api;
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.spawnWorldResolver = new SpawnWorldResolver(api.placement(), api.schematics(), api);
        this.runtimeEffects = new VolcanoRuntimeEffects(plugin, messages, sessionRegistry);
        this.arenaRegionService = WorldGuardIntegrations.createArenaRegionService(plugin);
        rebuildGates();
    }

    public void reload(VolcanoPluginConfig config) {
        this.config = config;
        rebuildGates();
    }

    public VolcanoPluginConfig config() {
        return config;
    }

    public VolcanoMessageService messages() {
        return messages;
    }

    public SoulEventsApi api() {
        return api;
    }

    public LootGuardService lootGuard() {
        return api.protection().loot();
    }

    public ArenaRegionService arenaRegions() {
        return arenaRegionService;
    }

    public VolcanoSessionRegistry sessions() {
        return sessionRegistry;
    }

    public void setVaultEconomy(VaultEconomyService vaultEconomy) {
        this.vaultEconomy = vaultEconomy;
    }

    public void setDespawnBossBarService(VolcanoDespawnBossBarService despawnBossBarService) {
        if (this.despawnBossBarService != null) {
            this.despawnBossBarService.stop();
        }
        this.despawnBossBarService = despawnBossBarService;
        if (despawnBossBarService != null) {
            despawnBossBarService.wire(
                    sessionRegistry::snapshot,
                    this::activeEvent,
                    config::type
            );
            despawnBossBarService.start();
        }
    }

    public Optional<ActiveEvent> activeEvent(UUID sessionId) {
        return api.modules().activeEvents(VolcanoModule.MODULE_ID).stream()
                .filter(event -> event.sessionId().equals(sessionId))
                .findFirst();
    }

    public void spawnScheduled(String typeId) {
        Optional<VolcanoTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty() || !typeOptional.get().settings().enabled) {
            return;
        }
        spawnInConfiguredWorldAsync(null, typeId, "scheduler", false);
    }

    public void spawnAdminAsync(CommandSender sender, String typeId) {
        spawnAdminBatchAsync(sender, typeId, 1);
    }

    public void spawnAdminBatchAsync(CommandSender sender, String typeId, int count) {
        Optional<VolcanoTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty()) {
            messages.send(sender, "volcano.unknown-type", Map.of("type", typeId));
            return;
        }
        VolcanoTypeSettings type = typeOptional.get().settings();
        if (!type.summon.adminSummonEnabled && !sender.hasPermission(VolcanoPermissions.STAFF)) {
            messages.send(sender, "command.no-permission", Map.of());
            return;
        }
        int total = Math.max(1, Math.min(count, 10));
        if (total > 1 && sender != null) {
            messages.send(sender, "volcano.admin-batch-start", Map.of("count", Integer.toString(total), "type", typeId));
        }
        spawnAdminBatchAttempt(sender, typeId, total, 0, 0);
    }

    private void spawnAdminBatchAttempt(
            CommandSender feedback,
            String typeId,
            int total,
            int attempt,
            int spawned
    ) {
        if (attempt >= total) {
            if (feedback != null && spawned > 0 && total > 1) {
                messages.send(feedback, "volcano.admin-batch-done", Map.of(
                        "count", Integer.toString(spawned),
                        "type", typeId
                ));
            }
            return;
        }
        spawnInConfiguredWorldAsync(feedback, typeId, "admin", hasBypass(feedback), success -> {
            if (success) {
                spawnAdminBatchAttempt(feedback, typeId, total, attempt + 1, spawned + 1);
            } else if (feedback != null && total > 1) {
                messages.send(feedback, "volcano.admin-batch-stopped", Map.of(
                        "spawned", Integer.toString(spawned),
                        "type", typeId
                ));
            }
        });
    }

    public void spawnPlayerAsync(Player player, String typeId) {
        Optional<VolcanoTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty()) {
            messages.send(player, "volcano.unknown-type", Map.of("type", typeId));
            return;
        }
        VolcanoTypeDefinition definition = typeOptional.get();
        VolcanoTypeSettings type = definition.settings();
        if (!type.summon.playerSummonEnabled) {
            messages.send(player, "volcano.summon-disabled", Map.of());
            return;
        }
        if (!player.hasPermission(VolcanoPermissions.SUMMON)
                && !player.hasPermission(VolcanoPermissions.summonForType(typeId))) {
            messages.send(player, "command.no-permission", Map.of());
            return;
        }
        double cost = type.summon.vaultCost;
        if (cost > 0.0) {
            if (vaultEconomy == null || !vaultEconomy.available()) {
                messages.send(player, "volcano.vault-missing", Map.of(
                        "type_name", resolveTypeName(definition)
                ));
                return;
            }
            if (!vaultEconomy.has(player, cost)) {
                messages.send(player, "volcano.vault-insufficient", Map.of(
                        "type_name", resolveTypeName(definition),
                        "cost", vaultEconomy.format(cost),
                        "balance", vaultEconomy.format(vaultEconomy.balance(player))
                ));
                return;
            }
            if (!vaultEconomy.withdraw(player, cost)) {
                messages.send(player, "volcano.vault-charge-failed", Map.of(
                        "type_name", resolveTypeName(definition),
                        "cost", vaultEconomy.format(cost)
                ));
                return;
            }
            messages.send(player, "volcano.vault-charged", Map.of(
                    "type_name", resolveTypeName(definition),
                    "cost", vaultEconomy.format(cost)
            ));
            spawnInConfiguredWorldAsync(player, typeId, "player", hasBypass(player), success -> {
                if (!success) {
                    vaultEconomy.deposit(player, cost);
                    messages.send(player, "volcano.vault-refunded", Map.of(
                            "type_name", resolveTypeName(definition),
                            "cost", vaultEconomy.format(cost)
                    ));
                }
            });
            return;
        }
        spawnInConfiguredWorldAsync(player, typeId, "player", hasBypass(player));
    }

    public void teleportToActive(Player player, String typeId) {
        List<ActiveEvent> events = api.modules().activeEvents(VolcanoModule.MODULE_ID).stream()
                .filter(event -> event.typeId().equalsIgnoreCase(typeId))
                .toList();
        if (events.isEmpty()) {
            messages.send(player, "volcano.teleport-none", Map.of("type", typeId));
            return;
        }
        Location playerLocation = player.getLocation();
        ActiveEvent target = events.stream()
                .min(Comparator.comparingDouble(event -> event.anchor().distanceSquared(playerLocation)))
                .orElse(events.getFirst());
        Optional<VolcanoTypeDefinition> definitionOptional = config.type(typeId);
        Location anchor = target.anchor().clone().add(0.5, 2.0, 0.5);
        anchor.setPitch(playerLocation.getPitch());
        anchor.setYaw(playerLocation.getYaw());
        player.teleport(anchor);
        if (definitionOptional.isPresent()) {
            messages.send(player, "volcano.teleported", placeholders(typeId, definitionOptional.get(), target.anchor(), player));
        }
    }

    public void handleItemPickup(Player player, Item itemEntity) {
        Optional<UUID> sessionIdOptional = sessionRegistry.sessionIdForEntity(itemEntity.getUniqueId());
        if (sessionIdOptional.isEmpty()) {
            return;
        }
        UUID sessionId = sessionIdOptional.get();
        Optional<VolcanoSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty()) {
            return;
        }
        VolcanoSessionRegistry.LootItem lootItem = recordOptional.get().lootItems().stream()
                .filter(item -> item.entityId().equals(itemEntity.getUniqueId()))
                .findFirst()
                .orElse(null);
        if (lootItem == null || lootItem.claimed()) {
            return;
        }
        ItemStack stack = itemEntity.getItemStack();
        LootGuardService lootGuard = lootGuard();
        var refOptional = lootGuard.obfuscatedRef(stack);
        if (refOptional.isEmpty()) {
            return;
        }
        var ref = refOptional.get();
        if (!lootGuard.canTake(player, ref.sessionId(), ref.slotIndex())) {
            if (lootGuard.isSlotClaimed(ref.sessionId(), ref.slotIndex())) {
                messages.send(player, "volcano.loot-slot-taken", Map.of());
            } else {
                api.messages().send(player, "protection.loot.cooldown", Map.of());
            }
            return;
        }
        if (!lootGuard.tryTakeObfuscated(player, stack, ref.sessionId(), ref.slotIndex())) {
            messages.send(player, "volcano.loot-pickup-failed", Map.of());
            return;
        }
        lootGuard.registerTake(player, ref.sessionId(), ref.slotIndex());
        removeLabel(itemEntity.getWorld(), lootItem.labelId());
        itemEntity.remove();
        sessionRegistry.markLootClaimed(sessionId, itemEntity.getUniqueId());
        sendPickupActionBar(player, sessionId);
    }

    private void sendPickupActionBar(Player player, UUID sessionId) {
        Optional<ActiveEvent> active = activeEvent(sessionId);
        if (active.isEmpty()) {
            return;
        }
        ActiveEvent event = active.get();
        config.type(event.typeId()).ifPresent(definition -> {
            VolcanoVisualSettings visual = definition.settings().visual;
            if (!visual.pickupActionBarEnabled || visual.pickupActionBarKeys.isEmpty()) {
                return;
            }
            String key = visual.pickupActionBarKeys.get(
                    ThreadLocalRandom.current().nextInt(visual.pickupActionBarKeys.size())
            );
            messages.sendActionBar(
                    player,
                    key,
                    placeholders(event.typeId(), definition, null, player)
            );
        });
    }

    public void shutdown() {
        for (ActiveEvent event : api.modules().activeEvents(VolcanoModule.MODULE_ID)) {
            endSession(event.sessionId(), "SHUTDOWN");
        }
        sessionRegistry.shutdown();
        arenaRegionService.shutdown();
        if (despawnBossBarService != null) {
            despawnBossBarService.stop();
        }
    }

    private void spawnInConfiguredWorldAsync(
            CommandSender feedback,
            String typeId,
            String source,
            boolean bypassLimits
    ) {
        spawnInConfiguredWorldAsync(feedback, typeId, source, bypassLimits, ignored -> {
        });
    }

    private void spawnInConfiguredWorldAsync(
            CommandSender feedback,
            String typeId,
            String source,
            boolean bypassLimits,
            Consumer<Boolean> completion
    ) {
        Optional<VolcanoTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty()) {
            if (feedback != null) {
                messages.send(feedback, "volcano.unknown-type", Map.of("type", typeId));
            }
            completion.accept(false);
            return;
        }
        VolcanoTypeDefinition definition = typeOptional.get();
        if (!definition.settings().usesSchematic()) {
            plugin.getLogger().warning("Volcano type " + typeId + " requires schematic.enabled");
            completion.accept(false);
            return;
        }
        WorldPlacementGate gate = gate(typeId);
        String spawnWorldName = spawnWorldResolver.configuredWorldName(definition.settings());
        World spawnWorld = Bukkit.getWorld(spawnWorldName);
        SpawnSearchDebug debug = new SpawnSearchDebug(
                plugin,
                config.module().spawnDebugEnabled,
                typeId
        );
        if (spawnWorld == null) {
            plugin.getLogger().warning(
                    "Spawn failed type=" + typeId + " source=" + source + ": world not loaded " + spawnWorldName
            );
            if (feedback != null) {
                messages.send(feedback, "volcano.world-not-found", Map.of("world", spawnWorldName));
            }
            completion.accept(false);
            return;
        }
        WorldPlacementResult worldCheck = gate.checkWorld(spawnWorld);
        if (!worldCheck.allowed()) {
            debug.finishFailedWorld(
                    spawnWorldName,
                    SpawnSearchDebug.gateReason(worldCheck.denial().name(), worldCheck.regionName())
            );
            plugin.getLogger().warning(
                    "Spawn failed type=" + typeId + " source=" + source + ": " + debug.failureMessage()
            );
            if (feedback != null) {
                sendPlacementError(feedback, worldCheck);
            }
            completion.accept(false);
            return;
        }
        plugin.getLogger().info(
                "Spawn search type=" + typeId
                        + " world=" + spawnWorldName
                        + " source=" + source
                        + " debug=" + config.module().spawnDebugEnabled
        );
        spawnWorldResolver.resolveAsync(plugin, definition.settings(), gate, debug, location -> {
            if (location.isEmpty()) {
                plugin.getLogger().warning(
                        "Spawn failed type=" + typeId
                                + " source=" + source + ": " + debug.failureMessage()
                );
                if (feedback != null) {
                    messages.send(feedback, "volcano.spawn-failed", Map.of(
                            "type", typeId,
                            "type_name", resolveTypeName(definition),
                            "world", spawnWorldName
                    ));
                }
                completion.accept(false);
                return;
            }
            if (!tryStart(typeId, definition, location.get(), bypassLimits)) {
                plugin.getLogger().warning(
                        "Spawn failed type=" + typeId
                                + " source=" + source + ": concurrent limit reached"
                );
                if (feedback != null) {
                    messages.send(feedback, "volcano.limit-reached", Map.of("type", typeId));
                }
                completion.accept(false);
                return;
            }
            plugin.getLogger().info(
                    "Spawn started type=" + typeId
                            + " at " + location.get().getBlockX() + ", "
                            + location.get().getBlockY() + ", "
                            + location.get().getBlockZ()
                            + " world=" + spawnWorldName
                            + " source=" + source
            );
            if (feedback != null) {
                String messageKey = "admin".equals(source) ? "volcano.admin-summoned" : "volcano.summoned";
                messages.send(feedback, messageKey, placeholders(typeId, definition, location.get(), null));
            }
            completion.accept(true);
        });
    }

    private boolean tryStart(
            String typeId,
            VolcanoTypeDefinition definition,
            Location pasteOrigin,
            boolean bypassLimits
    ) {
        if (!canSpawn(typeId, definition.settings(), bypassLimits)) {
            return false;
        }
        startSessionWithSchematic(typeId, definition, pasteOrigin);
        return true;
    }

    private void startSessionWithSchematic(String typeId, VolcanoTypeDefinition definition, Location pasteOrigin) {
        VolcanoTypeSettings type = definition.settings();
        String schematicId = type.schematicId();
        Optional<Location> ventOptional = api.schematics().resolveChestAnchor(pasteOrigin, schematicId);
        if (ventOptional.isEmpty()) {
            plugin.getLogger().warning("Volcano vent anchor missing for type " + typeId);
            return;
        }
        Location ventAnchor = blockAnchor(ventOptional.get());
        ActiveEvent session = api.sessions().start(VolcanoModule.MODULE_ID, typeId, ventAnchor);
        UUID sessionId = session.sessionId();

        int eruptDelay = Math.max(0, type.lifecycle.eruptDelaySeconds);
        Instant eruptAt = Instant.now().plusSeconds(eruptDelay);
        int activeSeconds = Math.max(1, type.lifecycle.maxActiveSeconds);
        Instant extinguishAt = eruptAt.plusSeconds(activeSeconds);

        api.sessions().setLootableAt(sessionId, eruptAt);
        api.sessions().setPhase(sessionId, EventPhase.PREPARING);

        SchematicSpawnOverrides spawnOverrides = SchematicSpawnOverridesFactory.from(type.schematic);
        SchematicPasteOptions options = SchematicPasteOptions.of(sessionId);
        Optional<SchematicWorldBounds> schematicBounds = api.schematics().worldBounds(schematicId, pasteOrigin);
        arenaRegionService.create(sessionId, ventAnchor, type.arenaWorldGuard, schematicBounds);

        api.schematics().paste(schematicId, pasteOrigin, options, spawnOverrides).thenAccept(result ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!result.success()) {
                        plugin.getLogger().warning(
                                "Volcano schematic paste failed type=" + typeId
                                        + " session=" + sessionId
                                        + " schematic=" + schematicId
                                        + " at " + pasteOrigin.getBlockX() + ", "
                                        + pasteOrigin.getBlockY() + ", "
                                        + pasteOrigin.getBlockZ()
                        );
                        endSession(sessionId, "SCHEMATIC_FAILED");
                        return;
                    }
                    Location resolvedVent = blockAnchor(result.chestAnchor());
                    plugin.getLogger().info(
                            "Volcano schematic pasted type=" + typeId
                                    + " session=" + sessionId
                                    + " vent=" + resolvedVent.getBlockX() + ", "
                                    + resolvedVent.getBlockY() + ", "
                                    + resolvedVent.getBlockZ()
                                    + " eruptDelaySec=" + eruptDelay
                    );
                    finalizeSessionStart(
                            typeId,
                            definition,
                            resolvedVent,
                            blockAnchor(pasteOrigin),
                            sessionId,
                            eruptAt,
                            extinguishAt,
                            schematicBounds
                    );
                })
        );
    }

    private void finalizeSessionStart(
            String typeId,
            VolcanoTypeDefinition definition,
            Location ventAnchor,
            Location pasteOrigin,
            UUID sessionId,
            Instant eruptAt,
            Instant extinguishAt,
            Optional<SchematicWorldBounds> schematicBounds
    ) {
        VolcanoTypeSettings type = definition.settings();
        sessionRegistry.register(sessionId, ventAnchor, pasteOrigin, eruptAt, extinguishAt, schematicBounds);
        arenaRegionService.create(sessionId, ventAnchor, type.arenaWorldGuard, schematicBounds);
        runtimeEffects.start(sessionId, definition, sessionRegistry.find(sessionId).orElseThrow());

        api.sessions().setPhase(sessionId, EventPhase.ACTIVE);

        if (type.broadcast.enabled && type.broadcast.spawnEnabled) {
            messages.broadcast(type.broadcast.messageKey, placeholders(typeId, definition, ventAnchor, null));
        }

        long eruptDelayTicks = Math.max(0L, eruptAt.toEpochMilli() - System.currentTimeMillis()) / 50L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> erupt(sessionId, definition), eruptDelayTicks);
        scheduleSessionEnd(sessionId, extinguishAt, "EXPIRED");
    }

    private void erupt(UUID sessionId, VolcanoTypeDefinition definition) {
        Optional<VolcanoSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty() || activeEvent(sessionId).isEmpty()) {
            return;
        }
        VolcanoSessionRegistry.SessionRecord record = recordOptional.get();
        if (record.erupted()) {
            return;
        }
        sessionRegistry.markErupted(sessionId);

        VolcanoTypeSettings type = definition.settings();
        LootTableSettings loot = definition.loot();
        int itemCount = Math.max(1, type.eruption.itemCount);
        List<ItemStack> rolled = lootRollService.rollForEruption(loot, itemCount);
        plugin.getLogger().info(
                "Volcano erupt session=" + sessionId
                        + " type=" + definition.id()
                        + " items=" + rolled.size()
                        + " continuous until " + record.extinguishAt()
        );
        List<ItemStack> masks = SerializedItemStackCodec.decodeAll(loot.obfuscationItemsBase64);
        ArrayDeque<ItemStack> queue = new ArrayDeque<>(rolled);

        if (type.broadcast.enabled && type.broadcast.unlockedEnabled) {
            messages.broadcast(type.broadcast.unlockedMessageKey, placeholders(
                    definition.id(),
                    definition,
                    record.ventAnchor(),
                    null
            ));
        }

        scheduleContinuousEruption(sessionId, definition, queue, masks, 0);
    }

    private void scheduleContinuousEruption(
            UUID sessionId,
            VolcanoTypeDefinition definition,
            ArrayDeque<ItemStack> queue,
            List<ItemStack> masks,
            int slotIndex
    ) {
        Optional<VolcanoSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty() || activeEvent(sessionId).isEmpty()) {
            return;
        }
        VolcanoSessionRegistry.SessionRecord record = recordOptional.get();
        if (Instant.now().isAfter(record.extinguishAt())) {
            return;
        }
        ItemStack next = queue.poll();
        if (next == null) {
            return;
        }
        Location vent = record.ventAnchor().clone().add(0.5, 0.0, 0.5);
        if (vent.getWorld() == null) {
            return;
        }
        launchLootItem(sessionId, definition, vent, next, masks, slotIndex);

        if (queue.isEmpty()) {
            return;
        }
        VolcanoEruptionSettings eruption = definition.settings().eruption;
        int minTicks = Math.max(1, eruption.minTicksBetweenLaunches);
        int maxTicks = Math.max(minTicks, eruption.maxTicksBetweenLaunches);
        int delay = ThreadLocalRandom.current().nextInt(minTicks, maxTicks + 1);
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> scheduleContinuousEruption(sessionId, definition, queue, masks, slotIndex + 1),
                delay
        );
    }

    private void launchLootItem(
            UUID sessionId,
            VolcanoTypeDefinition definition,
            Location vent,
            ItemStack real,
            List<ItemStack> masks,
            int slotIndex
    ) {
        if (sessionRegistry.find(sessionId).isEmpty()) {
            return;
        }
        ItemStack mask = pickObfuscationMask(masks);
        ItemStack obfuscated = lootGuard().obfuscate(real, sessionId, slotIndex, mask);
        Item entity = runtimeEffects.launchLootItem(vent, obfuscated, definition.settings().eruption);
        if (entity == null) {
            return;
        }
        TextDisplay label = runtimeEffects.spawnItemLabel(
                vent.getWorld(),
                entity.getLocation(),
                definition.settings().visual,
                definition
        );
        sessionRegistry.addLootItem(
                sessionId,
                new VolcanoSessionRegistry.LootItem(
                        entity.getUniqueId(),
                        label == null ? null : label.getUniqueId(),
                        slotIndex,
                        false
                )
        );
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

    private void endSession(UUID sessionId, String phase) {
        plugin.getLogger().info("Volcano end session=" + sessionId + " phase=" + phase);
        if (!"SHUTDOWN".equals(phase)) {
            Optional<ActiveEvent> active = activeEvent(sessionId);
            Optional<VolcanoSessionRegistry.SessionRecord> record = sessionRegistry.find(sessionId);
            if (active.isPresent() && record.isPresent()) {
                config.type(active.get().typeId()).ifPresent(definition -> broadcastLifecycle(
                        active.get().typeId(),
                        definition,
                        record.get().anchor(),
                        definition.settings().broadcast,
                        definition.settings().broadcast.removedMessageKey,
                        definition.settings().broadcast.removedEnabled
                ));
            }
        }
        cleanupLootEntities(sessionId);
        runtimeEffects.stop(sessionId);
        arenaRegionService.remove(sessionId);
        sessionRegistry.remove(sessionId);
        lootGuard().clearSession(sessionId);
        api.schematics().undo(sessionId);
        api.sessions().end(sessionId);
    }

    private void cleanupLootEntities(UUID sessionId) {
        sessionRegistry.find(sessionId).ifPresent(record -> {
            World world = record.ventAnchor().getWorld();
            if (world == null) {
                return;
            }
            for (VolcanoSessionRegistry.LootItem lootItem : record.lootItems()) {
                if (lootItem.claimed()) {
                    continue;
                }
                world.getEntities().stream()
                        .filter(entity -> entity.getUniqueId().equals(lootItem.entityId()))
                        .forEach(entity -> entity.remove());
                removeLabel(world, lootItem.labelId());
            }
        });
    }

    private static void removeLabel(World world, UUID labelId) {
        if (labelId == null) {
            return;
        }
        world.getEntities().stream()
                .filter(entity -> entity.getUniqueId().equals(labelId))
                .forEach(entity -> entity.remove());
    }

    private boolean canSpawn(String typeId, VolcanoTypeSettings type, boolean bypassConcurrentLimit) {
        List<ActiveEvent> active = api.modules().activeEvents(VolcanoModule.MODULE_ID).stream().toList();
        if (active.size() >= config.module().maxConcurrentTotal) {
            return false;
        }
        if (bypassConcurrentLimit) {
            return true;
        }
        long typeActive = active.stream().filter(event -> event.typeId().equals(typeId)).count();
        return typeActive < type.maxConcurrent;
    }

    private WorldPlacementGate gate(String typeId) {
        return gates.getOrDefault(typeId, new WorldPlacementGate(
                config.typeSettings(typeId).worldPlacement,
                WorldGuardIntegrations.createProbe()
        ));
    }

    private void rebuildGates() {
        Map<String, WorldPlacementGate> rebuilt = new HashMap<>();
        for (VolcanoTypeDefinition definition : config.types()) {
            rebuilt.put(definition.id(), new WorldPlacementGate(
                    definition.settings().worldPlacement,
                    WorldGuardIntegrations.createProbe()
            ));
        }
        gates = Map.copyOf(rebuilt);
    }

    private static Location blockAnchor(Location anchor) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX(),
                anchor.getBlockY(),
                anchor.getBlockZ()
        );
    }

    private static ItemStack pickObfuscationMask(List<ItemStack> masks) {
        if (masks.isEmpty()) {
            return null;
        }
        return masks.get(ThreadLocalRandom.current().nextInt(masks.size()));
    }

    private void broadcastLifecycle(
            String typeId,
            VolcanoTypeDefinition definition,
            Location anchor,
            BroadcastSettings broadcast,
            String messageKey,
            boolean enabled
    ) {
        if (!broadcast.enabled || !enabled) {
            return;
        }
        messages.broadcast(messageKey, placeholders(typeId, definition, anchor, null));
    }

    private void sendPlacementError(CommandSender sender, WorldPlacementResult result) {
        messages.send(sender, result.messageKey().orElse("volcano.placement.invalid-location"), Map.of(
                "world", result.worldName(),
                "region", result.regionName(),
                "distance", result.regionName()
        ));
    }

    private Map<String, String> placeholders(
            String typeId,
            VolcanoTypeDefinition definition,
            Location anchor,
            Player player
    ) {
        Map<String, String> values = new HashMap<>();
        values.put("type", typeId);
        values.put("type_name", resolveTypeName(definition));
        if (anchor != null && anchor.getWorld() != null) {
            values.put("world", anchor.getWorld().getName());
            values.put("x", Integer.toString(anchor.getBlockX()));
            values.put("y", Integer.toString(anchor.getBlockY()));
            values.put("z", Integer.toString(anchor.getBlockZ()));
        }
        if (player != null) {
            values.put("player", player.getName());
        }
        return values;
    }

    private String resolveTypeName(VolcanoTypeDefinition definition) {
        return messages.resolvePlain(definition.settings().displayNameKey, Map.of());
    }

    private static boolean hasBypass(CommandSender sender) {
        return sender != null && sender.hasPermission(VolcanoPermissions.BYPASS);
    }
}
