package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("SoulEvents-AirDrop — модуль (лимиты, locale, БД). Типы — types/, лут — loot/, GUI — gui/"))
public final class AirDropModuleSettings extends YamlSerializable {

    @Comment(@CommentValue("Глобальный лимит активных аирдропов всех типов."))
    public int maxConcurrentTotal = 20;

    @Comment(@CommentValue("Подробный лог поиска точки в консоль (не в чат игрокам)."))
    public boolean spawnDebugEnabled = false;

    @NewLine
    @Comment(@CommentValue("Локализация (тексты — lang/ в папке плагина)."))
    public LocaleSettings locale = new LocaleSettings();

    @NewLine
    @Comment(@CommentValue("База данных сессий и истории спавнов."))
    public DatabaseSettings database = new DatabaseSettings();
}
