package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public class GateProfileSettings extends YamlSerializable {

    @Comment(@CommentValue("Запретить участие в инвизе."))
    public boolean denyInvisible = true;

    @Comment(@CommentValue("Запретить участие в полёте."))
    public boolean denyFlying = true;

    @Comment(@CommentValue("Обязателен нагрудник."))
    public boolean requireChestplate = false;

    @Comment(@CommentValue("Обязательны поножи."))
    public boolean requireLeggings = false;

    @Comment(@CommentValue("Обязательны ботинки."))
    public boolean requireBoots = false;

    @Comment(@CommentValue("Обязателен шлем."))
    public boolean requireHelmet = false;

    @Comment(@CommentValue("В руке — один из предметов списка (Base64 ItemStack)."))
    public List<String> requiredHeldItemsBase64 = new ArrayList<>();

    @Comment(@CommentValue("В руке нельзя держать предметы из списка (Base64 ItemStack)."))
    public List<String> forbiddenHeldItemsBase64 = new ArrayList<>();

    @Comment(@CommentValue("Обязательная броня из списка (Base64 ItemStack)."))
    public List<String> requiredArmorBase64 = new ArrayList<>();
}
