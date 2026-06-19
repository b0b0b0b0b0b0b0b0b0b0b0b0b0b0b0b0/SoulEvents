package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("SoulEvents-Volcano — модуль. Типы — types/, лут — loot/, GUI — gui/"))
public final class VolcanoModuleSettings extends YamlSerializable {

    @Comment(@CommentValue("Макс. одновременно активных вулканов всех типов."))
    public int maxConcurrentTotal = 3;

    @Comment(@CommentValue("Подробный лог поиска точки в консоль."))
    public boolean spawnDebugEnabled = true;

    @NewLine
    @Comment(@CommentValue("Локализация (тексты — lang/ в папке плагина)."))
    public LocaleSettings locale = new LocaleSettings();
}
