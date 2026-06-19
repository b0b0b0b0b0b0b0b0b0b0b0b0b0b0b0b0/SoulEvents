package bm.b0b0b0.soulevents.volcano.config;

import bm.b0b0b0.soulevents.volcano.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoTypeSettings;

public record VolcanoTypeDefinition(
        String id,
        VolcanoTypeSettings settings,
        LootTableSettings loot
) {

    public String lootTableId() {
        if (settings.lootTableId == null || settings.lootTableId.isEmpty()) {
            return id;
        }
        return settings.lootTableId;
    }
}
