package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class WaveWaveSettingsGuiSettings extends YamlSerializable {

    public int rows = 4;
    public int infoSlot = 4;
    public String infoMaterial = Material.BOOK.name();
    public int backSlot = 31;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int toggleBossSlot = 10;
    public String toggleBossMaterial = Material.NETHER_STAR.name();
    public int bossTypeSlot = 12;
    public int bossHealthMinusSlot = 19;
    public int bossHealthInfoSlot = 20;
    public int bossHealthPlusSlot = 21;
    public int deleteWaveSlot = 23;
    public String deleteWaveMaterial = Material.RED_DYE.name();
}
