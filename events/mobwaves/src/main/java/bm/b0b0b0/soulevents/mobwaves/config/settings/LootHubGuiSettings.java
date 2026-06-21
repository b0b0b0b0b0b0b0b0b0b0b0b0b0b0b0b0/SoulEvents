package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class LootHubGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 4;

    @Comment(@CommentValue("Слот «Назад»."))
    public int infoSlot = 4;

    public String infoMaterial = Material.BOOK.name();

    public int backSlot = 31;

    @Comment(@CommentValue("Кнопка «Маски обфускации»."))
    public int obfuscationSlot = 11;

    @Comment(@CommentValue("Кнопка «Пул лута»."))
    public int poolSlot = 15;

    @Comment(@CommentValue("Слот «−» для occupiedSlots."))
    public int occupiedMinusSlot = 28;

    @Comment(@CommentValue("Слот отображения occupiedSlots."))
    public int occupiedInfoSlot = 29;

    @Comment(@CommentValue("Слот «+» для occupiedSlots."))
    public int occupiedPlusSlot = 30;

    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
    public String obfuscationMaterial = Material.COAL.name();
    public String poolMaterial = Material.CHEST.name();
    public String occupiedMinusMaterial = Material.RED_DYE.name();
    public String occupiedPlusMaterial = Material.LIME_DYE.name();
    public String occupiedInfoMaterial = Material.PAPER.name();
}
