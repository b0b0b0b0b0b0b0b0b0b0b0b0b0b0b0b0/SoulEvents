package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

import java.util.ArrayList;
import java.util.List;

public final class VolcanoVisualSettings {

    @Comment(@CommentValue("Дым из жерла вулкана (кубы дыма)."))
    public boolean ventSmokeEnabled = true;

    public String ventSmokeParticle = "CAMPFIRE_COSY_SMOKE";

    public int ventSmokeCount = 5;

    @Comment(@CommentValue("Дым над блоками магмы/лавы в схеме (частицы)."))
    public boolean magmaSmokeEnabled = true;

    public String magmaSmokeParticle = "CAMPFIRE_COSY_SMOKE";

    public String magmaSmokeSecondaryParticle = "LARGE_SMOKE";

    public String magmaSmokeDarkParticle = "SMOKE";

    public int magmaSmokeCount = 0;

    public int magmaSmokeParticlesPerPlume = 5;

    public int magmaSmokeVerticalLayers = 1;

    public int magmaSmokeIntervalTicks = 20;

    public double magmaSmokeSpawnOffsetY = 0.35;

    public double magmaSmokeCubeHalfSize = 0.14;

    public double magmaSmokeSpread = 0.14;

    public double magmaSmokeVerticalSpread = 0.55;

    public double magmaSmokeRiseSpeed = 0.11;

    public int magmaSmokeMaxPointsPerTick = 4;

    @Comment(@CommentValue("TextDisplay над летящим/лежащим предметом."))
    public boolean itemLabelEnabled = true;

    public double itemLabelOffsetY = 0.55;

    public float itemLabelScale = 0.8f;

    @Comment(@CommentValue("false = подпись прячется за блоками."))
    public boolean itemLabelSeeThrough = false;

    @Comment(@CommentValue("Lang-ключи подписей «подбери» (случайный на предмет)."))
    public List<String> itemLabelKeys = defaultLabelKeys();

    @Comment(@CommentValue("Action bar игроку при подборе дропа."))
    public boolean pickupActionBarEnabled = true;

    @Comment(@CommentValue("Lang-ключи action bar (случайный при каждом подборе)."))
    public List<String> pickupActionBarKeys = defaultPickupActionBarKeys();

    private static List<String> defaultLabelKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("volcano.label.pickup.1");
        keys.add("volcano.label.pickup.2");
        keys.add("volcano.label.pickup.3");
        keys.add("volcano.label.pickup.4");
        return keys;
    }

    private static List<String> defaultPickupActionBarKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("volcano.pickup.action.1");
        keys.add("volcano.pickup.action.2");
        keys.add("volcano.pickup.action.3");
        keys.add("volcano.pickup.action.4");
        keys.add("volcano.pickup.action.5");
        return keys;
    }
}
