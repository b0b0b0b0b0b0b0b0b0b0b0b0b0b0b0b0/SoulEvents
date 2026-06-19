package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class RequiredItemsGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 6;

    @Comment(@CommentValue("Слот «Назад»."))
    public int backSlot = 49;

    @Comment(@CommentValue("Справка по редактору."))
    public int guideSlot = 47;

    @Comment(@CommentValue("Сколько верхних слотов редактируемы (0..N-1). Хватит на полный сет + оружие."))
    public int editableSlotCount = 45;

    public String backMaterial = Material.LIME_DYE.name();
    public String fillerMaterial = Material.GRAY_STAINED_GLASS_PANE.name();
    public String guideMaterial = Material.BOOK.name();
}

