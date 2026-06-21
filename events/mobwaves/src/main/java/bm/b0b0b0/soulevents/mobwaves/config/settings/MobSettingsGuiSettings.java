package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class MobSettingsGuiSettings extends YamlSerializable {

    public int rows = 6;
    public int infoSlot = 4;
    public String infoMaterial = Material.BOOK.name();
    public int backSlot = 49;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int mobListStartSlot = 9;
    public int mobListEndSlot = 44;
}
