package bm.b0b0b0.soulevents.mobwaves.service;

public record MobCombatContext(boolean superBoss, double entryMaxHealth) {

    public static MobCombatContext normal() {
        return new MobCombatContext(false, 0.0);
    }

    public static MobCombatContext superBoss(double entryMaxHealth) {
        return new MobCombatContext(true, entryMaxHealth);
    }
}
