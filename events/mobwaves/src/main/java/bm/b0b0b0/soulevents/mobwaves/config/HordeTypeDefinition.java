package bm.b0b0b0.soulevents.mobwaves.config;

import bm.b0b0b0.soulevents.mobwaves.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeTypeSettings;

public record HordeTypeDefinition(
        String id,
        HordeTypeSettings settings,
        LootTableSettings loot
) {

    public String lootTableId() {
        if (settings.lootTableId == null || settings.lootTableId.isEmpty()) {
            return id;
        }
        return settings.lootTableId;
    }
}
