package bm.b0b0b0.soulevents.api.protection;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.UUID;

public interface EffectResolverService {

    List<PotionEffect> resolve(UUID sessionId, Player player, List<PotionEffect> baseEffects);

    boolean isImmune(UUID sessionId, Player player, PotionEffect effect);

    boolean isAmplified(UUID sessionId, Player player, PotionEffect effect);

    void reload();
}
