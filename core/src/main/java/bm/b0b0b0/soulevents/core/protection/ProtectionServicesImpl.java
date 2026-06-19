package bm.b0b0b0.soulevents.core.protection;

import bm.b0b0b0.soulevents.api.protection.ArenaGuardService;
import bm.b0b0b0.soulevents.api.protection.EffectResolverService;
import bm.b0b0b0.soulevents.api.protection.LootGuardService;
import bm.b0b0b0.soulevents.api.protection.ParticipantGateService;
import bm.b0b0b0.soulevents.api.protection.ProtectionServices;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import org.bukkit.plugin.Plugin;

public final class ProtectionServicesImpl implements ProtectionServices {

    private final ParticipantGateServiceImpl gates;
    private final LootGuardServiceImpl loot;
    private final ArenaGuardServiceImpl arena;
    private final EffectResolverServiceImpl effects;

    public ProtectionServicesImpl(Plugin plugin, PluginConfig config) {
        this.gates = new ParticipantGateServiceImpl(config);
        this.loot = new LootGuardServiceImpl(plugin, config);
        this.arena = new ArenaGuardServiceImpl(config);
        this.effects = new EffectResolverServiceImpl(config);
    }

    @Override
    public ParticipantGateService gates() {
        return gates;
    }

    @Override
    public LootGuardService loot() {
        return loot;
    }

    @Override
    public ArenaGuardService arena() {
        return arena;
    }

    @Override
    public EffectResolverService effects() {
        return effects;
    }

    public void reload(PluginConfig config) {
        loot.shutdownPending();
        gates.reload(config);
        loot.reload(config);
        arena.reload(config);
        effects.reload(config);
    }

    public void shutdown() {
        loot.shutdown();
    }
}
