package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class TypeListGuiSettings {

    @Comment(@CommentValue("Первый слот списка типов."))
    public int startSlot = 10;

    @Comment(@CommentValue("Material иконки типа по умолчанию."))
    public String defaultIconMaterial = Material.CHEST.name();

    @Comment(@CommentValue("Material если тип выключен."))
    public String disabledIconMaterial = Material.BARRIER.name();
}
