package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import bm.b0b0b0.soulevents.api.module.EventPhase;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOptions;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.chest.AirDropChestHolder;
import bm.b0b0b0.soulevents.airdrop.config.settings.BroadcastSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.RequiredLootSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.WorldPlacementSettings;
import bm.b0b0b0.soulevents.api.protection.GateContext;
import bm.b0b0b0.soulevents.api.protection.GateResult;
import bm.b0b0b0.soulevents.api.protection.LootGuardService;
import bm.b0b0b0.soulevents.api.protection.ObfuscatedLootRef;
import bm.b0b0b0.soulevents.airdrop.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.airdrop.integration.ArenaRegionService;
import bm.b0b0b0.soulevents.airdrop.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
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
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        this.spawnWorldResolver = new SpawnWorldResolver(api.placement());
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
        ensureLootChest(sessionId, definition.get(), record);
    }

    public void reload(AirDropPluginConfig config) {
        this.config = config;
        rebuildGates();
    }

    public AirDropPluginConfig config() {
        return config;
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
        int clusterIndex = 0;
        if (record.clusterEnabled()) {
            Optional<Integer> slotIndex = AirDropClusterChestPlacer.slotIndexAt(record.anchor(), clickedBlock);
            if (slotIndex.isEmpty()) {
                messages.send(player, "airdrop.chest-open-failed", Map.of());
                return;
            }
            clusterIndex = slotIndex.get();
            if (record.isDecoyCluster() && clusterIndex != record.clusterLootSlotIndex()) {
                messages.send(player, "airdrop.chest-decoy", Map.of());
                return;
            }
        }
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
        if (type.requiredLoot.enabled && !hasRequiredLoot(player, type.requiredLoot)) {
            messages.send(player, "airdrop.required-item-missing", Map.of());
            return;
        }
        final int openedClusterIndex = clusterIndex;
        AirDropChestHolder holder = record.lootChest(openedClusterIndex).orElse(null);
        if (holder == null) {
            ensureLootChest(sessionId, definition, record);
            holder = sessionRegistry.find(sessionId)
                    .flatMap(current -> current.lootChest(openedClusterIndex))
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

    private static boolean hasRequiredLoot(Player player, RequiredLootSettings requiredLoot) {
        if (requiredLoot.requiredItemsBase64.isEmpty()) {
            return false;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        for (String encoded : requiredLoot.requiredItemsBase64) {
            ItemStack required = decodeItem(encoded);
            if (required != null && held.isSimilar(required)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack decodeItem(String encoded) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException exception) {
            return null;
        }
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
        for (int index = 0; index < record.lootChests().size(); index++) {
            if (record.isDecoyCluster() && index != record.clusterLootSlotIndex()) {
                continue;
            }
            AirDropChestHolder holder = record.lootChest(index).orElse(null);
            if (holder == null) {
                continue;
            }
            if (!isInventoryEmpty(holder.getInventory())) {
                return false;
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
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> endSession(sessionId, "LOOTED"),
                cleanupSeconds * 20L
        );
        sessionRegistry.rescheduleCleanup(sessionId, task, cleanupAt);
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
        if (!type.summon.adminSummonEnabled && !sender.hasPermission("soulevents.airdrop.admin")) {
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
        if (!typeOptional.get().settings().summon.playerSummonEnabled) {
            messages.send(player, "airdrop.summon-disabled", Map.of());
            return;
        }
        if (!player.hasPermission("soulevents.airdrop.summon")
                && !player.hasPermission("soulevents.airdrop.summon." + typeId)) {
            messages.send(player, "command.no-permission", Map.of());
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
        if (spawnWorld == null) {
            if (feedback != null) {
                messages.send(feedback, "airdrop.world-not-found", Map.of("world", spawnWorldName));
            }
            completion.accept(false);
            return;
        }
        WorldPlacementResult worldCheck = gate.checkWorld(spawnWorld);
        if (!worldCheck.allowed()) {
            if (feedback != null) {
                sendPlacementError(feedback, worldCheck);
            }
            completion.accept(false);
            return;
        }
        if (feedback != null) {
            messages.send(feedback, "airdrop.searching", Map.of("world", spawnWorldName));
        }
        spawnWorldResolver.resolveAsync(plugin, definition.settings(), gate, location -> {
            if (location.isEmpty()) {
                if (feedback != null) {
                    messages.send(feedback, "airdrop.spawn-failed", Map.of("type", typeId, "world", spawnWorldName));
                }
                completion.accept(false);
                return;
            }
            if (!tryStart(typeId, definition, location.get(), source, bypassLimits)) {
                if (feedback != null) {
                    messages.send(feedback, "airdrop.limit-reached", Map.of("type", typeId));
                }
                completion.accept(false);
                return;
            }
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
        startSession(typeId, definition, anchor, source);
        return true;
    }

    private boolean canSpawn(String typeId, AirDropTypeSettings type, boolean bypassLimits) {
        if (bypassLimits) {
            return true;
        }
        List<ActiveEvent> active = api.modules().activeEvents(AirDropModule.MODULE_ID).stream().toList();
        if (active.size() >= config.module().maxConcurrentTotal) {
            return false;
        }
        long typeActive = active.stream().filter(event -> event.typeId().equals(typeId)).count();
        return typeActive < type.maxConcurrent;
    }

    private void startSession(String typeId, AirDropTypeDefinition definition, Location anchor, String source) {
        AirDropTypeSettings type = definition.settings();
        Location blockAnchor = blockAnchor(anchor);
        Block anchorBlock = blockAnchor.getBlock();
        Material originalMaterial = anchorBlock.getType();
        BlockData originalBlockData = anchorBlock.getBlockData().clone();
        Material chestMaterial = resolveChestMaterial(definition, type);

        ActiveEvent session = api.sessions().start(AirDropModule.MODULE_ID, typeId, blockAnchor);
        UUID sessionId = session.sessionId();
        Instant lootableAt = computeLootableAt(type);
        api.sessions().setLootableAt(sessionId, lootableAt);
        sessionRepository.insertSession(sessionId, typeId, blockAnchor, source);
        ClusterSetup cluster = installChestBlocks(blockAnchor, chestMaterial, type);
        sessionRegistry.register(
                sessionId,
                blockAnchor,
                chestMaterial,
                originalMaterial,
                originalBlockData,
                cluster.clusterEnabled(),
                cluster.clusterBlocks(),
                cluster.clusterLootSlotIndex()
        );
        assignLootChests(sessionId, definition);
        arenaRegionService.create(sessionId, blockAnchor, type.arenaRadius, type.arenaWorldGuard);

        if (visualService != null) {
            visualService.playSpawn(definition, blockAnchor, sessionId, lootableAt, chestMaterial);
        }

        api.sessions().setPhase(sessionId, EventPhase.ACTIVE);
        if (type.preOpenBeacon.enabled || type.preOpenMobs.enabled) {
            api.sessions().setPhase(sessionId, EventPhase.PRE_OPEN);
        }
        if (type.broadcast.enabled && type.broadcast.spawnEnabled) {
            messages.broadcast(type.broadcast.messageKey, placeholders(typeId, definition, blockAnchor, null));
        }
        scheduleUnlockBroadcast(sessionId, typeId, definition, blockAnchor, lootableAt);

        if (!type.schematicId.isEmpty()) {
            SchematicPasteOptions options = new SchematicPasteOptions(
                    sessionId,
                    type.landscapeBlend,
                    type.blendRadius,
                    false
            );
            api.schematics().paste(type.schematicId, blockAnchor, options);
        }
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

    private ClusterSetup installChestBlocks(Location anchor, Material chestMaterial, AirDropTypeSettings type) {
        if (type.chestCluster.enabled) {
            AirDropClusterChestPlacer.ClusterPlacement placement = AirDropClusterChestPlacer.install(
                    anchor,
                    chestMaterial,
                    type.chestCluster.isDecoyMode()
            );
            return new ClusterSetup(true, placement.replacedBlocks(), placement.lootSlotIndex());
        }
        anchor.getBlock().setType(chestMaterial);
        return new ClusterSetup(false, List.of(), -1);
    }

    private void reinstallChestBlocks(AirDropSessionRegistry.SessionRecord record) {
        if (record.clusterEnabled()) {
            AirDropClusterChestPlacer.install(
                    record.anchor(),
                    record.chestMaterial(),
                    record.isDecoyCluster(),
                    record.clusterLootSlotIndex()
            );
            return;
        }
        record.anchor().getBlock().setType(record.chestMaterial());
    }

    private void assignLootChests(UUID sessionId, AirDropTypeDefinition definition) {
        Optional<AirDropSessionRegistry.SessionRecord> recordOptional = sessionRegistry.find(sessionId);
        if (recordOptional.isEmpty() || recordOptional.get().hasLootChests()) {
            return;
        }
        AirDropSessionRegistry.SessionRecord record = recordOptional.get();
        LootTableSettings loot = definition.loot();
        int chestSize = normalizeChestSize(loot.chestSize);
        int chestCount = record.clusterEnabled() ? AirDropClusterChestPlacer.CLUSTER_CHEST_COUNT : 1;
        List<AirDropChestHolder> holders = new ArrayList<>(chestCount);
        for (int clusterIndex = 0; clusterIndex < chestCount; clusterIndex++) {
            if (record.isDecoyCluster() && clusterIndex != record.clusterLootSlotIndex()) {
                holders.add(createLootChest(sessionId, definition, chestSize, clusterIndex, List.of()));
                continue;
            }
            holders.add(createLootChest(sessionId, definition, chestSize, clusterIndex, lootRollService.roll(loot)));
        }
        sessionRegistry.assignLootChests(sessionId, holders);
    }

    private AirDropChestHolder createLootChest(
            UUID sessionId,
            AirDropTypeDefinition definition,
            int chestSize,
            int clusterIndex,
            List<ItemStack> rolled
    ) {
        AirDropChestHolder holder = new AirDropChestHolder(
                sessionId,
                messages.resolve("airdrop.chest.title", Map.of(
                        "type_name", PlainTextComponentSerializer.plainText().serialize(
                                messages.resolve(definition.settings().displayNameKey, Map.of())
                        ),
                        "chest", Integer.toString(clusterIndex + 1)
                )),
                chestSize
        );
        ItemStack[] contents = holder.getInventory().getContents();
        int slot = 0;
        int slotOffset = clusterIndex * LOOT_SLOT_STRIDE;
        for (ItemStack item : rolled) {
            if (slot >= contents.length) {
                break;
            }
            contents[slot] = api.protection().loot().obfuscate(item, sessionId, slotOffset + slot);
            slot++;
        }
        holder.getInventory().setContents(contents);
        return holder;
    }

    private void ensureLootChest(
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

    private record ClusterSetup(
            boolean clusterEnabled,
            List<AirDropClusterChestPlacer.ReplacedBlock> clusterBlocks,
            int clusterLootSlotIndex
    ) {
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
            broadcastLifecycle(sessionId, typeId, definition, anchor, broadcast, broadcast.unlockedMessageKey, true, null);
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (sessionRegistry.find(sessionId).isEmpty()) {
                        return;
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
            return "0";
        }
        return Double.toString(type.summon.vaultCost);
    }

    private void sendPlacementError(CommandSender sender, WorldPlacementResult result) {
        messages.send(sender, result.messageKey().orElse("airdrop.placement.invalid-location"), Map.of(
                "world", result.worldName(),
                "region", result.regionName(),
                "distance", result.regionName()
        ));
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
            AirDropClusterChestPlacer.restore(record.clusterBlocks());
            Block block = record.anchor().getBlock();
            block.setType(record.originalMaterial());
            if (record.originalBlockData() != null) {
                block.setBlockData(record.originalBlockData());
            }
        });
    }

    private static boolean hasBypass(CommandSender sender) {
        return sender.hasPermission("soulevents.airdrop.bypass");
    }
}
