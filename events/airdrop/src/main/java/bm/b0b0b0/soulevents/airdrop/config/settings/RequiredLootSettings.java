package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public class RequiredLootSettings extends YamlSerializable {

    @Comment(@CommentValue("Нужны кастомные предметы из списка ниже."))
    public boolean enabled = false;

    @Comment(@CommentValue("ALL — каждый шаблон должен быть у игрока (инвентарь + броня + руки). ANY — хотя бы один."))
    public String matchMode = "ALL";

    @Comment(@CommentValue("Шаблоны — Base64 ItemStack.serializeAsBytes(). Полный NBT/PDC, любой размер. GUI: до 45 слотов."))
    public List<String> requiredItemsBase64 = new ArrayList<>();

    public boolean isAnyMatch() {
        return "ANY".equalsIgnoreCase(matchMode) || "ONE".equalsIgnoreCase(matchMode);
    }

    public void cycleMatchMode() {
        matchMode = isAnyMatch() ? "ALL" : "ANY";
    }
}
