package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class LootEntrySettings {

    @Comment(@CommentValue("Material или Base64 ItemStack (если itemBase64 не пуст)."))
    public String material = "IRON_INGOT";

    @Comment(@CommentValue("Точный предмет Base64 (приоритет над material)."))
    public String itemBase64 = "";

    @Comment(@CommentValue("Мин. количество."))
    public int minAmount = 1;

    @Comment(@CommentValue("Макс. количество."))
    public int maxAmount = 1;

    @Comment(@CommentValue("Шанс 0.0–1.0"))
    public double chance = 1.0;

    public static LootEntrySettings of(String material, int min, int max, double chance) {
        LootEntrySettings entry = new LootEntrySettings();
        entry.material = material;
        entry.minAmount = min;
        entry.maxAmount = max;
        entry.chance = chance;
        return entry;
    }
}
