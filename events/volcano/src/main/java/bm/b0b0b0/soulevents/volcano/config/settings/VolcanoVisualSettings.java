package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

import java.util.ArrayList;
import java.util.List;

public final class VolcanoVisualSettings {

    @Comment(@CommentValue("Дым из жерла вулкана (кубы дыма)."))
    public boolean ventSmokeEnabled = true;

    public String ventSmokeParticle = "CAMPFIRE_SIGNAL_SMOKE";

    public int ventSmokeCount = 5;

    @Comment(@CommentValue("Импульс дыма из жерла каждые N тиков."))
    public int ventSmokeIntervalTicks = 10;

    @Comment(@CommentValue("Горизонтальный разброс скорости частиц (меньше = уже столб)."))
    public double ventSmokeSpread = 0.14;

    @Comment(@CommentValue("Вертикальный разброс скорости (не высота столба)."))
    public double ventSmokeVerticalSpread = 0.08;

    public double ventSmokeRiseSpeed = 0.06;

    @Comment(@CommentValue("Дым над блоками магмы/лавы в схеме (частицы)."))
    public boolean magmaSmokeEnabled = true;

    @Comment(@CommentValue("Крупные серые клубы на верхушке столба."))
    public String magmaSmokeParticle = "LARGE_SMOKE";

    public String magmaSmokeSecondaryParticle = "LARGE_SMOKE";

    @Comment(@CommentValue("Основной тёмный дым (CAMPFIRE_SIGNAL_SMOKE — серая струя, не белый COSY)."))
    public String magmaSmokeDarkParticle = "CAMPFIRE_SIGNAL_SMOKE";

    @Comment(@CommentValue("0 = все точки по очереди (magmaSmokeMaxPointsPerTick за импульс)."))
    public int magmaSmokeCount = 0;

    public int magmaSmokeParticlesPerPlume = 2;

    @Comment(@CommentValue("Точек спавна по высоте над магмой (столб без разлёта)."))
    public int magmaSmokeVerticalLayers = 4;

    @Comment(@CommentValue("Импульс дыма каждые N тиков."))
    public int magmaSmokeIntervalTicks = 10;

    public double magmaSmokeSpawnOffsetY = 0.2;

    public double magmaSmokeCubeHalfSize = 0.1;

    public double magmaSmokeSpread = 0.09;

    public double magmaSmokeVerticalSpread = 0.07;

    public double magmaSmokeRiseSpeed = 0.06;

    @Comment(@CommentValue("Расстояние между слоями столба (блоки)."))
    public double magmaSmokeStackStep = 0.55;

    public int magmaSmokeMaxPointsPerTick = 5;

    @Comment(@CommentValue("Огненные частицы на bedrock-маркерах схемы (как у лута на земле, но выше)."))
    public boolean bedrockGlowEnabled = true;

    public double bedrockGlowOffsetY = 0.55;

    @Comment(@CommentValue("Гром и фейковые молнии рядом с вулканом."))
    public VolcanoAmbientSettings ambient = new VolcanoAmbientSettings();

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

    @Comment(@CommentValue("Горячее жерло: урон на bedrock/угле во время извержения."))
    public VolcanoVentHeatSettings ventHeat = new VolcanoVentHeatSettings();

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
