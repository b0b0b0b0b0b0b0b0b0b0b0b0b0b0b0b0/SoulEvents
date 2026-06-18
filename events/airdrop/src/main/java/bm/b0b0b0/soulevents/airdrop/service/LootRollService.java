package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.config.settings.LootEntrySettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootRollService {

    public List<ItemStack> roll(LootTableSettings loot) {
        List<ItemStack> items = new ArrayList<>();
        rollPass(loot, items);
        int minimum = Math.max(0, loot.minItemStacks);
        int guard = 0;
        while (items.size() < minimum && guard++ < 12) {
            rollPass(loot, items);
        }
        return List.copyOf(items);
    }

    private void rollPass(LootTableSettings loot, List<ItemStack> items) {
        for (LootEntrySettings entry : loot.entries) {
            if (ThreadLocalRandom.current().nextDouble() > entry.chance) {
                continue;
            }
            ItemStack stack = decodeEntry(entry);
            if (stack != null && !stack.isEmpty()) {
                items.add(stack);
            }
        }
        for (String encoded : loot.extraItemsBase64) {
            ItemStack stack = decodeBase64(encoded);
            if (stack != null && !stack.isEmpty()) {
                items.add(stack);
            }
        }
    }

    private ItemStack decodeEntry(LootEntrySettings entry) {
        if (entry.itemBase64 != null && !entry.itemBase64.isEmpty()) {
            return decodeBase64(entry.itemBase64);
        }
        Material material = Material.matchMaterial(entry.material);
        if (material == null || material.isAir()) {
            return null;
        }
        int min = Math.max(1, entry.minAmount);
        int max = Math.max(min, entry.maxAmount);
        int amount = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        return new ItemStack(material, amount);
    }

    private ItemStack decodeBase64(String encoded) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
