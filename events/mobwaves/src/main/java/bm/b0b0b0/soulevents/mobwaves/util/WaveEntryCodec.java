package bm.b0b0b0.soulevents.mobwaves.util;

import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveMobEntrySettings;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WaveEntryCodec {

    private WaveEntryCodec() {
    }

    public static List<WaveMobEntrySettings> readInventory(Inventory inventory, int editableSlots) {
        Map<EntityType, Integer> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < editableSlots && slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            MobWaveEntitySupport.fromSpawnEgg(stack.getType()).ifPresent(type ->
                    counts.merge(type, stack.getAmount(), Integer::sum)
            );
        }
        List<WaveMobEntrySettings> entries = new ArrayList<>();
        for (Map.Entry<EntityType, Integer> entry : counts.entrySet()) {
            WaveMobEntrySettings settings = new WaveMobEntrySettings();
            settings.entityType = entry.getKey().name();
            settings.count = entry.getValue();
            entries.add(settings);
        }
        return entries;
    }

    public static void writeInventory(Inventory inventory, int editableSlots, List<WaveMobEntrySettings> entries) {
        for (int slot = 0; slot < editableSlots && slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, null);
        }
        int slot = 0;
        for (WaveMobEntrySettings entry : entries) {
            EntityType type = MobWaveEntitySupport.resolveEntityType(entry.entityType);
            if (type == null || entry.count <= 0) {
                continue;
            }
            Material egg = MobWaveEntitySupport.spawnEgg(type);
            int remaining = entry.count;
            while (remaining > 0 && slot < editableSlots) {
                int stackSize = Math.min(64, remaining);
                inventory.setItem(slot++, new ItemStack(egg, stackSize));
                remaining -= stackSize;
            }
        }
    }
}
