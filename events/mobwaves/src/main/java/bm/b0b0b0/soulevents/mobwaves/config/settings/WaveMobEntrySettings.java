package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public final class WaveMobEntrySettings extends YamlSerializable {

    public String entityType = "ZOMBIE";
    public int count = 1;

    @Comment(@CommentValue("HP только для этой записи. 0 = из mob-overrides профиля."))
    public double maxHealth = 0.0;
}
