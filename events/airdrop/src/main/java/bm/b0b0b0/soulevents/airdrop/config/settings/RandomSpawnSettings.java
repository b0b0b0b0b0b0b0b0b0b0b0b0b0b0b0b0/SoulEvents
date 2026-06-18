package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public class RandomSpawnSettings extends YamlSerializable {

    @Comment(@CommentValue("Мин. расстояние от центра по X/Z."))
    public int minRadiusFromCenter = 500;

    @Comment(@CommentValue("Макс. расстояние от центра по X/Z."))
    public int maxRadiusFromCenter = 5000;

    @Comment(@CommentValue("Центр = spawn мира. Если false — centerX / centerZ."))
    public boolean useWorldSpawnAsCenter = true;

    @Comment(@CommentValue("Центр X, если useWorldSpawnAsCenter = false."))
    public int centerX = 0;

    @Comment(@CommentValue("Центр Z, если useWorldSpawnAsCenter = false."))
    public int centerZ = 0;

    @Comment(@CommentValue("Попыток найти подходящую точку на поверхности."))
    public int maxAttempts = 48;

    @Comment(@CommentValue("Смещение Y над highest block в выбранных X/Z."))
    public int surfaceYOffset = 1;

    @Comment(@CommentValue("Не спавнить за пределами радиуса карты от центра (X/Z)."))
    public boolean mapBoundaryEnabled = true;

    @Comment(@CommentValue("Макс. радиус карты от центра в блоках. 0 = без лимита."))
    public int mapBoundaryRadius = 7500;

    @Comment(@CommentValue("Запас от границы карты в блоках (уменьшает эффективный радиус)."))
    public int mapBoundaryMargin = 64;

    @Comment(@CommentValue("Искать ровную площадку под объект (ядро FlatSurfaceFinder)."))
    public boolean requireFlatSurface = true;

    @Comment(@CommentValue("Макс. перепад highest block между точками следа (0 = идеально ровно)."))
    public int flatMaxHeightDelta = 0;

    @Comment(@CommentValue("Свободных блоков воздуха над каждой точкой следа."))
    public int flatMinAirAbove = 2;
}
