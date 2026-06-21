package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("GUI SoulEvents-MobWaves — слоты и Material. Тексты — lang/"))
public final class GuiGeneralSettings extends YamlSerializable {

    @Comment(@CommentValue("Админ-меню /mobwaves admin"))
    public AdminHubGuiSettings adminHub = new AdminHubGuiSettings();

    @NewLine
    @Comment(@CommentValue("Список типов орды"))
    public TypeListGuiSettings typeList = new TypeListGuiSettings();

    @NewLine
    public TypeSettingsGuiSettings typeSettings = new TypeSettingsGuiSettings();

    @NewLine
    @Comment(@CommentValue("Список профилей волн"))
    public ProfilesHubGuiSettings profilesHub = new ProfilesHubGuiSettings();

    @NewLine
    @Comment(@CommentValue("Редактор одного профиля волн"))
    public ProfileHubGuiSettings profileHub = new ProfileHubGuiSettings();

    @NewLine
    public WaveEditorGuiSettings waveEditor = new WaveEditorGuiSettings();

    @NewLine
    public WaveWaveSettingsGuiSettings waveWaveSettings = new WaveWaveSettingsGuiSettings();

    @NewLine
    public WaveProfileSettingsGuiSettings waveProfileSettings = new WaveProfileSettingsGuiSettings();

    @NewLine
    public MobSettingsGuiSettings mobSettings = new MobSettingsGuiSettings();

    @NewLine
    public MobOverrideGuiSettings mobOverride = new MobOverrideGuiSettings();

    @NewLine
    public MobEffectsGuiSettings mobEffects = new MobEffectsGuiSettings();

    @NewLine
    public LootHubGuiSettings lootHub = new LootHubGuiSettings();

    @NewLine
    public LootPoolGuiSettings lootPool = new LootPoolGuiSettings();

    @NewLine
    public ObfuscationItemsGuiSettings obfuscationItems = new ObfuscationItemsGuiSettings();
}
