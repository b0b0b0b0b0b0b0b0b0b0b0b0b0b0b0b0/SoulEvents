package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import bm.b0b0b0.soulevents.api.module.EventPhase;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOptions;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.api.world.WorldPlacementDenial;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPermissions;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.chest.AirDropChestHolder;
import bm.b0b0b0.soulevents.airdrop.config.settings.BroadcastSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.airdrop.config.TypeConfigPersistence;
import bm.b0b0b0.soulevents.airdrop.util.RequiredItemMatcher;
import bm.b0b0b0.soulevents.airdrop.util.SerializedItemStackCodec;
import bm.b0b0b0.soulevents.airdrop.config.settings.WorldPlacementSettings;
import bm.b0b0b0.soulevents.api.protection.GateContext;
import bm.b0b0b0.soulevents.api.protection.GateResult;
import bm.b0b0b0.soulevents.api.protection.LootGuardService;
import bm.b0b0b0.soulevents.api.protection.ObfuscatedLootRef;
import bm.b0b0b0.soulevents.airdrop.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.airdrop.integration.ArenaRegionService;
import bm.b0b0b0.soulevents.airdrop.integration.VaultEconomyService;
import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
import bm.b0b0b0.soulevents.airdrop.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import bm.b0b0b0.soulevents.airdrop.module.AirDropModule;
import bm.b0b0b0.soulevents.airdrop.repository.SqlAirDropSessionRepository;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class AirDropService {

    private static final int LOOT_SLOT_STRIDE = 1000;

    private final SoulEventsApi api;
    private final Plugin plugin;
    private final AirDropMessageService messages;
    private final SqlAirDropSessionRepository sessionRepository;
    private final SpawnWorldResolver spawnWorldResolver;
    private final LootRollService lootRollService = new LootRollService();
    private final WorldGuardProbe worldGuardProbe = WorldGuardIntegrations.createProbe();
    private final AirDropSessionRegistry sessionRegistry = new AirDropSessionRegistry();
    private ArenaRegionService arenaRegionService;
    private AirDropPluginConfig config;
    private AirDropVisualService visualService;
    private AirDropDespawnBossBarService despawnBossBarService;
    private VaultEconomyService vaultEconomy;
    private AirDropPreOpenService preOpenService;
    private Map<String, WorldPlacementGate> gates = Map.of();

    public AirDropService(
            SoulEventsApi api,
            Plugin plugin,
            AirDropPluginConfig config,
            AirDropMessageService messages,
            SqlAirDropSessionRepository sessionRepository
    ) {
        this.api = api;
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.sessionRepository = sessionRepository;
        this.spawnWorldResolver = new SpawnWorldResolver(api.placement(), api.schematics(), api);
        this.arenaRegionService = WorldGuardIntegrations.createArenaRegionService(plugin);
        rebuildGates();
    }

    public void setVisualService(AirDropVisualService visualService) {
        this.visualService = visualService;
        visualService.setChestCallbacks(
                sessionId -> sessionRegistry.find(sessionId).map(AirDropSessionRegistry.SessionRecord::isChestIntact).orElse(false),
                this::handleChestMissing,
                sessionId -> sessionRegistry.find(sessionId)
                        .map(AirDropSessionRegistry.SessionRecord::cleanupAt)
                        .filter(instant -> instant != null),
                sessionId -> sessionRegistry.find(sessionId).map(AirDropSessionRegistry.SessionRecord::looted).orElse(false),
                sessionId -> sessionRegistry.find(sessionId).map(this::isSessionChestGuiEmpty).orElse(false),
                this::notifyChestGuiEmpty
        );
    }

    public void setDespawnBossBarService(AirDropDespawnBossBarService despawnBossBarService) {
        if (this.despawnBossBarService != null) {
            this.despawnBossBarService.stop();
        }
        this.despawnBossBarService = despawnBossBarService;
        despawnBossBarService.wire(
                sessionRegistry::snapshot,
                this::activeEvent,
                config::type
        );
        despawnBossBarService.start();
    }

    public void setVaultEconomy(VaultEconomyService vaultEconomy) {
        this.vaultEconomy = vaultEconomy;
    }

    public void setPreOpenService(AirDropPreOpenService preOpenService) {
        this.preOpenService = preOpenService;
    }

    public void handleChestMissing(UUID sessionId) {
        restoreChestAnchor(sessionId);
    }

    private void restoreChestAnchor(UUID sessionId) {
        Optional<AirDropSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty()) {
            return;
        }
        AirDropSessionRegistry.SessionRecord record = recordOptional.get();
        if (record.isChestIntact()) {
            return;
        }
        Optional<ActiveEvent> active = activeEvent(sessionId);
        if (active.isEmpty()) {
            endSession(sessionId, "ANCHOR_LOST");
            return;
        }
        Optional<AirDropTypeDefinition> definition = config.type(active.get().typeId());
        if (definition.isEmpty()) {
            endSession(sessionId, "ANCHOR_LOST");
            return;
        }
        ensureLootChests(sessionId, definition.get(), record);
    }

    public void reload(AirDropPluginConfig config) {
        this.config = config;
        rebuildGates();
    }

    public AirDropPluginConfig config() {
        return config;
    }

    public ArenaRegionService arenaRegions() {
        return arenaRegionService;
    }

    public AirDropMessageService messages() {
        return messages;
    }

    public LootGuardService lootGuard() {
        return api.protection().loot();
    }

    public SoulEventsApi api() {
        return api;
    }

    public Optional<UUID> sessionIdAt(Location location) {
        return sessionRegistry.sessionIdAt(location);
    }

    public Optional<ActiveEvent> activeEvent(UUID sessionId) {
        return api.modules().activeEvents(AirDropModule.MODULE_ID).stream()
                .filter(event -> event.sessionId().equals(sessionId))
                .findFirst();
    }

    public void tryOpenChest(Player player, UUID sessionId, Location clickedBlock) {
        Optional<AirDropSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty()) {
            messages.send(player, "airdrop.chest-open-failed", Map.of());
            return;
        }
        AirDropSessionRegistry.SessionRecord record = recordOptional.get();
        AirDropSessionRegistry.ChestClickTarget clickTarget = record.resolveClick(clickedBlock)
                .orElse(null);
        if (clickTarget == null) {
            messages.send(player, "airdrop.chest-open-failed", Map.of());
            return;
        }
        int pointIndex = clickTarget.pointIndex();
        int clusterIndex = clickTarget.clusterIndex();
        AirDropSessionRegistry.LootPoint lootPoint = record.lootPoints().get(pointIndex);
        Optional<ActiveEvent> active = activeEvent(sessionId);
        if (active.isEmpty()) {
            messages.send(player, "airdrop.chest-unavailable", Map.of());
            return;
        }
        Optional<AirDropTypeDefinition> definitionOptional = config.type(active.get().typeId());
        if (definitionOptional.isEmpty()) {
            messages.send(player, "airdrop.chest-open-failed", Map.of());
            return;
        }
        AirDropTypeDefinition definition = definitionOptional.get();
        AirDropTypeSettings type = definition.settings();
        Instant lootableAt = active.get().lootableAt().orElse(Instant.EPOCH);
        if (Instant.now().isBefore(lootableAt)) {
            long seconds = Math.max(1L, Duration.between(Instant.now(), lootableAt).toSeconds());
            messages.send(player, "airdrop.chest-not-ready", Map.of("timer", formatOpenTimer(seconds)));
            return;
        }
        GateResult gate = api.protection().gates().check(
                player,
                sessionId,
                GateContext.OPEN_CONTAINER,
                type.gateProfileId
        );
        if (!gate.allowed()) {
            sendGateDenial(player, gate);
            return;
        }
        if (type.openPermission.enabled) {
            String permission = TypeConfigPersistence.resolveOpenPermission(active.get().typeId(), type.openPermission.permission);
            if (!player.hasPermission(permission)) {
                messages.send(player, "airdrop.open-permission-denied", Map.of("permission", permission));
                return;
            }
        }
        if (type.requiredLoot.enabled && !RequiredItemMatcher.hasRequiredItem(player, type.requiredLoot)) {
            messages.send(player, "airdrop.required-item-missing", Map.of());
            return;
        }
        if (lootPoint.isDecoyCluster() && clusterIndex != lootPoint.clusterLootSlotIndex()) {
            messages.send(player, "airdrop.chest-decoy", Map.of());
            return;
        }
        final int holderIndex = AirDropSessionRegistry.sequentialChestIndex(record.lootPoints(), pointIndex, clusterIndex);
        AirDropChestHolder holder = record.lootChest(holderIndex).orElse(null);
        if (holder == null) {
            ensureLootChests(sessionId, definition, record);
            holder = sessionRegistry.find(sessionId)
                    .flatMap(current -> current.lootChest(holderIndex))
                    .orElse(null);
        }
        if (holder == null) {
            messages.send(player, "airdrop.chest-open-failed", Map.of());
            return;
        }
        player.openInventory(holder.getInventory());
        onChestFirstOpened(sessionId, player);
    }

    private void sendGateDenial(Player player, GateResult gate) {
        String messageKey = gate.messageKey();
        if (messageKey == null || messageKey.isEmpty()) {
            messages.send(player, "airdrop.chest-open-denied", Map.of());
            return;
        }
        if (messageKey.startsWith("protection.")) {
            api.messages().send(player, messageKey, Map.of());
            return;
        }
        messages.send(player, messageKey, Map.of());
    }

    private static String formatOpenTimer(long seconds) {
        long minutes = seconds / 60L;
        long rest = seconds % 60L;
        if (minutes > 0L) {
            return minutes + "м " + rest + "с";
        }
        return rest + "с";
    }

    public void onChestFirstOpened(UUID sessionId, Player player) {
        Optional<AirDropSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty() || recordOptional.get().opened()) {
            return;
        }
        Optional<ActiveEvent> active = activeEvent(sessionId);
        if (active.isEmpty()) {
            return;
        }
        Optional<AirDropTypeDefinition> definitionOptional = config.type(active.get().typeId());
        if (definitionOptional.isEmpty()) {
            return;
        }
        AirDropTypeDefinition definition = definitionOptional.get();
        AirDropTypeSettings type = definition.settings();
        sessionRegistry.markOpened(sessionId);
        api.sessions().setPhase(sessionId, EventPhase.LOOTABLE);
        broadcastLifecycle(
                sessionId,
                active.get().typeId(),
                definition,
                recordOptional.get().anchor(),
                type.broadcast,
                type.broadcast.openedMessageKey,
                type.broadcast.openedEnabled,
                player
        );
    }

    public void scheduleEmptyCheck(UUID sessionId, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> checkChestEmptied(sessionId, player));
    }

    public void notifyChestGuiEmpty(UUID sessionId) {
        checkChestEmptied(sessionId, null);
    }

    private boolean isSessionChestGuiEmpty(AirDropSessionRegistry.SessionRecord record) {
        if (!record.hasLootChests()) {
            return false;
        }
        for (int pointIndex = 0; pointIndex < record.lootPoints().size(); pointIndex++) {
            AirDropSessionRegistry.LootPoint point = record.lootPoints().get(pointIndex);
            int chestCount = point.clusterEnabled() ? AirDropClusterChestPlacer.CLUSTER_CHEST_COUNT : 1;
            for (int clusterIndex = 0; clusterIndex < chestCount; clusterIndex++) {
                if (point.isDecoyCluster() && clusterIndex != point.clusterLootSlotIndex()) {
                    continue;
                }
                int holderIndex = AirDropSessionRegistry.sequentialChestIndex(record.lootPoints(), pointIndex, clusterIndex);
                AirDropChestHolder holder = record.lootChest(holderIndex).orElse(null);
                if (holder == null) {
                    continue;
                }
                if (!isInventoryEmpty(holder.getInventory())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkChestEmptied(UUID sessionId, Player player) {
        Optional<AirDropSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty() || recordOptional.get().looted()) {
            return;
        }
        AirDropSessionRegistry.SessionRecord record = recordOptional.get();
        if (!record.hasLootChests()) {
            return;
        }
        if (!isSessionChestGuiEmpty(record)) {
            return;
        }
        onChestLooted(sessionId, player);
    }

    private void onChestLooted(UUID sessionId, Player player) {
        Optional<AirDropSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty() || recordOptional.get().looted()) {
            return;
        }
        Optional<ActiveEvent> active = activeEvent(sessionId);
        if (active.isEmpty()) {
            return;
        }
        Optional<AirDropTypeDefinition> definitionOptional = config.type(active.get().typeId());
        if (definitionOptional.isEmpty()) {
            return;
        }
        AirDropTypeDefinition definition = definitionOptional.get();
        AirDropTypeSettings type = definition.settings();
        sessionRegistry.markLooted(sessionId);
        broadcastLifecycle(
                sessionId,
                active.get().typeId(),
                definition,
                recordOptional.get().anchor(),
                type.broadcast,
                type.broadcast.lootedMessageKey,
                type.broadcast.lootedEnabled,
                player
        );
        int cleanupSeconds = Math.max(1, type.lifecycle.cleanupSecondsAfterLooted);
        Instant cleanupAt = Instant.now().plusSeconds(cleanupSeconds);
        scheduleSessionEnd(sessionId, cleanupAt, "LOOTED");
    }

    private void scheduleSessionEnd(UUID sessionId, Instant endAt, String phase) {
        long delaySeconds = Math.max(0L, Duration.between(Instant.now(), endAt).toSeconds());
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> endSession(sessionId, phase),
                delaySeconds * 20L
        );
        sessionRegistry.rescheduleCleanup(sessionId, task, endAt);
    }

    public void spawnScheduled(String typeId) {
        Optional<AirDropTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty() || !typeOptional.get().settings().enabled) {
            return;
        }
        spawnInConfiguredWorldAsync(null, typeId, "scheduler", false);
    }

    public void spawnAdminAsync(CommandSender sender, String typeId) {
        spawnAdminBatchAsync(sender, typeId, 1);
    }

    public void spawnAdminBatchAsync(CommandSender sender, String typeId, int count) {
        Optional<AirDropTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty()) {
            messages.send(sender, "airdrop.unknown-type", Map.of("type", typeId));
            return;
        }
        AirDropTypeSettings type = typeOptional.get().settings();
        if (!type.summon.adminSummonEnabled && !sender.hasPermission(AirDropPermissions.STAFF)) {
            messages.send(sender, "command.no-permission", Map.of());
            return;
        }
        int total = Math.max(1, Math.min(count, 10));
        if (total > 1 && sender != null) {
            messages.send(sender, "airdrop.admin-batch-start", Map.of("count", Integer.toString(total), "type", typeId));
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
                messages.send(feedback, "airdrop.admin-batch-done", Map.of(
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
                messages.send(feedback, "airdrop.admin-batch-stopped", Map.of(
                        "spawned", Integer.toString(spawned),
                        "type", typeId
                ));
            }
        });
    }

    public void teleportToActive(Player player, String typeId) {
        List<ActiveEvent> events = api.modules().activeEvents(AirDropModule.MODULE_ID).stream()
                .filter(event -> event.typeId().equalsIgnoreCase(typeId))
                .toList();
        if (events.isEmpty()) {
            messages.send(player, "airdrop.teleport-none", Map.of("type", typeId));
            return;
        }
        Location playerLocation = player.getLocation();
        ActiveEvent target = events.stream()
                .min(Comparator.comparingDouble(event -> event.anchor().distanceSquared(playerLocation)))
                .orElse(events.getFirst());
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        Location anchor = target.anchor().clone().add(0.5, 2.0, 0.5);
        anchor.setPitch(playerLocation.getPitch());
        anchor.setYaw(playerLocation.getYaw());
        player.teleport(anchor);
        if (definitionOptional.isPresent()) {
            messages.send(player, "airdrop.teleported", placeholders(typeId, definitionOptional.get(), target.anchor(), player));
        } else {
            messages.send(player, "airdrop.teleported", Map.of(
                    "type", typeId,
                    "world", target.anchor().getWorld().getName(),
                    "x", Integer.toString(target.anchor().getBlockX()),
                    "y", Integer.toString(target.anchor().getBlockY()),
                    "z", Integer.toString(target.anchor().getBlockZ())
            ));
        }
    }

    public void spawnPlayerAsync(Player player, String typeId) {
        Optional<AirDropTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty()) {
            messages.send(player, "airdrop.unknown-type", Map.of("type", typeId));
            return;
        }
        AirDropTypeDefinition definition = typeOptional.get();
        AirDropTypeSettings type = definition.settings();
        if (!type.summon.playerSummonEnabled) {
            messages.send(player, "airdrop.summon-disabled", Map.of());
            return;
        }
        if (!player.hasPermission(AirDropPermissions.SUMMON)
                && !player.hasPermission(AirDropPermissions.summonForType(typeId))) {
            messages.send(player, "command.no-permission", Map.of());
            return;
        }
        double cost = type.summon.vaultCost;
        if (cost > 0.0) {
            if (vaultEconomy == null || !vaultEconomy.available()) {
                messages.send(player, "airdrop.vault-missing", Map.of(
                        "type_name", resolveTypeName(definition)
                ));
                return;
            }
            if (!vaultEconomy.has(player, cost)) {
                messages.send(player, "airdrop.vault-insufficient", Map.of(
                        "type_name", resolveTypeName(definition),
                        "cost", vaultEconomy.format(cost),
                        "balance", vaultEconomy.format(vaultEconomy.balance(player))
                ));
                return;
            }
            if (!vaultEconomy.withdraw(player, cost)) {
                messages.send(player, "airdrop.vault-charge-failed", Map.of(
                        "type_name", resolveTypeName(definition),
                        "cost", vaultEconomy.format(cost)
                ));
                return;
            }
            messages.send(player, "airdrop.vault-charged", Map.of(
                    "type_name", resolveTypeName(definition),
                    "cost", vaultEconomy.format(cost)
            ));
            spawnInConfiguredWorldAsync(player, typeId, "player", hasBypass(player), success -> {
                if (!success) {
                    vaultEconomy.deposit(player, cost);
                    messages.send(player, "airdrop.vault-refunded", Map.of(
                            "type_name", resolveTypeName(definition),
                            "cost", vaultEconomy.format(cost)
                    ));
                }
            });
            return;
        }
        spawnInConfiguredWorldAsync(player, typeId, "player", hasBypass(player));
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
        Optional<AirDropTypeDefinition> typeOptional = config.type(typeId);
        if (typeOptional.isEmpty()) {
            if (feedback != null) {
                messages.send(feedback, "airdrop.unknown-type", Map.of("type", typeId));
            }
            completion.accept(false);
            return;
        }
        AirDropTypeDefinition definition = typeOptional.get();
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
                    "Spawn failed type=" + typeId
                            + " source=" + source + ": " + debug.failureMessage()
            );
            if (feedback != null) {
                messages.send(feedback, "airdrop.world-not-found", Map.of("world", spawnWorldName));
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
                    "Spawn failed type=" + typeId
                            + " source=" + source + ": " + debug.failureMessage()
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
        );
        spawnWorldResolver.resolveAsync(plugin, definition.settings(), gate, debug, location -> {
            if (location.isEmpty()) {
                plugin.getLogger().warning(
                        "Spawn failed type=" + typeId
                                + " source=" + source + ": " + debug.failureMessage()
                );
                if (feedback != null) {
                    messages.send(feedback, "airdrop.spawn-failed", Map.of(
                            "type", typeId,
                            "type_name", resolveTypeName(definition),
                            "world", spawnWorldName
                    ));
                }
                completion.accept(false);
                return;
            }
            if (!tryStart(typeId, definition, location.get(), source, bypassLimits)) {
                plugin.getLogger().warning(
                        "Spawn failed type=" + typeId
                                + " source=" + source + ": concurrent limit reached"
                );
                if (feedback != null) {
                    messages.send(feedback, "airdrop.limit-reached", Map.of("type", typeId));
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
                String messageKey = "admin".equals(source) ? "airdrop.admin-summoned" : "airdrop.summoned";
                messages.send(feedback, messageKey, placeholders(typeId, definition, location.get(), null));
            }
            completion.accept(true);
        });
    }

    public void shutdown() {
        for (ActiveEvent event : api.modules().activeEvents(AirDropModule.MODULE_ID)) {
            endSession(event.sessionId(), "SHUTDOWN");
        }
        sessionRegistry.shutdown();
        arenaRegionService.shutdown();
        if (visualService != null) {
            visualService.shutdown();
        }
        if (despawnBossBarService != null) {
            despawnBossBarService.stop();
        }
        if (preOpenService != null) {
            preOpenService.shutdown();
        }
    }

    private boolean tryStart(
            String typeId,
            AirDropTypeDefinition definition,
            Location anchor,
            String source,
            boolean bypassLimits
    ) {
        if (!canSpawn(typeId, definition.settings(), bypassLimits)) {
            return false;
        }
        if (definition.settings().usesSchematic()) {
            startSessionWithSchematic(typeId, definition, anchor, source);
            return true;
        }
        startSession(typeId, definition, anchor, source);
        return true;
    }

    private void startSessionWithSchematic(
            String typeId,
            AirDropTypeDefinition definition,
            Location pasteOrigin,
            String source
    ) {
        AirDropTypeSettings type = definition.settings();
        String schematicId = type.schematicId();
        Optional<Location> chestOptional = api.schematics().resolveChestAnchor(pasteOrigin, schematicId);
        if (chestOptional.isEmpty()) {
            plugin.getLogger().warning("Schematic chest anchor missing for type " + typeId);
            return;
        }
        Location blockAnchor = blockAnchor(chestOptional.get());
        ActiveEvent session = api.sessions().start(AirDropModule.MODULE_ID, typeId, blockAnchor);
        UUID sessionId = session.sessionId();
        Instant lootableAt = computeLootableAt(type);
        api.sessions().setLootableAt(sessionId, lootableAt);
        api.sessions().setPhase(sessionId, EventPhase.PREPARING);
        sessionRepository.insertSession(sessionId, typeId, blockAnchor, source);

        SchematicSpawnOverrides spawnOverrides = SchematicSpawnOverridesFactory.from(type.schematic);
        SchematicPasteOptions options = SchematicPasteOptions.of(sessionId);
        Optional<SchematicWorldBounds> schematicBounds = api.schematics()
                .worldBounds(schematicId, pasteOrigin);
        arenaRegionService.create(sessionId, blockAnchor, type.arenaWorldGuard, schematicBounds);
        plugin.getLogger().info(
                "AirDrop schematic spawn: type=" + typeId
                        + " schematic=" + schematicId
                        + " paste=" + pasteOrigin.getBlockX() + ", "
                        + pasteOrigin.getBlockY() + ", "
                        + pasteOrigin.getBlockZ()
                        + " chest=" + blockAnchor.getBlockX() + ", "
                        + blockAnchor.getBlockY() + ", "
                        + blockAnchor.getBlockZ()
        );
        api.schematics().paste(schematicId, pasteOrigin, options, spawnOverrides).thenAccept(result ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!result.success()) {
                        String reason = result.errorKey().orElse("unknown");
                        plugin.getLogger().warning(
                                "AirDrop schematic paste failed type=" + typeId
                                        + " session=" + sessionId
                                        + " reason=" + reason
                                        + " at "
                                        + pasteOrigin.getBlockX() + ", "
                                        + pasteOrigin.getBlockY() + ", "
                                        + pasteOrigin.getBlockZ()
                                        + " (details in SoulEvents log)"
                        );
                        endSession(sessionId, "SCHEMATIC_FAILED");
                        return;
                    }
                    Location chestAnchor = blockAnchor(result.chestAnchor());
                    List<Location> chestAnchors = result.chestAnchors().stream()
                            .map(AirDropService::blockAnchor)
                            .toList();
                    finalizeSessionStart(
                            typeId,
                            definition,
                            chestAnchors,
                            source,
                            sessionId,
                            lootableAt,
                            schematicBounds
                    );
                })
        );
    }

    private void finalizeSessionStart(
            String typeId,
            AirDropTypeDefinition definition,
            List<Location> lootAnchors,
            String source,
            UUID sessionId,
            Instant lootableAt,
            Optional<SchematicWorldBounds> schematicBounds
    ) {
        if (lootAnchors.isEmpty()) {
            plugin.getLogger().warning("AirDrop finalize skipped: no loot anchors session=" + sessionId);
            endSession(sessionId, "SCHEMATIC_FAILED");
            return;
        }
        AirDropTypeSettings type = definition.settings();
        Location blockAnchor = lootAnchors.getFirst();
        Material chestMaterial = resolveChestMaterial(definition, type);
        if (lootAnchors.size() > 1 && type.chestCluster.enabled) {
            plugin.getLogger().info(
                    "AirDrop type=" + typeId + ": chestCluster disabled for multi-marker spawn ("
                            + lootAnchors.size() + " points)"
            );
        }
        List<AirDropSessionRegistry.LootPoint> lootPoints = installLootPoints(lootAnchors, chestMaterial, type);
        sessionRegistry.register(sessionId, lootPoints, chestMaterial);
        assignLootChests(sessionId, definition);
        arenaRegionService.create(sessionId, blockAnchor, type.arenaWorldGuard, schematicBounds);

        if (visualService != null) {
            visualService.playSpawn(definition, lootAnchors, sessionId, lootableAt, chestMaterial);
        }

        api.sessions().setPhase(sessionId, EventPhase.ACTIVE);
        if (type.preOpenBeacon.enabled || type.preOpenMobs.enabled) {
            api.sessions().setPhase(sessionId, EventPhase.PRE_OPEN);
            if (preOpenService != null) {
                preOpenService.start(sessionId, definition, blockAnchor, lootableAt);
            }
        }
        if (type.broadcast.enabled && type.broadcast.spawnEnabled) {
            messages.broadcast(type.broadcast.messageKey, placeholders(typeId, definition, blockAnchor, null));
        }
        scheduleUnlockBroadcast(sessionId, typeId, definition, blockAnchor, lootableAt);
        int maxActiveSeconds = Math.max(1, type.lifecycle.maxActiveSecondsAfterLootable);
        Instant despawnAt = lootableAt.plusSeconds(maxActiveSeconds);
        scheduleSessionEnd(sessionId, despawnAt, "EXPIRED");
    }

    private void finalizeSessionStart(
            String typeId,
            AirDropTypeDefinition definition,
            Location blockAnchor,
            String source,
            UUID sessionId,
            Instant lootableAt,
            Optional<SchematicWorldBounds> schematicBounds
    ) {
        finalizeSessionStart(
                typeId,
                definition,
                List.of(blockAnchor),
                source,
                sessionId,
                lootableAt,
                schematicBounds
        );
    }

    private void startSession(String typeId, AirDropTypeDefinition definition, Location anchor, String source) {
        AirDropTypeSettings type = definition.settings();
        Location blockAnchor = blockAnchor(anchor);
        ActiveEvent session = api.sessions().start(AirDropModule.MODULE_ID, typeId, blockAnchor);
        UUID sessionId = session.sessionId();
        Instant lootableAt = computeLootableAt(type);
        api.sessions().setLootableAt(sessionId, lootableAt);
        sessionRepository.insertSession(sessionId, typeId, blockAnchor, source);
        finalizeSessionStart(typeId, definition, blockAnchor, source, sessionId, lootableAt, Optional.empty());
    }

    private boolean canSpawn(String typeId, AirDropTypeSettings type, boolean bypassConcurrentLimit) {
        List<ActiveEvent> active = api.modules().activeEvents(AirDropModule.MODULE_ID).stream().toList();
        if (active.size() >= config.module().maxConcurrentTotal) {
            return false;
        }
        if (bypassConcurrentLimit) {
            return true;
        }
        long typeActive = active.stream().filter(event -> event.typeId().equals(typeId)).count();
        return typeActive < type.maxConcurrent;
    }

    private static Location blockAnchor(Location anchor) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX(),
                anchor.getBlockY(),
                anchor.getBlockZ()
        );
    }

    private Instant computeLootableAt(AirDropTypeSettings type) {
        int delay = Math.max(0, type.lifecycle.minOpenDelaySeconds);
        if (type.preOpenBeacon.enabled) {
            delay = Math.max(delay, type.preOpenBeacon.delaySeconds);
        }
        if (delay <= 0) {
            return Instant.now();
        }
        return Instant.now().plusSeconds(delay);
    }

    private Material resolveChestMaterial(AirDropTypeDefinition definition, AirDropTypeSettings type) {
        Material fromLoot = Material.matchMaterial(definition.loot().chestMaterial);
        if (fromLoot == null || !fromLoot.isBlock()) {
            fromLoot = Material.ENDER_CHEST;
        }
        if (!type.chestCluster.enabled) {
            return fromLoot;
        }
        String override = type.chestCluster.chestMaterial;
        if (override == null || override.isBlank()) {
            return fromLoot;
        }
        Material clusterMaterial = Material.matchMaterial(override);
        if (clusterMaterial != null && clusterMaterial.isBlock()) {
            return clusterMaterial;
        }
        return fromLoot;
    }

    private List<AirDropSessionRegistry.LootPoint> installLootPoints(
            List<Location> anchors,
            Material chestMaterial,
            AirDropTypeSettings type
    ) {
        boolean clusterEnabled = type.chestCluster.enabled && anchors.size() == 1;
        List<AirDropSessionRegistry.LootPoint> points = new ArrayList<>(anchors.size());
        for (Location anchor : anchors) {
            Location resolved = blockAnchor(anchor);
            Block block = resolved.getBlock();
            Material originalMaterial = block.getType();
            BlockData originalBlockData = block.getBlockData().clone();
            if (clusterEnabled) {
                AirDropClusterChestPlacer.ClusterPlacement placement = AirDropClusterChestPlacer.install(
                        resolved,
                        chestMaterial,
                        type.chestCluster.isDecoyMode()
                );
                points.add(new AirDropSessionRegistry.LootPoint(
                        resolved,
                        originalMaterial,
                        originalBlockData,
                        true,
                        placement.replacedBlocks(),
                        placement.lootSlotIndex()
                ));
            } else {
                block.setType(chestMaterial);
                points.add(new AirDropSessionRegistry.LootPoint(
                        resolved,
                        originalMaterial,
                        originalBlockData,
                        false,
                        List.of(),
                        -1
                ));
            }
        }
        return points;
    }

    private void reinstallChestBlocks(AirDropSessionRegistry.SessionRecord record) {
        for (AirDropSessionRegistry.LootPoint point : record.lootPoints()) {
            if (point.clusterEnabled()) {
                AirDropClusterChestPlacer.install(
                        point.anchor(),
                        record.chestMaterial(),
                        point.isDecoyCluster(),
                        point.clusterLootSlotIndex()
                );
            } else {
                point.anchor().getBlock().setType(record.chestMaterial());
            }
        }
    }

    private void assignLootChests(UUID sessionId, AirDropTypeDefinition definition) {
        Optional<AirDropSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty() || recordOptional.get().hasLootChests()) {
            return;
        }
        AirDropSessionRegistry.SessionRecord record = recordOptional.get();
        LootTableSettings loot = definition.loot();
        int chestSize = normalizeChestSize(loot.chestSize);
        List<AirDropChestHolder> holders = new ArrayList<>(
                AirDropSessionRegistry.totalChestSlots(record.lootPoints())
        );
        for (int pointIndex = 0; pointIndex < record.lootPoints().size(); pointIndex++) {
            AirDropSessionRegistry.LootPoint point = record.lootPoints().get(pointIndex);
            int chestCount = point.clusterEnabled() ? AirDropClusterChestPlacer.CLUSTER_CHEST_COUNT : 1;
            for (int clusterIndex = 0; clusterIndex < chestCount; clusterIndex++) {
                int flatIndex = AirDropSessionRegistry.flatChestIndex(pointIndex, clusterIndex);
                if (point.isDecoyCluster() && clusterIndex != point.clusterLootSlotIndex()) {
                    holders.add(createLootChest(sessionId, definition, loot, chestSize, flatIndex, List.of()));
                    continue;
                }
                holders.add(createLootChest(
                        sessionId,
                        definition,
                        loot,
                        chestSize,
                        flatIndex,
                        lootRollService.roll(loot, chestSize)
                ));
            }
        }
        sessionRegistry.assignLootChests(sessionId, holders);
    }

    private AirDropChestHolder createLootChest(
            UUID sessionId,
            AirDropTypeDefinition definition,
            LootTableSettings loot,
            int chestSize,
            int flatIndex,
            List<ItemStack> rolled
    ) {
        AirDropChestHolder holder = new AirDropChestHolder(
                sessionId,
                messages.resolve("airdrop.chest.title", Map.of(
                        "type_name", PlainTextComponentSerializer.plainText().serialize(
                                messages.resolve(definition.settings().displayNameKey, Map.of())
                        ),
                        "chest", Integer.toString(flatIndex + 1)
                )),
                chestSize
        );
        ItemStack[] contents = holder.getInventory().getContents();
        List<ItemStack> masks = SerializedItemStackCodec.decodeAll(loot.obfuscationItemsBase64);
        List<Integer> targetSlots = lootRollService.randomChestSlots(chestSize, rolled.size());
        int slotOffset = flatIndex * LOOT_SLOT_STRIDE;
        for (int index = 0; index < rolled.size() && index < targetSlots.size(); index++) {
            int chestSlot = targetSlots.get(index);
            if (chestSlot < 0 || chestSlot >= contents.length) {
                continue;
            }
            ItemStack mask = pickObfuscationMask(masks);
            contents[chestSlot] = api.protection().loot().obfuscate(
                    rolled.get(index),
                    sessionId,
                    slotOffset + chestSlot,
                    mask
            );
        }
        holder.getInventory().setContents(contents);
        return holder;
    }

    private static ItemStack pickObfuscationMask(List<ItemStack> masks) {
        if (masks.isEmpty()) {
            return null;
        }
        return masks.get(ThreadLocalRandom.current().nextInt(masks.size()));
    }

    private void ensureLootChests(
            UUID sessionId,
            AirDropTypeDefinition definition,
            AirDropSessionRegistry.SessionRecord record
    ) {
        if (!record.isChestIntact()) {
            reinstallChestBlocks(record);
        }
        if (!record.hasLootChests()) {
            assignLootChests(sessionId, definition);
        }
    }

    private static int normalizeChestSize(int chestSize) {
        int normalized = Math.max(9, Math.min(54, chestSize));
        return normalized - (normalized % 9);
    }

    private Map<String, String> placeholders(
            String typeId,
            AirDropTypeDefinition definition,
            Location anchor,
            Player player
    ) {
        Map<String, String> values = new HashMap<>();
        values.put("type", typeId);
        values.put("type_name", PlainTextComponentSerializer.plainText().serialize(
                messages.resolve(definition.settings().displayNameKey, Map.of())
        ));
        values.put("world", anchor.getWorld().getName());
        values.put("x", Integer.toString(anchor.getBlockX()));
        values.put("y", Integer.toString(anchor.getBlockY()));
        values.put("z", Integer.toString(anchor.getBlockZ()));
        values.put("cost", formatCost(definition.settings()));
        values.put("seconds", Integer.toString(Math.max(1, definition.settings().lifecycle.cleanupSecondsAfterLooted)));
        values.put("player", player == null ? "" : player.getName());
        return values;
    }

    private void scheduleUnlockBroadcast(
            UUID sessionId,
            String typeId,
            AirDropTypeDefinition definition,
            Location anchor,
            Instant lootableAt
    ) {
        BroadcastSettings broadcast = definition.settings().broadcast;
        if (!broadcast.enabled || !broadcast.unlockedEnabled) {
            return;
        }
        long delaySeconds = Math.max(0L, java.time.Duration.between(Instant.now(), lootableAt).toSeconds());
        if (delaySeconds <= 0L) {
            if (preOpenService != null) {
                preOpenService.stop(sessionId);
            }
            broadcastLifecycle(sessionId, typeId, definition, anchor, broadcast, broadcast.unlockedMessageKey, true, null);
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (sessionRegistry.find(sessionId).isEmpty()) {
                        return;
                    }
                    if (preOpenService != null) {
                        preOpenService.stop(sessionId);
                    }
                    broadcastLifecycle(
                            sessionId,
                            typeId,
                            definition,
                            anchor,
                            broadcast,
                            broadcast.unlockedMessageKey,
                            true,
                            null
                    );
                },
                delaySeconds * 20L
        );
        sessionRegistry.assignUnlockTask(sessionId, task);
    }

    private void broadcastLifecycle(
            UUID sessionId,
            String typeId,
            AirDropTypeDefinition definition,
            Location anchor,
            BroadcastSettings broadcast,
            String messageKey,
            boolean stageEnabled,
            Player player
    ) {
        if (!broadcast.enabled || !stageEnabled) {
            return;
        }
        messages.broadcast(messageKey, placeholders(typeId, definition, anchor, player));
    }

    private static boolean isInventoryEmpty(org.bukkit.inventory.Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    private String formatCost(AirDropTypeSettings type) {
        if (type.summon.vaultCost <= 0.0) {
            return vaultEconomy != null ? vaultEconomy.format(0.0) : "0";
        }
        if (vaultEconomy != null && vaultEconomy.available()) {
            return vaultEconomy.format(type.summon.vaultCost);
        }
        return Double.toString(type.summon.vaultCost);
    }

    private String resolveTypeName(AirDropTypeDefinition definition) {
        return PlainTextComponentSerializer.plainText().serialize(
                messages.resolve(definition.settings().displayNameKey, Map.of())
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
            return "airdrop.placement.invalid-location";
        }
        return "airdrop.placement." + result.denial().name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private WorldPlacementGate gate(String typeId) {
        WorldPlacementGate gate = gates.get(typeId);
        if (gate != null) {
            return gate;
        }
        return new WorldPlacementGate(new WorldPlacementSettings(), worldGuardProbe);
    }

    private void rebuildGates() {
        Map<String, WorldPlacementGate> rebuilt = new HashMap<>();
        for (AirDropTypeDefinition definition : config.types()) {
            rebuilt.put(definition.id(), new WorldPlacementGate(definition.settings().worldPlacement, worldGuardProbe));
        }
        gates = Map.copyOf(rebuilt);
    }

    private void endSession(UUID sessionId, String phase) {
        if (!"SHUTDOWN".equals(phase)) {
            Optional<ActiveEvent> active = activeEvent(sessionId);
            Optional<AirDropSessionRegistry.SessionRecord> record = sessionRegistry.find(sessionId);
            if (active.isPresent() && record.isPresent()) {
                config.type(active.get().typeId()).ifPresent(definition -> broadcastLifecycle(
                        sessionId,
                        active.get().typeId(),
                        definition,
                        record.get().anchor(),
                        definition.settings().broadcast,
                        definition.settings().broadcast.removedMessageKey,
                        definition.settings().broadcast.removedEnabled,
                        null
                ));
            }
        }
        restoreAnchorBlock(sessionId);
        if (preOpenService != null) {
            preOpenService.stop(sessionId);
        }
        if (visualService != null) {
            visualService.remove(sessionId);
        }
        arenaRegionService.remove(sessionId);
        sessionRegistry.remove(sessionId);
        api.protection().loot().clearSession(sessionId);
        api.schematics().undo(sessionId);
        sessionRepository.endSession(sessionId, phase);
        api.sessions().end(sessionId);
    }

    private void restoreAnchorBlock(UUID sessionId) {
        sessionRegistry.find(sessionId).ifPresent(record -> {
            for (AirDropSessionRegistry.LootPoint point : record.lootPoints()) {
                AirDropClusterChestPlacer.restore(point.clusterBlocks());
                Block block = point.anchor().getBlock();
                block.setType(point.originalMaterial());
                if (point.originalBlockData() != null) {
                    block.setBlockData(point.originalBlockData());
                }
            }
        });
    }

    private static boolean hasBypass(CommandSender sender) {
        return sender.hasPermission(AirDropPermissions.BYPASS);
    }
}
