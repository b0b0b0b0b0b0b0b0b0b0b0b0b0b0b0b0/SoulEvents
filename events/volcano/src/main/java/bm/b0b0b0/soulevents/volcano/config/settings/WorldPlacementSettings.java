package bm.b0b0b0.soulevents.volcano.config.settings;

import bm.b0b0b0.soulevents.api.world.WorldListMode;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public class WorldPlacementSettings extends YamlSerializable {

    @Comment(@CommentValue("WHITELIST — только миры из списка; BLACKLIST — все, кроме списка."))
    public WorldListMode worldListMode = WorldListMode.WHITELIST;

    @Comment(@CommentValue("Имена миров для whitelist/blacklist."))
    public List<String> worlds = defaultWorlds();

    @Comment(@CommentValue("Мир спавна аирдропа. Только он, не мир игрока."))
    public String spawnWorld = "world";

    @Comment(@CommentValue("Проверять регионы WorldGuard в точке спавна."))
    public boolean worldGuardEnabled = false;

    @Comment(@CommentValue("WHITELIST — только регионы из списка; BLACKLIST — все, кроме списка."))
    public WorldListMode regionListMode = WorldListMode.BLACKLIST;

    @Comment(@CommentValue("ID регионов WorldGuard (без мира, только имя региона)."))
    public List<String> regions = new ArrayList<>();

    @Comment(@CommentValue("Запретить спавн внутри любого WG-региона (кроме временного аирдропа)."))
    public boolean denySpawnInsideRegions = true;

    @Comment(@CommentValue("Мин. блоков до ближайшего WG-региона. 0 = не проверять."))
    public int minBlocksFromNearestRegion = 32;

    private static List<String> defaultWorlds() {
        List<String> list = new ArrayList<>();
        list.add("world");
        return list;
    }
}
