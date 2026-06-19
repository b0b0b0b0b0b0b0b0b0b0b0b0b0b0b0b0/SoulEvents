package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

import java.util.ArrayList;
import java.util.List;

public final class LocaleSettings {

    @Comment(@CommentValue("Локаль по умолчанию (файл lang/{locale}.yml)."))
    public String defaultLocale = "ru";

    @Comment(@CommentValue("Запасная локаль, если ключ не найден."))
    public String fallbackLocale = "en";

    @Comment(@CommentValue("Доступные локали."))
    public List<String> locales = defaultLocales();

    private static List<String> defaultLocales() {
        List<String> list = new ArrayList<>();
        list.add("ru");
        list.add("en");
        return list;
    }
}
