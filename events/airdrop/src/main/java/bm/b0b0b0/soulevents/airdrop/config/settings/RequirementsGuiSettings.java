package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class RequirementsGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 6;

    @Comment(@CommentValue("Слот «Назад»."))
    public int backSlot = 45;

    @Comment(@CommentValue("Переключатель «требовать право»."))
    public int permissionToggleSlot = 20;

    @Comment(@CommentValue("Переключатель «требовать предмет». ЛКМ — вкл/выкл, ПКМ — редактор."))
    public int customItemToggleSlot = 24;

    @Comment(@CommentValue("Переключатель режима: ALL (весь сет) / ANY (один из)."))
    public int matchModeSlot = 22;

    @Comment(@CommentValue("Справка по экрану (только текст)."))
    public int guideSlot = 13;

    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public String permissionToggleMaterial = Material.IRON_DOOR.name();
    public String matchModeMaterial = Material.COMPARATOR.name();
    public String customItemToggleMaterial = Material.TRIPWIRE_HOOK.name();
    public String guideMaterial = Material.BOOK.name();
    public String fillerMaterial = Material.GRAY_STAINED_GLASS_PANE.name();
}
