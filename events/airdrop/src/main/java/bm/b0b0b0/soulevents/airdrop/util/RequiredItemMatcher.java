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
            for (ItemStack stack : carried) {
                if (stack.isSimilar(template)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesAll(List<ItemStack> templates, List<ItemStack> carried) {
        boolean[] consumed = new boolean[carried.size()];
        for (ItemStack template : templates) {
            boolean matched = false;
            for (int index = 0; index < carried.size(); index++) {
                if (consumed[index]) {
                    continue;
                }
                if (carried.get(index).isSimilar(template)) {
                    consumed[index] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }
}
