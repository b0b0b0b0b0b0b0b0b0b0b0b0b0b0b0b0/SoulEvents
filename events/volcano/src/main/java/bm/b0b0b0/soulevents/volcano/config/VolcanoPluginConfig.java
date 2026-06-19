package bm.b0b0b0.soulevents.volcano.config;

import bm.b0b0b0.soulevents.volcano.config.settings.GuiGeneralSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoModuleSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoTypeSettings;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class VolcanoPluginConfig {

    private final VolcanoModuleSettings module;
    private final GuiGeneralSettings gui;
    private final Map<String, VolcanoTypeDefinition> types;

    public VolcanoPluginConfig(
            VolcanoModuleSettings module,
            GuiGeneralSettings gui,
            Map<String, VolcanoTypeDefinition> types
    ) {
        this.module = module;
        this.gui = gui;
        this.types = types;
    }

    public VolcanoModuleSettings module() {
        return module;
    }

    public GuiGeneralSettings gui() {
        return gui;
    }

    public Map<String, VolcanoTypeDefinition> typesById() {
        return types;
    }

    public Collection<VolcanoTypeDefinition> types() {
        return types.values();
    }

    public Optional<VolcanoTypeDefinition> type(String typeId) {
        return Optional.ofNullable(types.get(typeId));
    }

    public VolcanoTypeSettings typeSettings(String typeId) {
        return type(typeId).map(VolcanoTypeDefinition::settings).orElse(null);
    }

    public LootTableSettings loot(String typeId) {
        return type(typeId).map(VolcanoTypeDefinition::loot).orElse(null);
    }
}
