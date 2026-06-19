package bm.b0b0b0.soulevents.api.inventory;

import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public interface ProtectedLootInventoryHolder extends InventoryHolder {

    UUID lootSessionId();
}
