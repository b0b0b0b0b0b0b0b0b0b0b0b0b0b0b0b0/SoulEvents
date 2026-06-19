package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.List;

public final class SchematicTerrainMaterialsSettings extends YamlSerializable {

    @Comment(@CommentValue("Блоки «верхнего слоя» при подгонке рельефа (fill/blend)."))
    public List<String> naturalTop = defaultNaturalTop();

    @Comment(@CommentValue("Блоки, которые можно срезать при опускании колонки под footprint."))
    public List<String> removable = defaultRemovable();

    public static List<String> defaultNaturalTop() {
        return List.of(
                "GRASS_BLOCK",
                "PODZOL",
                "MYCELIUM",
                "DIRT",
                "COARSE_DIRT",
                "ROOTED_DIRT",
                "SAND",
                "RED_SAND",
                "SNOW_BLOCK",
                "MOSS_BLOCK"
        );
    }

    public static List<String> defaultRemovable() {
        return List.of(
                "GRASS_BLOCK",
                "PODZOL",
                "MYCELIUM",
                "DIRT",
                "COARSE_DIRT",
                "ROOTED_DIRT",
                "SAND",
                "RED_SAND",
                "GRAVEL",
                "SNOW",
                "SNOW_BLOCK",
                "MOSS_BLOCK",
                "STONE",
                "COBBLESTONE",
                "ANDESITE",
                "DIORITE",
                "GRANITE",
                "TALL_GRASS",
                "SHORT_GRASS",
                "FERN",
                "LARGE_FERN",
                "DEAD_BUSH",
                "DANDELION",
                "POPPY",
                "BLUE_ORCHID",
                "ALLIUM",
                "AZURE_BLUET",
                "RED_TULIP",
                "ORANGE_TULIP",
                "WHITE_TULIP",
                "PINK_TULIP",
                "OXEYE_DAISY",
                "CORNFLOWER",
                "LILY_OF_THE_VALLEY",
                "SUNFLOWER",
                "LILAC",
                "ROSE_BUSH",
                "PEONY"
        );
    }
}
