package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class MobSettingsGuiSettings extends YamlSerializable {

    public int rows = 6;
    public int backSlot = 45;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int mobListStartSlot = 9;
}
