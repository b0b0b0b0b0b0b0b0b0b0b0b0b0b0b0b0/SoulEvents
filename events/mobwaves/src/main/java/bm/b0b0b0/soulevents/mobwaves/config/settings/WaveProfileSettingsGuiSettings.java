package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class WaveProfileSettingsGuiSettings extends YamlSerializable {

    public int rows = 4;
    public int backSlot = 27;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int spawnRadiusInfoSlot = 10;
    public int spawnRadiusMinusSlot = 9;
    public int spawnRadiusPlusSlot = 11;
    public int batchSizeInfoSlot = 13;
    public int batchSizeMinusSlot = 12;
    public int batchSizePlusSlot = 14;
    public int intervalInfoSlot = 16;
    public int intervalMinusSlot = 15;
    public int intervalPlusSlot = 17;
    public int graceInfoSlot = 22;
    public int graceMinusSlot = 21;
    public int gracePlusSlot = 23;
}
