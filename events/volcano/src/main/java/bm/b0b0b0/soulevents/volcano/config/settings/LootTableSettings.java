package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Comment(@CommentValue("Лут сундука аирдропа. Файл loot/<typeId>.yml"))
public final class LootTableSettings extends YamlSerializable {

    @Comment(@CommentValue("Материал блока-сундука в мире (например ENDER_CHEST)."))
    public String chestMaterial = "ENDER_CHEST";

    @Comment(@CommentValue("Слотов в GUI лута (9, 18, 27, 36, 45, 54)."))
    public int chestSize = 54;

    @Comment(@CommentValue("Минимум стаков после ролла (добирает повторным проходом по таблице). Используется если poolItemsBase64 пуст."))
    public int minItemStacks = 6;

    @Comment(@CommentValue("Сколько ячеек сундука заполнить (рандомные слоты). Максимум — chestSize."))
    public int occupiedSlots = 6;

    @Comment(@CommentValue("Пул лута из GUI (Base64). Если не пуст — ролл из пула, иначе entries/extraItems."))
    public List<String> poolItemsBase64 = new ArrayList<>();

    @Comment(@CommentValue("Маски обфускации вместо угля (до 3, Base64). Пусто — COAL."))
    public List<String> obfuscationItemsBase64 = new ArrayList<>();

    @Comment(@CommentValue("Взвешенные записи лута."))
    public List<LootEntrySettings> entries = defaultEntries();

    @Comment(@CommentValue("Доп. предметы как Base64 ItemStack (точный NBT)."))
    public List<String> extraItemsBase64 = new ArrayList<>();

    private static List<LootEntrySettings> defaultEntries() {
        List<LootEntrySettings> entries = new ArrayList<>();
        entries.add(LootEntrySettings.of(Material.IRON_INGOT.name(), 4, 16, 1.0));
        entries.add(LootEntrySettings.of(Material.GOLD_INGOT.name(), 2, 12, 1.0));
        entries.add(LootEntrySettings.of(Material.DIAMOND.name(), 2, 6, 0.85));
        entries.add(LootEntrySettings.of(Material.EMERALD.name(), 1, 8, 0.75));
        entries.add(LootEntrySettings.of(Material.GOLDEN_APPLE.name(), 1, 3, 0.65));
        entries.add(LootEntrySettings.of(Material.ENCHANTED_GOLDEN_APPLE.name(), 1, 1, 0.15));
        return entries;
    }
}

