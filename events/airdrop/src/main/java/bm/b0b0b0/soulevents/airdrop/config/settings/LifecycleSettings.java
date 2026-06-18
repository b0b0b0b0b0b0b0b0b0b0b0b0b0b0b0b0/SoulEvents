package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class LifecycleSettings extends YamlSerializable {

    @Comment(@CommentValue("Мин. секунд до открытия сундука (всегда, даже для default)."))
    public int minOpenDelaySeconds = 60;

    @Comment(@CommentValue("Секунд до удаления аirdrop после полного опустошения сундука."))
    public int cleanupSecondsAfterLooted = 15;
}
