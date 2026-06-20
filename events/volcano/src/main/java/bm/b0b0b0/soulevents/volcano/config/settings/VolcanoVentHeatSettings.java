package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

import java.util.ArrayList;
import java.util.List;

public final class VolcanoVentHeatSettings {

    @Comment(@CommentValue("Урон игрокам на bedrock/угле у жерла во время извержения."))
    public boolean enabled = true;

    @Comment(@CommentValue("Проверка урона каждые N тиков."))
    public int intervalTicks = 10;

    @Comment(@CommentValue("Урон за тик проверки (2 = 1 сердечко)."))
    public double damage = 3.0;

    @Comment(@CommentValue("Горизонтальный радиус от жерла для hot-блоков (блоки)."))
    public double ventRadiusBlocks = 5.0;

    @Comment(@CommentValue("Поджигать на N тиков (0 = без огня)."))
    public int burnTicks = 20;

    @Comment(@CommentValue("Отталкивание от жерла (0 = выкл.)."))
    public double knockback = 0.35;

    @Comment(@CommentValue("Material hot-площадки в схеме (BEDROCK, COAL_BLOCK и т.д.)."))
    public List<String> blockMaterials = defaultBlockMaterials();

    private static List<String> defaultBlockMaterials() {
        List<String> materials = new ArrayList<>();
        materials.add("BEDROCK");
        materials.add("COAL_BLOCK");
        return materials;
    }
}
