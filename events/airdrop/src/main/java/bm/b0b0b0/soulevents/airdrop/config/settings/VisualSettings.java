package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class VisualSettings {

    @Comment(@CommentValue("Частицы и звук при появлении."))
    public boolean spawnEffectsEnabled = true;

    @Comment(@CommentValue("Имя частицы Bukkit (Particle)."))
    public String spawnParticle = "TOTEM_OF_UNDYING";

    @Comment(@CommentValue("Количество частиц."))
    public int spawnParticleCount = 64;

    @Comment(@CommentValue("Голограмма TextDisplay над сундуком."))
    public boolean hologramEnabled = true;

    @Comment(@CommentValue("Отдельная голограмма на каждую точку лута (если несколько bedrock-маркеров)."))
    public boolean hologramPerLootPoint = true;

    @Comment(@CommentValue("Смещение X от центра блока сундука."))
    public double hologramOffsetX = 0.0;

    @Comment(@CommentValue("Смещение Y над сундуком."))
    public double hologramOffsetY = 2.35;

    @Comment(@CommentValue("Смещение Z от центра блока сундука."))
    public double hologramOffsetZ = 0.0;

    @Comment(@CommentValue("Масштаб текста по X."))
    public float hologramScaleX = 1.0f;

    @Comment(@CommentValue("Масштаб текста по Y."))
    public float hologramScaleY = 1.0f;

    @Comment(@CommentValue("Масштаб текста по Z."))
    public float hologramScaleZ = 1.0f;

    @Comment(@CommentValue("CENTER | FIXED | VERTICAL | HORIZONTAL"))
    public String hologramBillboard = "CENTER";

    @Comment(@CommentValue("false = голограмма прячется за блоками (рекомендуется). true = текст виден сквозь стены."))
    public boolean hologramSeeThrough = false;

    @Comment(@CommentValue("Тень у текста."))
    public boolean hologramShadowed = true;

    @Comment(@CommentValue("LEFT | CENTER | RIGHT"))
    public String hologramAlignment = "CENTER";

    @Comment(@CommentValue("Ширина строки (Display line width)."))
    public int hologramLineWidth = 200;

    @Comment(@CommentValue("Прозрачность текста 0–255."))
    public int hologramTextOpacity = 255;

    @Comment(@CommentValue("Фон под текстом."))
    public boolean hologramBackgroundEnabled = true;

    @Comment(@CommentValue("ARGB фона: альфа 0–255."))
    public int hologramBackgroundAlpha = 90;

    public int hologramBackgroundRed = 0;

    public int hologramBackgroundGreen = 0;

    public int hologramBackgroundBlue = 0;

    @Comment(@CommentValue("Дальность отрисовки Display (блоки)."))
    public float hologramViewRange = 64.0f;

    @Comment(@CommentValue("Интервал обновления голограммы (тики)."))
    public int hologramTickIntervalTicks = 10;

    @Comment(@CommentValue("Lang-ключ до открытия сундука."))
    public String hologramWaitingKey = "airdrop.hologram.body-waiting";

    @Comment(@CommentValue("Lang-ключ когда сундук открыт и в нём есть лут."))
    public String hologramLootableKey = "airdrop.hologram.lootable";

    @Comment(@CommentValue("Голограмма: можно лутать + таймер исчезновения."))
    public String hologramLootableDespawnKey = "airdrop.hologram.lootable-despawn";

    @Comment(@CommentValue("Lang-ключ когда сундук опустошён (ещё без таймера удаления)."))
    public String hologramLootedKey = "airdrop.hologram.looted";

    @Comment(@CommentValue("Lang-ключ когда сундук опустошён и идёт отсчёт до удаления."))
    public String hologramLootedDespawnKey = "airdrop.hologram.looted-despawn";

    @Comment(@CommentValue("Постоянные частицы у сундука."))
    public boolean ambientEffectsEnabled = true;

    @Comment(@CommentValue("Частицы вокруг сундука (имя Particle)."))
    public String ambientParticle = "WITCH";

    @Comment(@CommentValue("Сколько ambient-частиц за тик."))
    public int ambientParticleCount = 6;

    @Comment(@CommentValue("Радиус игрока для ambient-частиц (блоки)."))
    public int ambientViewerRadius = 72;
}
