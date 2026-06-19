package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public class PreOpenMobsSettings extends YamlSerializable {

    @Comment(@CommentValue("Волна мобов перед открытием сундука."))
    public boolean enabled = false;

    @Comment(@CommentValue("ID профиля волны мобов (зарезервировано)."))
    public String profileId = "default";

    @Comment(@CommentValue("Сколько мобов в одной волне."))
    public int mobCount = 6;

    @Comment(@CommentValue("Радиус спавна вокруг сундука."))
    public int spawnRadius = 14;

    @Comment(@CommentValue("Интервал между волнами (сек)."))
    public int waveIntervalSeconds = 45;

    @Comment(@CommentValue("Максимум живых мобов одновременно."))
    public int maxAlive = 12;

    @Comment(@CommentValue("Типы мобов (EntityType)."))
    public List<String> mobTypes = defaultMobTypes();

    private static List<String> defaultMobTypes() {
        List<String> types = new ArrayList<>();
        types.add("ZOMBIE");
        types.add("HUSK");
        types.add("SKELETON");
        return types;
    }
}
