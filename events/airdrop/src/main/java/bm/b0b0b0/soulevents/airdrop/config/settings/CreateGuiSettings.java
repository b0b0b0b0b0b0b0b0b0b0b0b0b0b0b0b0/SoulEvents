package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class CreateGuiSettings {

    @Comment(@CommentValue("Строк инвентаря."))
    public int rows = 3;

    @Comment(@CommentValue("Слот назад."))
    public int backSlot = 22;

    @Comment(@CommentValue("Пресеты редкости."))
    public int commonSlot = 11;
    public int rareSlot = 13;
    public int donateSlot = 15;

    public String commonMaterial = Material.CHEST.name();
    public String rareMaterial = Material.ENDER_CHEST.name();
    public String donateMaterial = Material.GOLD_BLOCK.name();
    public String backMaterial = Material.LIGHT_GRAY_DYE.name();
}
