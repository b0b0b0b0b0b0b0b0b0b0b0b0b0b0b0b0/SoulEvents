package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class VisualSettings {

    @Comment(@CommentValue("Частицы и звук при появлении."))
    public boolean spawnEffectsEnabled = true;

    @Comment(@CommentValue("Имя частицы Bukkit (Particle)."))
    public String spawnParticle = "TOTEM_OF_UNDYING";

    @Comment(@CommentValue("Количество частиц."))
    public int spawnParticleCount = 64;

    @Comment(@CommentValue("Голограмма TextDisplay над сундуком."))
    public boolean hologramEnabled = true;

    @Comment(@CommentValue("Смещение Y над сундуком."))
    public double hologramOffsetY = 2.35;

    @Comment(@CommentValue("Lang-ключ когда сундук открыт и в нём есть лут."))
    public String hologramLootableKey = "airdrop.hologram.lootable";

    @Comment(@CommentValue("Голограмма: можно лутать + таймер исчезновения."))
    public String hologramLootableDespawnKey = "airdrop.hologram.lootable-despawn";

    @Comment(@CommentValue("Lang-ключ когда сундук опустошён (ещё без таймера удаления)."))
    public String hologramLootedKey = "airdrop.hologram.looted";

    @Comment(@CommentValue("Lang-ключ когда сундук опустошён и идёт отсчёт до удаления."))
    public String hologramLootedDespawnKey = "airdrop.hologram.looted-despawn";

    @Comment(@CommentValue("Постоянные частицы у сундука."))
    public boolean ambientEffectsEnabled = true;

    @Comment(@CommentValue("Частицы вокруг сундука (имя Particle)."))
    public String ambientParticle = "WITCH";

    @Comment(@CommentValue("Сколько ambient-частиц за тик."))
    public int ambientParticleCount = 6;
}
