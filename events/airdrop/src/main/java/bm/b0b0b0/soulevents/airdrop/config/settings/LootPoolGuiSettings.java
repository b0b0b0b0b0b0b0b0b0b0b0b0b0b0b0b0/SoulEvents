package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class LootPoolGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 6;

    @Comment(@CommentValue("Слот «Назад» (нижний ряд, не зона лута)."))
    public int backSlot = 49;

    @Comment(@CommentValue("Слот «Предыдущая страница» (нижний ряд)."))
    public int prevPageSlot = 45;

    @Comment(@CommentValue("Слот «Следующая страница» (нижний ряд)."))
    public int nextPageSlot = 53;

    @Comment(@CommentValue("Индикатор страницы (нижний ряд, только текст)."))
    public int pageInfoSlot = 48;

    @Comment(@CommentValue("Редактируемых слотов — 5 верхних рядов (0..44). Нижний ряд — кнопки."))
    public int editableSlotCount = 45;

    @Comment(@CommentValue("Максимум страниц пула."))
    public int maxPages = 5;

    public String backMaterial = Material.ARROW.name();
    public String prevPageMaterial = Material.LIGHT_GRAY_DYE.name();
    public String nextPageMaterial = Material.GRAY_DYE.name();
    public String pageInfoMaterial = Material.PAPER.name();
    public String fillerMaterial = Material.GRAY_STAINED_GLASS_PANE.name();
}
