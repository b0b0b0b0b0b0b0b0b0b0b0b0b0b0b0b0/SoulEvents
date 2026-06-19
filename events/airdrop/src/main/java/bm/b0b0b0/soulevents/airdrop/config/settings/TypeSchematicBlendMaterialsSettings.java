package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.List;

public final class TypeSchematicBlendMaterialsSettings extends YamlSerializable {

    @Comment(@CommentValue("Блоки, которые blend может заменить на верхний слой/грунт."))
    public List<String> replaceable = defaultReplaceable();

    public static List<String> defaultReplaceable() {
        return List.of(
                "STONE",
                "COBBLESTONE",
                "DEEPSLATE",
                "COBBLED_DEEPSLATE",
                "ANDESITE",
                "DIORITE",
                "GRANITE",
                "DIRT",
                "GRASS_BLOCK",
                "COARSE_DIRT",
                "SAND",
                "GRAVEL",
                "PODZOL",
                "MYCELIUM",
                "MOSS_BLOCK"
        );
    }
}
