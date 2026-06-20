package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.settings.LootEntrySettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.mobwaves.util.SerializedItemStackCodec;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootRollService {

    public List<ItemStack> rollForEruption(LootTableSettings loot, int itemCount) {
        if (itemCount <= 0) {
            return List.of();
        }
        List<ItemStack> pool = SerializedItemStackCodec.decodeAll(loot.poolItemsBase64);
        if (!pool.isEmpty()) {
            return rollFromPool(loot, pool, itemCount);
        }
        List<ItemStack> items = new ArrayList<>();
        int guard = 0;
        while (items.size() < itemCount && guard++ < itemCount * 8) {
            rollPass(loot, items);
        }
        if (items.size() > itemCount) {
            Collections.shuffle(items, ThreadLocalRandom.current());
            return List.copyOf(items.subList(0, itemCount));
        }
        return List.copyOf(items);
    }

    private List<ItemStack> rollFromPool(LootTableSettings loot, List<ItemStack> pool, int itemCount) {
        int target = Math.max(1, loot.occupiedSlots > 0 ? loot.occupiedSlots : itemCount);
        target = Math.min(target, itemCount);
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
