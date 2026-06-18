package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.HashMap;
import java.util.Map;

public class ProtectionSettings extends YamlSerializable {

    @Comment(@CommentValue("Задержка между взятием предметов из ивент-сундука (мс, на игрока)."))
    public long lootTakeCooldownMillis = 250L;

    @Comment(@CommentValue("Через сколько мс «уголь» превращается в настоящий предмет (~50 = 1 тик, ~100 = 2 тика)."))
    public long lootRevealDelayMillis = 100L;

    @Comment(@CommentValue("Дебаг LootGuard в консоль: obfuscate, take, reveal, ошибки."))
    public boolean lootDebugEnabled = true;

    @Comment(@CommentValue("Запретить лить жидкости на арене ивента."))
    public boolean blockFluidGrief = true;

    @Comment(@CommentValue("Запретить ломать/ставить блоки на арене ивента."))
    public boolean blockBlockGrief = true;

    @Comment(@CommentValue("Именованные gate-профили для модулей ивентов."))
    public Map<String, GateProfileSettings> gateProfiles = defaultGateProfiles();

    private static Map<String, GateProfileSettings> defaultGateProfiles() {
        Map<String, GateProfileSettings> profiles = new HashMap<>();
        profiles.put("default", new GateProfileSettings());
        return profiles;
    }
}
