package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

@Comment(@CommentValue("Встроенный «Нексус разлома» — без .schem. Строится кодом при спавне, undo при despawn."))
public final class HordeBuiltinNexusSettings extends YamlSerializable {

    @Comment(@CommentValue("true = вставить встроенный монолит (если schematic.enabled = false)."))
    public boolean enabled = true;

    @Comment(@CommentValue("Блоков центральной колонны над поверхностью (видимая «шляпа», 2–3 достаточно)."))
    public int visibleHeight = 2;

    @Comment(@CommentValue("Блоков центральной колонны под поверхностью (уходит в землю)."))
    public int buryDepth = 5;

    @Comment(@CommentValue("Полуширина footprint (2 = диаметр ~4 блока)."))
    public int footprintRadius = 1;

    public String coreMaterial = Material.CRYING_OBSIDIAN.name();

    public String shellMaterial = Material.DEEPSLATE_BRICKS.name();

    public String capMaterial = Material.RESPAWN_ANCHOR.name();

    @Comment(@CommentValue("Эффект soul fire — только частицы на вершине (блок в воздухе не ставится)."))
    public boolean soulFireCap = false;

    @Comment(@CommentValue("Lang-ключ названия структуры в broadcast (<structure_name>)."))
    public String structureNameKey = "mobwaves.structure.rift-nexus";
}
