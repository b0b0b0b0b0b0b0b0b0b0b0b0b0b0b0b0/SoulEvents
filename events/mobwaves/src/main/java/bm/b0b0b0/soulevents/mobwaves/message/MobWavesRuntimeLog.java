package bm.b0b0b0.soulevents.mobwaves.message;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import org.bukkit.plugin.Plugin;

public final class MobWavesRuntimeLog {

    private MobWavesRuntimeLog() {
    }

    public static void wave(Plugin plugin, MobWavesPluginConfig config, String message) {
        plugin.getLogger().info("[MobWaves-Wave] " + message);
    }

    public static void horde(Plugin plugin, MobWavesPluginConfig config, String message) {
        plugin.getLogger().info("[MobWaves-Horde] " + message);
    }

    public static void warn(Plugin plugin, String message) {
        plugin.getLogger().warning("[MobWaves] " + message);
    }

    public static void spawnVerbose(Plugin plugin, MobWavesPluginConfig config, String message) {
        if (config == null || !config.module().spawnDebugEnabled) {
            return;
        }
        plugin.getLogger().info("[MobWaves-Spawn] " + message);
    }

    public static void combat(Plugin plugin, MobWavesPluginConfig config, String message) {
        if (config == null || !config.module().combatDebugEnabled) {
            return;
        }
        plugin.getLogger().info("[MobWaves-Combat] " + message);
    }
}
