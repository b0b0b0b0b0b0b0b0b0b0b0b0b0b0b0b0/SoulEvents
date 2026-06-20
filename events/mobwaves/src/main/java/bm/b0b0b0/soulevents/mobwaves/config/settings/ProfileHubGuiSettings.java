package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class ProfileHubGuiSettings extends YamlSerializable {

    public int rows = 5;
    public int backSlot = 36;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int addWaveSlot = 8;
    public String addWaveMaterial = Material.LIME_DYE.name();
    public int mobSettingsSlot = 4;
    public String mobSettingsMaterial = Material.IRON_SWORD.name();
    public int profileSettingsSlot = 6;
    public String profileSettingsMaterial = Material.REPEATER.name();
    public int waveListStartSlot = 18;
}
