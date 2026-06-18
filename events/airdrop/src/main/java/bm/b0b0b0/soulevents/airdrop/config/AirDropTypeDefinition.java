package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;

public record AirDropTypeDefinition(
        String id,
        AirDropTypeSettings settings,
        LootTableSettings loot
) {

    public String lootTableId() {
        if (settings.lootTableId == null || settings.lootTableId.isEmpty()) {
            return id;
        }
        return settings.lootTableId;
    }
}
