package bm.b0b0b0.soulevents.airdrop.util;

import bm.b0b0b0.soulevents.airdrop.config.settings.RequiredLootSettings;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public final class RequiredItemMatcher {

    private RequiredItemMatcher() {
    }

    public static boolean hasRequiredItem(Player player, RequiredLootSettings settings) {
        if (!settings.enabled) {
            return true;
        }
        List<ItemStack> templates = SerializedItemStackCodec.decodeAll(settings.requiredItemsBase64);
        if (templates.isEmpty()) {
            return false;
        }
        List<ItemStack> carried = collectCarriedItems(player);
        if (settings.isAnyMatch()) {
            return matchesAny(templates, carried);
        }
        return matchesAll(templates, carried);
    }

    private static List<ItemStack> collectCarriedItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> carried = new ArrayList<>();
        appendStacks(carried, inventory.getStorageContents());
        appendStacks(carried, inventory.getArmorContents());
        appendStack(carried, inventory.getItemInOffHand());
        return carried;
    }

    private static void appendStacks(List<ItemStack> carried, ItemStack[] stacks) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            appendStack(carried, stack);
        }
    }

    private static void appendStack(List<ItemStack> carried, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        carried.add(stack);
    }

    private static boolean matchesAny(List<ItemStack> templates, List<ItemStack> carried) {
        for (ItemStack template : templates) {
            int need = Math.max(1, template.getAmount());
            int found = 0;
            for (ItemStack stack : carried) {
                if (stack.isSimilar(template)) {
                    found += stack.getAmount();
                    if (found >= need) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesAll(List<ItemStack> templates, List<ItemStack> carried) {
        int[] remaining = new int[carried.size()];
        for (int index = 0; index < carried.size(); index++) {
            remaining[index] = carried.get(index).getAmount();
        }
        for (ItemStack template : templates) {
            int need = Math.max(1, template.getAmount());
            for (int index = 0; index < carried.size() && need > 0; index++) {
                if (remaining[index] <= 0) {
                    continue;
                }
                if (carried.get(index).isSimilar(template)) {
                    int take = Math.min(remaining[index], need);
                    remaining[index] -= take;
                    need -= take;
                }
            }
            if (need > 0) {
                return false;
            }
        }
        return true;
    }
}
