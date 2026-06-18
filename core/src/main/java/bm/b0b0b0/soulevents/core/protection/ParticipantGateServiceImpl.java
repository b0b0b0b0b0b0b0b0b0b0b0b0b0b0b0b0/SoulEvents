package bm.b0b0b0.soulevents.core.protection;

import bm.b0b0b0.soulevents.api.protection.GateContext;
import bm.b0b0b0.soulevents.api.protection.GateResult;
import bm.b0b0b0.soulevents.api.protection.ParticipantGateService;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import bm.b0b0b0.soulevents.core.config.settings.GateProfileSettings;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public final class ParticipantGateServiceImpl implements ParticipantGateService {

    private Map<String, GateProfileSettings> profiles;

    public ParticipantGateServiceImpl(PluginConfig config) {
        this.profiles = config.protection().gateProfiles;
    }

    @Override
    public GateResult check(Player player, UUID sessionId, GateContext context, String profileId) {
        GateProfileSettings profile = profiles.get(profileId);
        if (profile == null) {
            profile = profiles.get("default");
        }
        if (profile == null) {
            return GateResult.allow();
        }
        if (profile.denyInvisible && player.isInvisible()) {
            return GateResult.deny("protection.gate.invisible");
        }
        if (profile.denyFlying && (player.isFlying() || player.getAllowFlight() && player.isGliding())) {
            return GateResult.deny("protection.gate.flying");
        }
        if (profile.requireHelmet && isEmpty(player.getInventory().getHelmet())) {
            return GateResult.deny("protection.gate.no-helmet");
        }
        if (profile.requireChestplate && isEmpty(player.getInventory().getChestplate())) {
            return GateResult.deny("protection.gate.no-chestplate");
        }
        if (profile.requireLeggings && isEmpty(player.getInventory().getLeggings())) {
            return GateResult.deny("protection.gate.no-leggings");
        }
        if (profile.requireBoots && isEmpty(player.getInventory().getBoots())) {
            return GateResult.deny("protection.gate.no-boots");
        }
        GateResult held = checkItems(player.getInventory().getItemInMainHand(), profile);
        if (!held.allowed()) {
            return held;
        }
        return GateResult.allow();
    }

    @Override
    public void reload() {
    }

    public void reload(PluginConfig config) {
        this.profiles = config.protection().gateProfiles;
    }

    private GateResult checkItems(ItemStack held, GateProfileSettings profile) {
        for (String encoded : profile.forbiddenHeldItemsBase64) {
            ItemStack forbidden = decode(encoded);
            if (forbidden != null && held.isSimilar(forbidden)) {
                return GateResult.deny("protection.gate.forbidden-item");
            }
        }
        if (profile.requiredHeldItemsBase64.isEmpty()) {
            return GateResult.allow();
        }
        for (String encoded : profile.requiredHeldItemsBase64) {
            ItemStack required = decode(encoded);
            if (required != null && held.isSimilar(required)) {
                return GateResult.allow();
            }
        }
        return GateResult.deny("protection.gate.missing-item");
    }

    private static ItemStack decode(String encoded) {
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            return ItemStack.deserializeBytes(data);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }
}
