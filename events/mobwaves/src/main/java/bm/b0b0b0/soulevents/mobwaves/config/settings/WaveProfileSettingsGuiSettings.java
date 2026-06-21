package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class WaveProfileSettingsGuiSettings extends YamlSerializable {

    public int rows = 5;
    public int infoSlot = 4;
    public String infoMaterial = Material.BOOK.name();
    public int backSlot = 40;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int spawnRadiusMinusSlot = 10;
    public int spawnRadiusInfoSlot = 11;
    public int spawnRadiusPlusSlot = 12;
    public int batchSizeMinusSlot = 19;
    public int batchSizeInfoSlot = 20;
    public int batchSizePlusSlot = 21;
    public int intervalMinusSlot = 28;
    public int intervalInfoSlot = 29;
    public int intervalPlusSlot = 30;
    public int graceMinusSlot = 37;
    public int graceInfoSlot = 38;
    public int gracePlusSlot = 39;
}
