package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public final class HordeLootVisualSettings extends YamlSerializable {

    @Comment(@CommentValue("TextDisplay над дропом с моба."))
    public boolean itemLabelEnabled = true;

    public double itemLabelOffsetY = 0.55;

    @Comment(@CommentValue("false = подпись не видна за блоками."))
    public boolean itemLabelSeeThrough = false;

    @Comment(@CommentValue("Lang-ключи подписи (случайный на предмет)."))
    public List<String> itemLabelKeys = defaultLabelKeys();

    private static List<String> defaultLabelKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("mobwaves.loot-label.pickup");
        keys.add("mobwaves.loot-label.rare");
        return keys;
    }
}
