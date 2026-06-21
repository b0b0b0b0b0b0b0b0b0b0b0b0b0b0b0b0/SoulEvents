package bm.b0b0b0.soulevents.mobwaves.config.settings;



import net.elytrium.serializer.language.object.YamlSerializable;

import org.bukkit.Material;



public final class WaveEditorGuiSettings extends YamlSerializable {



    public int rows = 6;

    public int editableSlotCount = 45;

    public int infoSlot = 4;
    public String infoMaterial = Material.BOOK.name();
    public int waveSettingsSlot = 46;
    public String waveSettingsMaterial = Material.COMPARATOR.name();
    public int bossSlot = 47;
    public int deleteWaveSlot = 52;
    public String deleteWaveMaterial = Material.RED_DYE.name();

    public int backSlot = 45;

    public String backMaterial = Material.LIGHT_GRAY_DYE.name();

    public int saveSlot = 53;

    public String saveMaterial = Material.LIME_DYE.name();

}

