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

    @Comment(@CommentValue("Action bar игроку при подборе дропа с моба."))
    public boolean pickupActionBarEnabled = true;

    @Comment(@CommentValue("Lang-ключи action bar (случайный при каждом подборе)."))
    public List<String> pickupActionBarKeys = defaultPickupActionBarKeys();

    private static List<String> defaultLabelKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("mobwaves.loot-label.pickup");
        keys.add("mobwaves.loot-label.rare");
        return keys;
    }

    private static List<String> defaultPickupActionBarKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("mobwaves.pickup.action.1");
        keys.add("mobwaves.pickup.action.2");
        keys.add("mobwaves.pickup.action.3");
        keys.add("mobwaves.pickup.action.4");
        keys.add("mobwaves.pickup.action.5");
        return keys;
    }
}
