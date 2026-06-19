package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class TypeSettingsGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 5;

    @Comment(@CommentValue("Слот кнопки назад."))
    public int backSlot = 36;

    @Comment(@CommentValue("Слот вызова в настроенном мире."))
    public int summonSlot = 22;

    @Comment(@CommentValue("Слот телепорта к ближайшему активному."))
    public int teleportSlot = 20;

    @Comment(@CommentValue("Слот пакетного призыва (несколько аирдропов)."))
    public int multiSummonSlot = 24;

    @Comment(@CommentValue("Сколько аирдропов призвать по кнопке пакетного призыва."))
    public int multiSummonCount = 3;

    @Comment(@CommentValue("Слот информации о луте."))
    public int lootInfoSlot = 31;

    @Comment(@CommentValue("Слот «Требования для открытия»."))
    public int requirementsSlot = 40;

    public String summonMaterial = Material.LIME_DYE.name();
    public String teleportMaterial = Material.COMPASS.name();
    public String multiSummonMaterial = Material.TNT.name();
    public String lootInfoMaterial = Material.WRITTEN_BOOK.name();
    public String requirementsMaterial = Material.NAME_TAG.name();
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
}
