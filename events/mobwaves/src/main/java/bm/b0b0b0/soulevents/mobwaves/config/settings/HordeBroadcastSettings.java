package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HordeBroadcastSettings extends YamlSerializable {

    public boolean enabled = true;
    public boolean spawnEnabled = true;
    public String messageKey = "mobwaves.broadcast.spawn";
    public boolean clearedEnabled = true;
    public String clearedMessageKey = "mobwaves.broadcast.cleared";
    public boolean removedEnabled = true;
    public String removedMessageKey = "mobwaves.broadcast.removed";
}
