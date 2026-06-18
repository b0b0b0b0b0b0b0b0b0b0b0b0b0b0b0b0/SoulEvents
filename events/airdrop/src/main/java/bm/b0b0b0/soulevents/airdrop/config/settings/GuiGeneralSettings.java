package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("GUI SoulEvents-AirDrop — слоты и Material. Тексты — lang/"))
public final class GuiGeneralSettings extends YamlSerializable {

    @Comment(@CommentValue("Админ-меню /airdrop admin"))
    public AdminHubGuiSettings adminHub = new AdminHubGuiSettings();

    @NewLine
    @Comment(@CommentValue("Список типов (ЛКМ — настройки)"))
    public TypeListGuiSettings typeList = new TypeListGuiSettings();

    @NewLine
    public TypeSettingsGuiSettings typeSettings = new TypeSettingsGuiSettings();

    @NewLine
    public CreateGuiSettings create = new CreateGuiSettings();
}
