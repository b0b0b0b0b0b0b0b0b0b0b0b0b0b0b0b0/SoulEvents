package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class ProfileHubGuiSettings extends YamlSerializable {

    @Comment(@CommentValue("Строк инвентаря (6 = верхние 5 рядов под волны, нижний — кнопки)."))
    public int rows = 6;

    public int infoSlot = 3;
    public String infoMaterial = Material.BOOK.name();
    public int mobSettingsSlot = 4;
    public String mobSettingsMaterial = Material.IRON_SWORD.name();
    public int profileSettingsSlot = 5;
    public String profileSettingsMaterial = Material.REPEATER.name();
    public int addWaveSlot = 8;
    public String addWaveMaterial = Material.LIME_DYE.name();

    @Comment(@CommentValue("Слоты списка волн (включительно). Нижний ряд 45+ — только кнопки."))
    public int waveListStartSlot = 9;
    public int waveListEndSlot = 44;

    public int backSlot = 45;
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public int prevPageSlot = 48;
    public String prevPageMaterial = Material.LIGHT_GRAY_DYE.name();
    public int pageInfoSlot = 49;
    public String pageInfoMaterial = Material.PAPER.name();
    public int nextPageSlot = 53;
    public String nextPageMaterial = Material.GRAY_DYE.name();
}
