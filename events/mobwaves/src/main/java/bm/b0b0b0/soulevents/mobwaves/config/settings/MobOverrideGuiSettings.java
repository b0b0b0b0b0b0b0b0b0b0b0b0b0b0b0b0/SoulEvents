package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class MobOverrideGuiSettings extends YamlSerializable {

    public int rows = 5;
    public int infoSlot = 4;
    public String infoMaterial = Material.BOOK.name();
    public int backSlot = 40;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int healthMinusSlot = 10;
    public int healthInfoSlot = 11;
    public int healthPlusSlot = 12;
    public int speedMinusSlot = 19;
    public int speedInfoSlot = 20;
    public int speedPlusSlot = 21;
    public int damageMinusSlot = 28;
    public int damageInfoSlot = 29;
    public int damagePlusSlot = 30;
    public int effectsSlot = 37;
    public String effectsMaterial = Material.POTION.name();
}
