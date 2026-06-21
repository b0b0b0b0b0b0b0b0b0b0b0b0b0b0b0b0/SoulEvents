package bm.b0b0b0.soulevents.core.protection;

import bm.b0b0b0.soulevents.api.protection.LootGuardService;
import bm.b0b0b0.soulevents.api.protection.ObfuscatedLootRef;
import bm.b0b0b0.soulevents.api.stats.EventStatsMetrics;
import bm.b0b0b0.soulevents.api.stats.PlayerEventStatsService;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LootGuardServiceImpl implements LootGuardService {

    private static final String LOG_PREFIX = "[LootGuard] ";

    private final Plugin plugin;
    private final PlayerEventStatsService stats;
    private final NamespacedKey slotKey;
    private final NamespacedKey sessionKey;
    private long takeCooldownMillis;
    private long revealDelayMillis;
    private boolean lootDebugEnabled;
    private final Map<TakeKey, Long> lastTakeAt = new ConcurrentHashMap<>();
    private final Map<RevealKey, ItemStack> pendingReveal = new ConcurrentHashMap<>();
    private final Set<RevealKey> claimedSlots = ConcurrentHashMap.newKeySet();
    private final Map<PlayerRevealKey, ItemStack> awaitingReveal = new ConcurrentHashMap<>();
    private final Map<PlayerRevealKey, BukkitTask> revealTasks = new ConcurrentHashMap<>();

    public LootGuardServiceImpl(Plugin plugin, PluginConfig config, PlayerEventStatsService stats) {
        this.plugin = plugin;
        this.stats = stats;
        this.slotKey = new NamespacedKey(plugin, "loot-slot");
        this.sessionKey = new NamespacedKey(plugin, "loot-session");
        apply(config);
    }

    @Override
    public boolean canTake(Player player, UUID sessionId, int slotIndex) {
        RevealKey slotKeyValue = new RevealKey(sessionId, slotIndex);
        if (claimedSlots.contains(slotKeyValue)) {
            return false;
        }
        TakeKey key = new TakeKey(player.getUniqueId(), sessionId, slotIndex);
        Long last = lastTakeAt.get(key);
        if (last == null) {
            return true;
        }
        boolean allowed = System.currentTimeMillis() - last >= takeCooldownMillis;
        if (!allowed) {
            debug("cooldown block player=" + player.getName()
                    + " session=" + sessionId
                    + " slot=" + slotIndex
                    + " waitMs=" + (takeCooldownMillis - (System.currentTimeMillis() - last)));
        }
        return allowed;
    }

    @Override
    public void registerTake(Player player, UUID sessionId, int slotIndex) {
        lastTakeAt.put(new TakeKey(player.getUniqueId(), sessionId, slotIndex), System.currentTimeMillis());
        debug("registerTake player=" + player.getName() + " session=" + sessionId + " slot=" + slotIndex);
    }

    @Override
    public ItemStack obfuscate(ItemStack real, UUID sessionId, int slotIndex) {
        return obfuscate(real, sessionId, slotIndex, null);
    }

    @Override
    public ItemStack obfuscate(ItemStack real, UUID sessionId, int slotIndex, ItemStack maskTemplate) {
        RevealKey key = new RevealKey(sessionId, slotIndex);
        if (claimedSlots.contains(key)) {
            debugWarn("obfuscate skipped claimed session=" + sessionId + " slot=" + slotIndex);
            return createMask(maskTemplate, sessionId, slotIndex);
        }
        ItemStack fake = createMask(maskTemplate, sessionId, slotIndex);
        pendingReveal.put(key, real.clone());
        debug("obfuscate session=" + sessionId
                + " slot=" + slotIndex
                + " real=" + describe(real)
                + " mask=" + describe(fake)
                + " pending=" + pendingReveal.size());
        return fake;
    }

    private ItemStack createMask(ItemStack maskTemplate, UUID sessionId, int slotIndex) {
        ItemStack fake;
        if (maskTemplate == null || maskTemplate.getType().isAir()) {
            fake = new ItemStack(Material.COAL, 1);
        } else {
            fake = maskTemplate.clone();
            fake.setAmount(1);
        }
        ItemMeta meta = fake.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(sessionKey, PersistentDataType.STRING, sessionId.toString());
            container.set(slotKey, PersistentDataType.INTEGER, slotIndex);
            fake.setItemMeta(meta);
        }
        return fake;
    }

    @Override
    public Optional<ObfuscatedLootRef> obfuscatedRef(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(sessionKey, PersistentDataType.STRING) || !container.has(slotKey, PersistentDataType.INTEGER)) {
            return Optional.empty();
        }
        String sessionValue = container.get(sessionKey, PersistentDataType.STRING);
        Integer slotValue = container.get(slotKey, PersistentDataType.INTEGER);
        if (sessionValue == null || slotValue == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ObfuscatedLootRef(UUID.fromString(sessionValue), slotValue));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isSlotClaimed(UUID sessionId, int slotIndex) {
        return claimedSlots.contains(new RevealKey(sessionId, slotIndex));
    }

    @Override
    public boolean tryTakeObfuscated(Player player, ItemStack obfuscated, UUID sessionId, int slotIndex) {
        RevealKey key = new RevealKey(sessionId, slotIndex);
        if (claimedSlots.contains(key)) {
            debugWarn("tryTakeObfuscated reject already claimed session=" + sessionId
                    + " slot=" + slotIndex
                    + " player=" + player.getName());
            return false;
        }
        ItemStack real = pendingReveal.remove(key);
        if (real == null) {
            debugWarn("tryTakeObfuscated reject no pending session=" + sessionId
                    + " slot=" + slotIndex
                    + " player=" + player.getName());
            return false;
        }
        if (!claimedSlots.add(key)) {
            debugWarn("tryTakeObfuscated burn race session=" + sessionId
                    + " slot=" + slotIndex
                    + " player=" + player.getName()
                    + " real=" + describe(real));
            return false;
        }
        PlayerRevealKey playerKey = new PlayerRevealKey(player.getUniqueId(), sessionId, slotIndex);
        if (awaitingReveal.putIfAbsent(playerKey, real) != null) {
            debugWarn("tryTakeObfuscated burn duplicate awaiting player=" + player.getName()
                    + " session=" + sessionId
                    + " slot=" + slotIndex
                    + " real=" + describe(real));
            return false;
        }
        debug("tryTakeObfuscated OK player=" + player.getName()
                + " session=" + sessionId
                + " slot=" + slotIndex
                + " real=" + describe(real));
        giveObfuscated(player, obfuscated);
        scheduleReveal(player, playerKey);
        return true;
    }

    @Override
    public void resumePendingReveal(Player player, ObfuscatedLootRef ref) {
        PlayerRevealKey playerKey = new PlayerRevealKey(player.getUniqueId(), ref.sessionId(), ref.slotIndex());
        if (!awaitingReveal.containsKey(playerKey)) {
            return;
        }
        if (revealTasks.containsKey(playerKey)) {
            return;
        }
        debug("resumePendingReveal reschedule player=" + player.getName()
                + " session=" + ref.sessionId()
                + " slot=" + ref.slotIndex());
        scheduleReveal(player, playerKey);
    }

    @Override
    public void clearSession(UUID sessionId) {
        int pendingBefore = countPending(sessionId);
        int awaitingBefore = countAwaiting(sessionId);
        int claimedBefore = countClaimed(sessionId);
        lastTakeAt.keySet().removeIf(takeKey -> takeKey.sessionId().equals(sessionId));
        pendingReveal.keySet().removeIf(revealKey -> revealKey.sessionId().equals(sessionId));
        claimedSlots.removeIf(revealKey -> revealKey.sessionId().equals(sessionId));
        Iterator<Map.Entry<PlayerRevealKey, ItemStack>> awaiting = awaitingReveal.entrySet().iterator();
        while (awaiting.hasNext()) {
            Map.Entry<PlayerRevealKey, ItemStack> entry = awaiting.next();
            if (entry.getKey().sessionId().equals(sessionId)) {
                cancelRevealTask(entry.getKey());
                awaiting.remove();
            }
        }
        if (pendingBefore > 0 || awaitingBefore > 0 || claimedBefore > 0) {
            debug("clearSession session=" + sessionId
                    + " droppedPending=" + pendingBefore
                    + " droppedAwaiting=" + awaitingBefore
                    + " droppedClaimed=" + claimedBefore);
        }
    }

    public void shutdownPending() {
        for (BukkitTask task : revealTasks.values()) {
            task.cancel();
        }
        revealTasks.clear();
        awaitingReveal.clear();
    }

    public void shutdown() {
        shutdownPending();
        pendingReveal.clear();
        claimedSlots.clear();
        lastTakeAt.clear();
    }

    @Override
    public void debug(String message) {
        if (!lootDebugEnabled) {
            return;
        }
        plugin.getLogger().info(LOG_PREFIX + message);
    }

    @Override
    public void debugWarn(String message) {
        if (!lootDebugEnabled) {
            return;
        }
        plugin.getLogger().warning(LOG_PREFIX + message);
    }

    @Override
    public void reload() {
    }

    public void reload(PluginConfig config) {
        shutdownPending();
        apply(config);
        lastTakeAt.clear();
        pendingReveal.clear();
        claimedSlots.clear();
    }

    private void scheduleReveal(Player player, PlayerRevealKey playerKey) {
        cancelRevealTask(playerKey);
        long delayTicks = Math.max(1L, (revealDelayMillis + 49L) / 50L);
        debug("scheduleReveal player=" + player.getName()
                + " session=" + playerKey.sessionId()
                + " slot=" + playerKey.slotIndex()
                + " delayTicks=" + delayTicks);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> completeReveal(player.getUniqueId(), playerKey),
                delayTicks
        );
        revealTasks.put(playerKey, task);
    }

    private void completeReveal(UUID playerId, PlayerRevealKey playerKey) {
        revealTasks.remove(playerKey);
        ItemStack real = awaitingReveal.remove(playerKey);
        if (real == null) {
            debugWarn("completeReveal ABORT awaiting missing playerId=" + playerId
                    + " session=" + playerKey.sessionId()
                    + " slot=" + playerKey.slotIndex());
            return;
        }
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            awaitingReveal.put(playerKey, real);
            debugWarn("completeReveal defer player offline id=" + playerId
                    + " session=" + playerKey.sessionId()
                    + " slot=" + playerKey.slotIndex());
            return;
        }
        if (replaceObfuscated(player, playerKey.sessionId(), playerKey.slotIndex(), real)) {
            stats.recordSession(playerId, playerKey.sessionId(), EventStatsMetrics.LOOT_TAKEN, 1L);
            debug("completeReveal OK player=" + player.getName()
                    + " session=" + playerKey.sessionId()
                    + " slot=" + playerKey.slotIndex()
                    + " -> " + describe(real));
            return;
        }
        debugWarn("completeReveal BURN loot (no matching mask) player=" + player.getName()
                + " session=" + playerKey.sessionId()
                + " slot=" + playerKey.slotIndex()
                + " real=" + describe(real));
    }

    private void giveObfuscated(Player player, ItemStack obfuscated) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(obfuscated.clone());
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            debug("giveObfuscated overflow drop player=" + player.getName() + " " + describe(leftover));
        }
    }

    private boolean replaceObfuscated(Player player, UUID sessionId, int slotIndex, ItemStack real) {
        PlayerInventory inventory = player.getInventory();
        for (int index = 0; index < inventory.getSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            Optional<ObfuscatedLootRef> ref = obfuscatedRef(stack);
            if (ref.isEmpty()) {
                continue;
            }
            if (!ref.get().sessionId().equals(sessionId) || ref.get().slotIndex() != slotIndex) {
                continue;
            }
            inventory.setItem(index, real.clone());
            player.updateInventory();
            return true;
        }
        return false;
    }

    private void cancelRevealTask(PlayerRevealKey playerKey) {
        BukkitTask task = revealTasks.remove(playerKey);
        if (task != null) {
            task.cancel();
        }
    }

    private int countPending(UUID sessionId) {
        int count = 0;
        for (RevealKey key : pendingReveal.keySet()) {
            if (key.sessionId().equals(sessionId)) {
                count++;
            }
        }
        return count;
    }

    private int countAwaiting(UUID sessionId) {
        int count = 0;
        for (PlayerRevealKey key : awaitingReveal.keySet()) {
            if (key.sessionId().equals(sessionId)) {
                count++;
            }
        }
        return count;
    }

    private int countClaimed(UUID sessionId) {
        int count = 0;
        for (RevealKey key : claimedSlots) {
            if (key.sessionId().equals(sessionId)) {
                count++;
            }
        }
        return count;
    }

    private static String describe(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return "AIR";
        }
        return stack.getType().name() + "x" + stack.getAmount();
    }

    private void apply(PluginConfig config) {
        this.takeCooldownMillis = config.protection().lootTakeCooldownMillis;
        this.revealDelayMillis = config.protection().lootRevealDelayMillis;
        this.lootDebugEnabled = config.protection().lootDebugEnabled;
    }

    private record TakeKey(UUID playerId, UUID sessionId, int slotIndex) {
    }

    private record RevealKey(UUID sessionId, int slotIndex) {
    }

    private record PlayerRevealKey(UUID playerId, UUID sessionId, int slotIndex) {
    }
}
