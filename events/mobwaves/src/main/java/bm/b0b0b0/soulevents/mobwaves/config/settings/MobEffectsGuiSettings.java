package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class MobEffectsGuiSettings extends YamlSerializable {

    public int rows = 5;
    public int infoSlot = 4;
    public String infoMaterial = Material.BOOK.name();
    public int backSlot = 40;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int addSlot = 22;
    public String addMaterial = Material.LIME_DYE.name();
    public int listStartSlot = 10;
    public int listEndSlot = 34;
}
