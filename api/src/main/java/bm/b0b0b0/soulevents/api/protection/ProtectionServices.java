package bm.b0b0b0.soulevents.api.protection;

public interface ProtectionServices {

    ParticipantGateService gates();

    LootGuardService loot();

    ArenaGuardService arena();

    EffectResolverService effects();
}
