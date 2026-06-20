package bm.b0b0b0.soulevents.mobwaves.config.settings;

public final class LootEntrySettings {

    public String material = "IRON_INGOT";
    public String itemBase64 = "";
    public int minAmount = 1;
    public int maxAmount = 1;
    public double chance = 1.0;

    public static LootEntrySettings of(String material, int min, int max, double chance) {
        LootEntrySettings entry = new LootEntrySettings();
        entry.material = material;
        entry.minAmount = min;
        entry.maxAmount = max;
        entry.chance = chance;
        return entry;
    }
}
