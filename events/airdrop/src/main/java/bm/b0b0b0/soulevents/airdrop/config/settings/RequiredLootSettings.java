package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public class RequiredLootSettings extends YamlSerializable {

    @Comment(@CommentValue("Для открытия сундука нужен особый предмет в руке."))
    public boolean enabled = false;

    @Comment(@CommentValue("Подходящие предметы — Base64 ItemStack (список)."))
    public List<String> requiredItemsBase64 = new ArrayList<>();
}
