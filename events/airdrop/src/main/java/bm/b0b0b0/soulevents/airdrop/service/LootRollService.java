package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.config.settings.LootEntrySettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.airdrop.util.SerializedItemStackCodec;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootRollService {

    public List<ItemStack> roll(LootTableSettings loot, int chestSize) {
        List<ItemStack> pool = SerializedItemStackCodec.decodeAll(loot.poolItemsBase64);
        if (!pool.isEmpty()) {
            return rollFromPool(loot, pool, chestSize);
        }
        List<ItemStack> items = new ArrayList<>();
        rollPass(loot, items);
        int minimum = Math.max(0, loot.minItemStacks);
        int guard = 0;
        while (items.size() < minimum && guard++ < 12) {
            rollPass(loot, items);
        }
        return List.copyOf(items);
    }

    public List<Integer> randomChestSlots(int chestSize, int itemCount) {
        if (itemCount <= 0 || chestSize <= 0) {
            return List.of();
        }
        List<Integer> slots = new ArrayList<>(chestSize);
        for (int slot = 0; slot < chestSize; slot++) {
            slots.add(slot);
        }
        Collections.shuffle(slots, ThreadLocalRandom.current());
        int limit = Math.min(itemCount, chestSize);
        return List.copyOf(slots.subList(0, limit));
    }

    private List<ItemStack> rollFromPool(LootTableSettings loot, List<ItemStack> pool, int chestSize) {
        int target = Math.max(0, loot.occupiedSlots);
        target = Math.min(target, chestSize);
        if (target == 0) {
            return List.of();
        }
        List<ItemStack> rolled = new ArrayList<>(target);
        for (int index = 0; index < target; index++) {
            ItemStack template = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            rolled.add(template.clone());
        }
        return List.copyOf(rolled);
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
