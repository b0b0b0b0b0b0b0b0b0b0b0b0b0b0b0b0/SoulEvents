package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class WaveWaveSettingsGuiSettings extends YamlSerializable {

    public int rows = 3;
    public int backSlot = 18;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int toggleBossSlot = 11;
    public String toggleBossMaterial = Material.NETHER_STAR.name();
    public int bossTypeSlot = 13;
    public int bossHealthMinusSlot = 12;
    public int bossHealthPlusSlot = 14;
    public int bossHealthInfoSlot = 4;
    public int deleteWaveSlot = 26;
    public String deleteWaveMaterial = Material.RED_DYE.name();
}
