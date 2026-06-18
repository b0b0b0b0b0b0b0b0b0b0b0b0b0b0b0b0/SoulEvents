package bm.b0b0b0.soulevents.api.protection;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public interface LootGuardService {

    boolean canTake(Player player, UUID sessionId, int slotIndex);

    void registerTake(Player player, UUID sessionId, int slotIndex);

    ItemStack obfuscate(ItemStack real, UUID sessionId, int slotIndex);

    Optional<ObfuscatedLootRef> obfuscatedRef(ItemStack item);

    boolean isSlotClaimed(UUID sessionId, int slotIndex);

    boolean tryTakeObfuscated(Player player, ItemStack obfuscated, UUID sessionId, int slotIndex);

    void resumePendingReveal(Player player, ObfuscatedLootRef ref);

    void debug(String message);

    void debugWarn(String message);

    void clearSession(UUID sessionId);

    void reload();
}
