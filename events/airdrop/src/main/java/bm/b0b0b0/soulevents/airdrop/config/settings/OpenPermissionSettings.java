package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public class OpenPermissionSettings extends YamlSerializable {

    @Comment(@CommentValue("Нужно право, чтобы открыть сундук этого типа."))
    public boolean enabled = false;

    @Comment(@CommentValue("Пусто = soulevents.airdrop.open.<id типа>."))
    public String permission = "";
}
