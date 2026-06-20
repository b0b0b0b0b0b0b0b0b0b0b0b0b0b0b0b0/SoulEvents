package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class ObfuscationItemsGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 5;

    @Comment(@CommentValue("Слот «Назад»."))
    public int backSlot = 40;

    @Comment(@CommentValue("Три редактируемых слота по центру (макс. маски обфускации)."))
    public int maskSlotLeft = 21;

    public int maskSlotCenter = 22;

    public int maskSlotRight = 23;

    @Comment(@CommentValue("Справка (только текст)."))
    public int guideSlot = 4;

    public String backMaterial = Material.LIME_DYE.name();
    public String fillerMaterial = Material.BARRIER.name();
    public String guideMaterial = Material.BOOK.name();
}
