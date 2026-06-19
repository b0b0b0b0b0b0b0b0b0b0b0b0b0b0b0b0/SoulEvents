package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

@Comment(@CommentValue("Временный WG-регион арены. Настраивается отдельно для каждого типа в types/<id>.yml → arenaWorldGuard."))
public final class ArenaWorldGuardSettings extends YamlSerializable {

    @Comment(@CommentValue("Создавать временный WG-регион на время ивента (перебивает чужие флаги)."))
    public boolean createTempRegion = true;

    @Comment(@CommentValue("Приоритет региона (выше = сильнее перекрывает другие)."))
    public int regionPriority = 1000;

    @Comment(@CommentValue("Горизонтальный отступ (блоки) вокруг схематики, если schematic.enabled."))
    public int marginWithSchematic = 10;

    @Comment(@CommentValue("Горизонтальный радиус (блоки) от сундука, если схематики нет."))
    public int marginWithoutSchematic = 50;

    @Comment(@CommentValue("Растянуть регион по Y на весь мир (аналог /rg expand vert)."))
    public boolean expandVertical = true;

    @Comment(@CommentValue("Доп. отступ по Y, если expandVertical = false."))
    public int verticalMargin = 48;

    @Comment(@CommentValue("Запретить ведра с водой/лавой внутри арены (разлив из ведра)."))
    public boolean denyFluidBuckets = true;

    @Comment(@CommentValue("Deny-правила через listener плагина (игнорирует WG bypass у OP)."))
    public boolean enforceDenyInPlugin = true;

    @Comment(@CommentValue("WG StateFlag ALLOW (kebab-case). Пусто = все флаги ALLOW, кроме denyFlags."))
    public List<String> allowFlags = new ArrayList<>();

    @Comment(@CommentValue("WG StateFlag DENY (kebab-case). Перекрывает allow. Взрывы и жидкости по умолчанию."))
    public List<String> denyFlags = defaultDenyFlags();

    private static List<String> defaultDenyFlags() {
        List<String> flags = new ArrayList<>();
        flags.add("tnt");
        flags.add("creeper-explosion");
        flags.add("other-explosion");
        flags.add("ghast-fireball");
        flags.add("wither-damage");
        flags.add("breeze-wind-charge");
        flags.add("enderdragon-block-damage");
        flags.add("water-flow");
        flags.add("lava-flow");
        flags.add("lava-fire");
        flags.add("enderman-grief");
        return flags;
    }
}
