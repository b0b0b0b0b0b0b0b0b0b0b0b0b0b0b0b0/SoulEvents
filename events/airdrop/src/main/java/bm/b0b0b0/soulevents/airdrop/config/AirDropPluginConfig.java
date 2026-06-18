package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropModuleSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.GuiGeneralSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class AirDropPluginConfig {

    private final AirDropModuleSettings module;
    private final GuiGeneralSettings gui;
    private final Map<String, AirDropTypeDefinition> types;

    public AirDropPluginConfig(
            AirDropModuleSettings module,
            GuiGeneralSettings gui,
            Map<String, AirDropTypeDefinition> types
    ) {
        this.module = module;
        this.gui = gui;
        this.types = types;
    }

    public AirDropModuleSettings module() {
        return module;
    }

    public GuiGeneralSettings gui() {
        return gui;
    }

    public Map<String, AirDropTypeDefinition> typesById() {
        return types;
    }

    public Collection<AirDropTypeDefinition> types() {
        return types.values();
    }

    public Optional<AirDropTypeDefinition> type(String typeId) {
        return Optional.ofNullable(types.get(typeId));
    }

    public AirDropTypeSettings typeSettings(String typeId) {
        return type(typeId).map(AirDropTypeDefinition::settings).orElse(null);
    }

    public LootTableSettings loot(String typeId) {
        return type(typeId).map(AirDropTypeDefinition::loot).orElse(null);
    }
}
