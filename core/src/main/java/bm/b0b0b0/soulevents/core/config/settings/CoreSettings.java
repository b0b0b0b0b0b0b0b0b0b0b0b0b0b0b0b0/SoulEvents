package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public class CoreSettings extends YamlSerializable {

    @Comment(@CommentValue("Локаль по умолчанию (файл lang/{locale}.yml без расширения)."))
    public String defaultLocale = "ru";

    @Comment(@CommentValue("Доступные локали в папке lang/."))
    public List<String> locales = defaultLocales();

    private static List<String> defaultLocales() {
        List<String> list = new ArrayList<>();
        list.add("ru");
        list.add("en");
        return list;
    }
}
