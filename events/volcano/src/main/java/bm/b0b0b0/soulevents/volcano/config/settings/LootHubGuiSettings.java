package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class LootHubGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 5;

    @Comment(@CommentValue("Слот «Назад»."))
    public int backSlot = 36;

    @Comment(@CommentValue("Кнопка «Маски обфускации»."))
    public int obfuscationSlot = 13;

    @Comment(@CommentValue("Кнопка «Пул лута» (сундук)."))
    public int poolSlot = 31;

    @Comment(@CommentValue("Слот «−» для occupiedSlots."))
    public int occupiedMinusSlot = 28;

    @Comment(@CommentValue("Слот отображения occupiedSlots (бумага)."))
    public int occupiedInfoSlot = 19;

    @Comment(@CommentValue("Слот «+» для occupiedSlots."))
    public int occupiedPlusSlot = 10;

    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public String obfuscationMaterial = Material.COAL.name();
    public String poolMaterial = Material.CHEST.name();
    public String occupiedMinusMaterial = Material.RED_STAINED_GLASS_PANE.name();
    public String occupiedPlusMaterial = Material.LIME_STAINED_GLASS_PANE.name();
    public String occupiedInfoMaterial = Material.PAPER.name();
    public String fillerMaterial = Material.GRAY_STAINED_GLASS_PANE.name();
}
