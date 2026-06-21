package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class TypeSettingsGuiSettings extends YamlSerializable {

    public int rows = 5;
    public int infoSlot = 4;
    public String infoMaterial = Material.BOOK.name();
    public int backSlot = 40;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int summonSlot = 20;
    public int teleportSlot = 22;
    public int despawnSlot = 24;
    public int waveProfileSlot = 29;
    public int lootInfoSlot = 33;
    public String summonMaterial = Material.LIME_DYE.name();
    public String teleportMaterial = Material.COMPASS.name();
    public String despawnMaterial = Material.BARRIER.name();
    public String waveProfileMaterial = Material.NETHER_STAR.name();
    public String lootInfoMaterial = Material.WRITTEN_BOOK.name();
}
