package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public class BroadcastSettings extends YamlSerializable {

    @Comment(@CommentValue("Глобальные broadcast-сообщения в чат по этапам аирдропа."))
    public boolean enabled = true;

    @Comment(@CommentValue("Сообщение при появлении аирдропа."))
    public boolean spawnEnabled = true;

    @Comment(@CommentValue("Lang-ключ: появление (MiniMessage, multiline)."))
    public String messageKey = "airdrop.broadcast.default";

    @Comment(@CommentValue("Сообщение когда таймер истёк и сундук можно открыть."))
    public boolean unlockedEnabled = true;

    @Comment(@CommentValue("Lang-ключ: сундук разблокирован."))
    public String unlockedMessageKey = "airdrop.broadcast.unlocked";

    @Comment(@CommentValue("Сообщение когда игрок впервые открыл сундук."))
    public boolean openedEnabled = true;

    @Comment(@CommentValue("Lang-ключ: первое открытие (<player>)."))
    public String openedMessageKey = "airdrop.broadcast.opened";

    @Comment(@CommentValue("Сообщение когда сундук опустошён."))
    public boolean lootedEnabled = true;

    @Comment(@CommentValue("Lang-ключ: разграблен."))
    public String lootedMessageKey = "airdrop.broadcast.looted";

    @Comment(@CommentValue("Сообщение когда аирдроп удалён с карты."))
    public boolean removedEnabled = true;

    @Comment(@CommentValue("Lang-ключ: исчез / очистка."))
    public String removedMessageKey = "airdrop.broadcast.removed";
}
