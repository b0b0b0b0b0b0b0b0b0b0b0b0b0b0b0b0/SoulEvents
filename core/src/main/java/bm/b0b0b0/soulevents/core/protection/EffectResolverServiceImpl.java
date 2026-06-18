package bm.b0b0b0.soulevents.core.protection;

import bm.b0b0b0.soulevents.api.protection.EffectResolverService;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EffectResolverServiceImpl implements EffectResolverService {

    public EffectResolverServiceImpl(PluginConfig config) {
    }

    @Override
    public List<PotionEffect> resolve(UUID sessionId, Player player, List<PotionEffect> baseEffects) {
        List<PotionEffect> resolved = new ArrayList<>();
        for (PotionEffect effect : baseEffects) {
            if (isImmune(sessionId, player, effect)) {
                continue;
            }
            if (isAmplified(sessionId, player, effect)) {
                resolved.add(new PotionEffect(
                        effect.getType(),
                        effect.getDuration(),
                        effect.getAmplifier() + 1,
                        effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon()
                ));
                continue;
            }
            resolved.add(effect);
        }
        return resolved;
    }

    @Override
    public boolean isImmune(UUID sessionId, Player player, PotionEffect effect) {
        return false;
    }

    @Override
    public boolean isAmplified(UUID sessionId, Player player, PotionEffect effect) {
        return false;
    }

    @Override
    public void reload() {
    }

    public void reload(PluginConfig config) {
    }
}
