package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class MobWavesModuleSettings extends YamlSerializable {

    @Comment(@CommentValue("Локали lang/ (defaultLocale, fallbackLocale)."))
    public LocaleSettings locale = new LocaleSettings();

    @Comment(@CommentValue("Радиус спавна по умолчанию, если в профиле/типе 0."))
    public int defaultSpawnRadius = 14;

    @Comment(@CommentValue("Сколько мобов выходит за один «пакет» внутри волны."))
    public int defaultBatchSize = 3;

    @Comment(@CommentValue("Тиков между пакетами спавна в одной волне."))
    public int defaultBatchIntervalTicks = 40;

    @Comment(@CommentValue("Секунд открытого сундука после зачистки волны, до следующей."))
    public int defaultGraceAfterClearSeconds = 20;

    @Comment(@CommentValue("Макс. одновременно активных орд на сервере."))
    public int maxConcurrentTotal = 8;

    @Comment(@CommentValue("Подробный лог поиска точки орды и каждой попытки спавна моба в консоль."))
    public boolean spawnDebugEnabled = true;

    @Comment(@CommentValue("Лог урона: моб→игрок, игрок→моб, friendly-fire блок."))
    public boolean combatDebugEnabled = true;

    @Comment(@CommentValue("Боевая мощь мобов орды (HP, урон, броня, солнце)."))
    public HordeMobCombatSettings hordeCombat = new HordeMobCombatSettings();
}
