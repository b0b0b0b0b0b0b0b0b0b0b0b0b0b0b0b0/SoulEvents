package bm.b0b0b0.soulevents.mobwaves.service;



import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;

import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntityTags;

import net.kyori.adventure.text.Component;

import org.bukkit.FluidCollisionMode;

import org.bukkit.Location;

import org.bukkit.attribute.Attribute;

import org.bukkit.entity.Display;

import org.bukkit.entity.Entity;

import org.bukkit.entity.LivingEntity;

import org.bukkit.entity.Player;

import org.bukkit.entity.TextDisplay;

import org.bukkit.plugin.Plugin;

import org.bukkit.scheduler.BukkitTask;

import org.bukkit.util.RayTraceResult;

import org.bukkit.util.Vector;



import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;



public final class MobHealthBarService {



    private final Plugin plugin;

    private final MobWaveMessageService messages;

    private final Map<UUID, UUID> mobToBarDisplay = new HashMap<>();
    private final Map<UUID, UUID> mobToValueDisplay = new HashMap<>();
    private final Map<UUID, UUID> mobToWaveDisplay = new HashMap<>();

    private BukkitTask tickTask;



    public MobHealthBarService(Plugin plugin, MobWaveMessageService messages) {

        this.plugin = plugin;

        this.messages = messages;

    }



    public void start() {

        if (tickTask != null) {

            return;

        }

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);

    }



    public void stop() {

        if (tickTask != null) {

            tickTask.cancel();

            tickTask = null;

        }

        for (UUID displayId : mobToBarDisplay.values()) {

            removeDisplay(displayId);

        }

        for (UUID displayId : mobToValueDisplay.values()) {
            removeDisplay(displayId);
        }
        for (UUID displayId : mobToWaveDisplay.values()) {
            removeDisplay(displayId);
        }
        mobToBarDisplay.clear();
        mobToValueDisplay.clear();
        mobToWaveDisplay.clear();

    }



    public void attach(LivingEntity mob) {

        if (mob == null || mob.isDead()) {

            return;

        }

        remove(mob.getUniqueId());

        UUID mobId = mob.getUniqueId();
        DisplayOffsets offsets = DisplayOffsets.forMob(plugin, mob);
        Location barAnchor = mob.getLocation().add(0.0, offsets.barY(), 0.0);
        Location valueAnchor = mob.getLocation().add(0.0, offsets.valueY(), 0.0);

        TextDisplay barDisplay = mob.getWorld().spawn(barAnchor, TextDisplay.class, spawned -> {

            spawned.text(renderBar(mob));

            spawned.setBillboard(Display.Billboard.CENTER);

            spawned.setSeeThrough(false);

            spawned.setShadowed(true);

            spawned.setDefaultBackground(false);

            spawned.setPersistent(false);

            spawned.setLineWidth(120);

        });

        TextDisplay valueDisplay = mob.getWorld().spawn(valueAnchor, TextDisplay.class, spawned -> {

            spawned.text(renderValue(mob));

            spawned.setBillboard(Display.Billboard.CENTER);

            spawned.setSeeThrough(false);

            spawned.setShadowed(true);

            spawned.setDefaultBackground(false);

            spawned.setPersistent(false);

            spawned.setLineWidth(120);

        });

        TextDisplay waveDisplay = null;
        if (offsets.showWave()) {
            Location waveAnchor = mob.getLocation().add(0.0, offsets.waveY(), 0.0);
            waveDisplay = mob.getWorld().spawn(waveAnchor, TextDisplay.class, spawned -> {
                spawned.text(renderWave(mob));
                spawned.setBillboard(Display.Billboard.CENTER);
                spawned.setSeeThrough(false);
                spawned.setShadowed(true);
                spawned.setDefaultBackground(false);
                spawned.setPersistent(false);
                spawned.setLineWidth(120);
            });
            MobWaveEntityTags.tagDisplay(plugin, waveDisplay, mobId);
            mobToWaveDisplay.put(mobId, waveDisplay.getUniqueId());
        }

        MobWaveEntityTags.tagDisplay(plugin, barDisplay, mobId);

        MobWaveEntityTags.tagDisplay(plugin, valueDisplay, mobId);

        mobToBarDisplay.put(mobId, barDisplay.getUniqueId());

        mobToValueDisplay.put(mobId, valueDisplay.getUniqueId());

    }



    public void remove(UUID mobId) {

        UUID barId = mobToBarDisplay.remove(mobId);

        if (barId != null) {

            removeDisplay(barId);

        }

        UUID valueId = mobToValueDisplay.remove(mobId);
        if (valueId != null) {
            removeDisplay(valueId);
        }
        UUID waveId = mobToWaveDisplay.remove(mobId);
        if (waveId != null) {
            removeDisplay(waveId);
        }
    }



    private void removeDisplay(UUID displayId) {

        Entity display = plugin.getServer().getEntity(displayId);

        if (display != null) {

            display.remove();

        }

    }



    private void tick() {

        mobToBarDisplay.entrySet().removeIf(entry -> {

            UUID mobId = entry.getKey();

            Entity mobEntity = plugin.getServer().getEntity(mobId);

            if (!(mobEntity instanceof LivingEntity living) || living.isDead() || !living.isValid()) {

                UUID valueId = mobToValueDisplay.remove(mobId);
                UUID waveId = mobToWaveDisplay.remove(mobId);
                removeDisplay(entry.getValue());
                if (valueId != null) {
                    removeDisplay(valueId);
                }
                if (waveId != null) {
                    removeDisplay(waveId);
                }
                return true;
            }

            Entity barEntity = plugin.getServer().getEntity(entry.getValue());
            Entity valueEntity = plugin.getServer().getEntity(mobToValueDisplay.get(mobId));
            Entity waveEntity = Optional.ofNullable(mobToWaveDisplay.get(mobId))
                    .map(id -> plugin.getServer().getEntity(id))
                    .orElse(null);

            if (!(barEntity instanceof TextDisplay barDisplay)) {

                if (valueEntity != null) {

                    valueEntity.remove();

                }

                mobToValueDisplay.remove(mobId);

                return true;

            }

            TextDisplay valueDisplay = valueEntity instanceof TextDisplay textDisplay ? textDisplay : null;
            TextDisplay waveDisplay = waveEntity instanceof TextDisplay waveText ? waveText : null;

            DisplayOffsets offsets = DisplayOffsets.forMob(plugin, living);
            Location barAnchor = living.getLocation().add(0.0, offsets.barY(), 0.0);
            Location valueAnchor = living.getLocation().add(0.0, offsets.valueY(), 0.0);
            Location waveAnchor = offsets.showWave()
                    ? living.getLocation().add(0.0, offsets.waveY(), 0.0)
                    : null;

            if (barDisplay.getLocation().distanceSquared(barAnchor) > 0.01) {

                barDisplay.teleport(barAnchor);

            }

            if (valueDisplay != null && valueDisplay.getLocation().distanceSquared(valueAnchor) > 0.01) {
                valueDisplay.teleport(valueAnchor);
            }
            if (waveDisplay != null && waveAnchor != null && waveDisplay.getLocation().distanceSquared(waveAnchor) > 0.01) {
                waveDisplay.teleport(waveAnchor);
            }

            barDisplay.text(renderBar(living));
            if (valueDisplay != null) {
                valueDisplay.text(renderValue(living));
                updateVisibility(living, valueDisplay);
            }
            if (waveDisplay != null) {
                waveDisplay.text(renderWave(living));
                updateVisibility(living, waveDisplay);
            }
            updateVisibility(living, barDisplay);

            return false;

        });

    }



    private void updateVisibility(LivingEntity mob, TextDisplay display) {

        for (Player player : mob.getWorld().getPlayers()) {

            if (hasLineOfSight(player, mob)) {

                player.showEntity(plugin, display);

            } else {

                player.hideEntity(plugin, display);

            }

        }

    }



    private static boolean hasLineOfSight(Player player, Entity target) {

        Location eye = player.getEyeLocation();

        Location targetEye = target.getLocation().add(0.0, target.getHeight() * 0.85, 0.0);

        Vector direction = targetEye.toVector().subtract(eye.toVector());

        double distance = direction.length();

        if (distance < 0.01) {

            return true;

        }

        direction.normalize();

        RayTraceResult result = player.getWorld().rayTraceBlocks(

                eye,

                direction,

                distance,

                FluidCollisionMode.NEVER,

                true

        );

        if (result == null || result.getHitBlock() == null) {

            return true;

        }

        return result.getHitPosition().distance(eye.toVector()) >= distance - 0.5;

    }



    private Component renderBar(LivingEntity mob) {

        double max = maxHealth(mob);

        double current = Math.max(0.0, mob.getHealth());

        int segments = 10;

        int filled = max <= 0.0 ? 0 : (int) Math.round((current / max) * segments);

        filled = Math.max(0, Math.min(segments, filled));

        StringBuilder bar = new StringBuilder();

        for (int index = 0; index < segments; index++) {

            bar.append(index < filled ? '█' : '░');

        }

        return messages.resolve(
                MobWaveEntityTags.isSuperBoss(plugin, mob) ? "mobwaves.health-bar-boss" : "mobwaves.health-bar",
                Map.of(
                        "bar", bar.toString(),
                        "wave", Integer.toString(Math.max(1, MobWaveEntityTags.waveNumber(plugin, mob)))
                )
        );

    }



    private Component renderWave(LivingEntity mob) {
        if (MobWaveEntityTags.isSuperBoss(plugin, mob)) {
            return Component.empty();
        }
        int wave = MobWaveEntityTags.waveNumber(plugin, mob);
        if (wave <= 0) {
            return Component.empty();
        }
        return messages.resolve("mobwaves.wave-badge", Map.of("wave", Integer.toString(wave)));
    }

    private record DisplayOffsets(double waveY, double barY, double valueY, boolean showWave) {

        private static DisplayOffsets forMob(Plugin plugin, LivingEntity mob) {
            double height = mob.getHeight();
            if (MobWaveEntityTags.isSuperBoss(plugin, mob)) {
                return new DisplayOffsets(0.0, height + 1.08, height + 0.68, false);
            }
            return new DisplayOffsets(
                    height + 0.95,
                    height + 0.62,
                    height + 0.28,
                    MobWaveEntityTags.waveNumber(plugin, mob) > 0
            );
        }
    }

    private Component renderValue(LivingEntity mob) {

        double max = maxHealth(mob);

        double current = Math.max(0.0, mob.getHealth());

        return messages.resolve(
                MobWaveEntityTags.isSuperBoss(plugin, mob) ? "mobwaves.health-value-boss" : "mobwaves.health-value",
                Map.of(
                "current", formatHealth(current),

                "max", formatHealth(max)

        ));

    }



    private static double maxHealth(LivingEntity mob) {

        var maxAttribute = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        return maxAttribute == null ? 20.0 : maxAttribute.getValue();

    }



    private static String formatHealth(double value) {

        return Integer.toString((int) Math.ceil(value));

    }

}

