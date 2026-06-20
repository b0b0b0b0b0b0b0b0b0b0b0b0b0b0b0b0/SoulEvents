package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Comment(@CommentValue("Лут орды. Файл loot/<lootTableId>.yml"))
public final class LootTableSettings extends YamlSerializable {

    @Comment(@CommentValue("Пул из GUI (Base64). Если не пуст — ролл из пула."))
    public List<String> poolItemsBase64 = new ArrayList<>();

    @Comment(@CommentValue("Маски обфускации вместо угля (до 3, Base64)."))
    public List<String> obfuscationItemsBase64 = new ArrayList<>();

    @Comment(@CommentValue("Сколько предметов брать из пула за один ролл (если pool не пуст)."))
    public int occupiedSlots = 1;

    public List<LootEntrySettings> entries = defaultEntries();

    public List<String> extraItemsBase64 = new ArrayList<>();

    private static List<LootEntrySettings> defaultEntries() {
        List<LootEntrySettings> entries = new ArrayList<>();
        entries.add(LootEntrySettings.of(Material.IRON_INGOT.name(), 2, 8, 1.0));
        entries.add(LootEntrySettings.of(Material.GOLD_INGOT.name(), 1, 4, 0.85));
        entries.add(LootEntrySettings.of(Material.DIAMOND.name(), 1, 2, 0.35));
        entries.add(LootEntrySettings.of(Material.EMERALD.name(), 1, 3, 0.5));
        return entries;
    }
}
