package bm.b0b0b0.soulevents.airdrop.chest;

import bm.b0b0b0.soulevents.api.inventory.ProtectedLootInventoryHolder;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class AirDropChestHolder implements InventoryHolder, ProtectedLootInventoryHolder {

    private final UUID sessionId;
    private final Inventory inventory;

    public AirDropChestHolder(UUID sessionId, Component title, int size) {
        this.sessionId = sessionId;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public UUID sessionId() {
        return sessionId;
    }

    @Override
    public UUID lootSessionId() {
        return sessionId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
