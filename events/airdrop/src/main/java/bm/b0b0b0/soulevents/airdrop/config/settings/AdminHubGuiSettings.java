package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

public final class AdminHubGuiSettings {

    @Comment(@CommentValue("Строк инвентаря (1–6)."))
    public int rows = 6;

    @Comment(@CommentValue("Слот «Создать новый аирдроп»."))
    public int createSlot = 4;

    @Comment(@CommentValue("Material кнопки создания."))
    public String createMaterial = Material.NETHER_STAR.name();
}
