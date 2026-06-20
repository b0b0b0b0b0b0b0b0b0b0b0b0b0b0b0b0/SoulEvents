package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class TypeListGuiSettings extends YamlSerializable {

    public int startSlot = 10;
    public String defaultIconMaterial = Material.SPAWNER.name();
    public String disabledIconMaterial = Material.BARRIER.name();
}
