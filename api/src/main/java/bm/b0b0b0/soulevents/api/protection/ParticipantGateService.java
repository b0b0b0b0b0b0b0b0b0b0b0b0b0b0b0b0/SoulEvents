package bm.b0b0b0.soulevents.api.protection;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface ParticipantGateService {

    GateResult check(Player player, UUID sessionId, GateContext context, String profileId);

    void reload();
}
