package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class ProfilesHubGuiSettings extends YamlSerializable {

    public int rows = 5;
    public int backSlot = 36;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int profileListStartSlot = 10;
    public String defaultProfileIconMaterial = Material.NETHER_STAR.name();
}
