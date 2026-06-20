package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class MobOverrideGuiSettings extends YamlSerializable {

    public int rows = 5;
    public int backSlot = 36;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int healthMinusSlot = 10;
    public int healthInfoSlot = 13;
    public int healthPlusSlot = 16;
    public int speedMinusSlot = 28;
    public int speedPlusSlot = 34;
    public int damageMinusSlot = 37;
    public int damagePlusSlot = 43;
}
