package bm.b0b0b0.soulevents.mobwaves.util;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.IntFunction;

public final class SerializedItemStackCodec {

    private SerializedItemStackCodec() {
    }

    public static String encode(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static List<ItemStack> decodeAll(List<String> encodedItems) {
        List<ItemStack> items = new ArrayList<>();
        if (encodedItems == null) {
            return items;
        }
        for (String encoded : encodedItems) {
            ItemStack item = decode(encoded);
            if (item != null && !item.getType().isAir()) {
                items.add(item);
            }
        }
        return items;
    }

    public static List<String> encodeAll(Iterable<ItemStack> items) {
        List<String> encoded = new ArrayList<>();
        for (ItemStack item : items) {
            String value = encode(item);
            if (value != null) {
                encoded.add(value);
            }
        }
        return encoded;
    }

    public static int countEncodedEntries(List<String> encodedItems) {
        if (encodedItems == null) {
            return 0;
        }
        int count = 0;
        for (String encoded : encodedItems) {
            if (encoded == null || encoded.isBlank()) {
                continue;
            }
            ItemStack item = decode(encoded);
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    public static List<String> encodeSlotItems(int slotCount, IntFunction<ItemStack> itemAt) {
        List<String> encoded = new ArrayList<>(slotCount);
        for (int slot = 0; slot < slotCount; slot++) {
            String value = encode(itemAt.apply(slot));
            encoded.add(value == null ? "" : value);
        }
        return encoded;
    }

    public static void decodeIntoSlots(List<String> encoded, int slotCount, SlotConsumer consumer) {
        decodeIntoSlots(encoded, 0, slotCount, consumer);
    }

    public static void decodeIntoSlots(List<String> encoded, int offset, int slotCount, SlotConsumer consumer) {
        if (encoded == null) {
            return;
        }
        for (int slot = 0; slot < slotCount; slot++) {
            int index = offset + slot;
            if (index >= encoded.size()) {
                return;
            }
            ItemStack stack = decode(encoded.get(index));
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            consumer.accept(slot, stack.clone());
        }
    }

    public static void trimTrailingEmptyEntries(List<String> encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return;
        }
        int lastNonEmpty = findLastNonEmptyIndex(encoded);
        if (lastNonEmpty < 0) {
            encoded.clear();
            return;
        }
        encoded.subList(lastNonEmpty + 1, encoded.size()).clear();
    }

    public static void trimTrailingPoolPages(List<String> encoded, int pageSize) {
        if (encoded == null || encoded.isEmpty() || pageSize <= 0) {
            return;
        }
        int lastNonEmpty = findLastNonEmptyIndex(encoded);
        if (lastNonEmpty < 0) {
            encoded.clear();
            return;
        }
        int keepUntil = ((lastNonEmpty / pageSize) + 1) * pageSize;
        if (keepUntil < encoded.size()) {
            encoded.subList(keepUntil, encoded.size()).clear();
        }
    }

    private static int findLastNonEmptyIndex(List<String> encoded) {
        for (int index = encoded.size() - 1; index >= 0; index--) {
            String value = encoded.get(index);
            if (value == null || value.isBlank()) {
                continue;
            }
            ItemStack item = decode(value);
            if (item != null && !item.getType().isAir()) {
                return index;
            }
        }
        return -1;
    }

    @FunctionalInterface
    public interface SlotConsumer {
        void accept(int slot, ItemStack item);
    }
}

