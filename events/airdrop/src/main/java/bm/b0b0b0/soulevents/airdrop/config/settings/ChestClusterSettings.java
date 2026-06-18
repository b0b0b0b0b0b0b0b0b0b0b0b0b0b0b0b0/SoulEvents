package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class ChestClusterSettings {

    @Comment(@CommentValue("Четыре сундука вокруг центра (носами наружу)."))
    public boolean enabled = true;

    @Comment(@CommentValue("ALL — свой лут в каждом сундуке; ONE_RANDOM — лут только в одном случайном."))
    public String lootMode = "ALL";

    @Comment(@CommentValue("Пусто = material из loot/<type>.yml (обычно ENDER_CHEST). CHEST — если нужен поворот «носом»."))
    public String chestMaterial = "";

    private ChestClusterSettings() {
    }

    public static ChestClusterSettings defaults() {
        return new ChestClusterSettings();
    }

    public boolean isDecoyMode() {
        return "ONE_RANDOM".equalsIgnoreCase(lootMode) || "ONE".equalsIgnoreCase(lootMode);
    }
}
