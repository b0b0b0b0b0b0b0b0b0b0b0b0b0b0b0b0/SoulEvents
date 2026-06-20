package bm.b0b0b0.soulevents.mobwaves.config;

import bm.b0b0b0.soulevents.mobwaves.config.settings.GuiGeneralSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobWavesModuleSettings;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class MobWavesPluginConfig {

    private final MobWavesModuleSettings module;
    private final GuiGeneralSettings gui;
    private final Map<String, WaveProfileDefinition> profilesById;
    private final Map<String, HordeTypeDefinition> typesById;

    public MobWavesPluginConfig(
            MobWavesModuleSettings module,
            GuiGeneralSettings gui,
            Map<String, WaveProfileDefinition> profilesById,
            Map<String, HordeTypeDefinition> typesById
    ) {
        this.module = module;
        this.gui = gui;
        this.profilesById = Map.copyOf(profilesById);
        this.typesById = Map.copyOf(typesById);
    }

    public MobWavesModuleSettings module() {
        return module;
    }

    public GuiGeneralSettings gui() {
        return gui;
    }

    public Collection<WaveProfileDefinition> profiles() {
        return profilesById.values();
    }

    public Collection<HordeTypeDefinition> types() {
        return typesById.values();
    }

    public Map<String, HordeTypeDefinition> typesById() {
        return typesById;
    }

    public Optional<WaveProfileDefinition> profile(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(profilesById.get(profileId.toLowerCase()));
    }

    public Optional<HordeTypeDefinition> type(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(typesById.get(typeId.toLowerCase()));
    }
}
