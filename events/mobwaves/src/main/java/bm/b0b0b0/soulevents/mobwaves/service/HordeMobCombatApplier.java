package bm.b0b0b0.soulevents.mobwaves.service;



import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeMobCombatSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobPotionEffectSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.util.MobPotionEffectSupport;

import org.bukkit.Material;

import org.bukkit.enchantments.Enchantment;

import org.bukkit.entity.EntityType;

import org.bukkit.entity.LivingEntity;

import org.bukkit.entity.Mob;

import org.bukkit.entity.Monster;

import org.bukkit.attribute.Attribute;

import org.bukkit.inventory.EntityEquipment;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class HordeMobCombatApplier {

    public static final double MAX_LIVING_HEALTH = 1024.0;

    private HordeMobCombatApplier() {

    }



    public static void apply(
            LivingEntity living,
            MobTypeOverrideSettings typeOverride,
            HordeMobCombatSettings combat
    ) {
        apply(living, typeOverride, combat, MobCombatContext.normal());
    }

    public static void apply(
            LivingEntity living,
            MobTypeOverrideSettings typeOverride,
            HordeMobCombatSettings combat,
            MobCombatContext context
    ) {
        apply(living, typeOverride, combat, context, null);
    }

    public static void apply(
            LivingEntity living,
            MobTypeOverrideSettings typeOverride,
            HordeMobCombatSettings combat,
            MobCombatContext context,
            List<MobPotionEffectSettings> potionEffects
    ) {
        activateMobAi(living);
        applyHealth(living, typeOverride, combat, context);
        applySpeed(living, typeOverride, combat);
        applyDamage(living, typeOverride, combat, context);
        applyFollowRange(living, combat);
        applyArmorAttributes(living, combat);
        applyKnockbackResistance(living, combat);
        applySunImmunity(living, combat);
        equipArmorSet(living, combat);
        equipWeapon(living, combat, context);
        applyPotionEffects(living, potionEffects);
        living.setFireTicks(0);
    }



    private static void activateMobAi(LivingEntity living) {

        if (living instanceof Mob mob) {

            mob.setAware(true);

            mob.setRemoveWhenFarAway(false);

        }

    }



    private static void applyHealth(
            LivingEntity living,
            MobTypeOverrideSettings typeOverride,
            HordeMobCombatSettings combat,
            MobCombatContext context
    ) {
        var attribute = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        double maxHealth;
        if (context.entryMaxHealth() > 0.0) {
            maxHealth = context.entryMaxHealth();
        } else if (typeOverride != null && typeOverride.maxHealth > 0.0) {
            maxHealth = typeOverride.maxHealth;
        } else {
            maxHealth = attribute.getBaseValue() * Math.max(1.0, combat.healthMultiplier);
        }
        if (context.superBoss()) {
            maxHealth *= Math.max(1.0, combat.superBossHealthMultiplier);
        }
        maxHealth = Math.max(1.0, Math.min(MAX_LIVING_HEALTH, maxHealth));
        attribute.setBaseValue(maxHealth);
        living.setHealth(maxHealth);
    }



    private static void applySpeed(

            LivingEntity living,

            MobTypeOverrideSettings typeOverride,

            HordeMobCombatSettings combat

    ) {

        double multiplier = resolveMultiplier(typeOverride, combat.speedMultiplier, true);

        if (Math.abs(multiplier - 1.0) <= 0.001) {

            return;

        }

        var attribute = living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

        if (attribute != null) {

            attribute.setBaseValue(attribute.getBaseValue() * multiplier);

        }

    }



    private static void applyDamage(
            LivingEntity living,
            MobTypeOverrideSettings typeOverride,
            HordeMobCombatSettings combat,
            MobCombatContext context
    ) {
        double multiplier = resolveMultiplier(typeOverride, combat.damageMultiplier, false);
        if (context.superBoss()) {
            multiplier *= 1.25;
        }
        if (Math.abs(multiplier - 1.0) <= 0.001) {
            return;
        }
        var attribute = living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attribute == null) {
            return;
        }
        double base = attribute.getBaseValue();
        if (base < 1.0) {
            base = 3.0;
        }
        attribute.setBaseValue(base * multiplier);
    }



    private static void applyFollowRange(LivingEntity living, HordeMobCombatSettings combat) {

        if (combat.followRangeBonus <= 0.0) {

            return;

        }

        var attribute = living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);

        if (attribute != null) {

            attribute.setBaseValue(Math.max(attribute.getBaseValue(), 16.0 + combat.followRangeBonus));

        }

    }



    private static void applyPotionEffects(LivingEntity living, List<MobPotionEffectSettings> effects) {
        MobPotionEffectSupport.apply(living, effects);
    }



    private static double resolveMultiplier(

            MobTypeOverrideSettings typeOverride,

            double moduleDefault,

            boolean speedField

    ) {

        if (typeOverride == null) {

            return Math.max(0.0, moduleDefault);

        }

        double profileValue = speedField ? typeOverride.speedMultiplier : typeOverride.damageMultiplier;

        if (profileValue > 0.0 && Math.abs(profileValue - 1.0) > 0.001) {

            return profileValue;

        }

        return Math.max(0.0, moduleDefault);

    }



    private static void applyArmorAttributes(LivingEntity living, HordeMobCombatSettings combat) {

        if (combat.armorBonus > 0.0) {

            var armor = living.getAttribute(Attribute.GENERIC_ARMOR);

            if (armor != null) {

                armor.setBaseValue(armor.getBaseValue() + combat.armorBonus);

            }

        }

        if (combat.armorToughnessBonus > 0.0) {

            var toughness = living.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);

            if (toughness != null) {

                toughness.setBaseValue(toughness.getBaseValue() + combat.armorToughnessBonus);

            }

        }

    }



    private static void applyKnockbackResistance(LivingEntity living, HordeMobCombatSettings combat) {

        if (combat.knockbackResistance <= 0.0) {

            return;

        }

        var attribute = living.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);

        if (attribute != null) {

            attribute.setBaseValue(Math.max(attribute.getBaseValue(), combat.knockbackResistance));

        }

    }



    private static void applySunImmunity(LivingEntity living, HordeMobCombatSettings combat) {

        if (!combat.immuneToSunlight) {

            return;

        }

        living.setFireTicks(0);

    }



    private static void equipArmorSet(LivingEntity living, HordeMobCombatSettings combat) {

        if (!combat.equipArmor || !(living instanceof Mob mob)) {

            return;

        }

        Material base = Material.matchMaterial(combat.armorMaterial);

        if (base == null) {

            return;

        }

        String prefix = base.name();

        Material helmet = Material.matchMaterial(prefix + "_HELMET");

        Material chest = Material.matchMaterial(prefix + "_CHESTPLATE");

        Material legs = Material.matchMaterial(prefix + "_LEGGINGS");

        Material boots = Material.matchMaterial(prefix + "_BOOTS");

        EntityEquipment equipment = mob.getEquipment();

        if (equipment == null) {

            return;

        }

        if (helmet != null) {

            equipment.setHelmet(new ItemStack(helmet));

            equipment.setHelmetDropChance(0.0f);

        }

        if (chest != null) {

            equipment.setChestplate(new ItemStack(chest));

            equipment.setChestplateDropChance(0.0f);

        }

        if (legs != null) {

            equipment.setLeggings(new ItemStack(legs));

            equipment.setLeggingsDropChance(0.0f);

        }

        if (boots != null) {

            equipment.setBoots(new ItemStack(boots));

            equipment.setBootsDropChance(0.0f);

        }

    }



    private static void equipWeapon(LivingEntity living, HordeMobCombatSettings combat, MobCombatContext context) {
        if (!combat.equipWeapons || !(living instanceof Mob mob)) {
            return;
        }
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }
        int sharpness = context.superBoss() ? 7 : 5;
        int power = context.superBoss() ? 7 : 5;
        EntityType type = living.getType();
        if (type == EntityType.SKELETON || type == EntityType.STRAY) {
            ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(Enchantment.POWER, power);
            equipment.setItemInMainHand(bow);
            equipment.setItemInMainHandDropChance(0.0f);
            return;
        }
        if (living instanceof Monster) {
            Material weapon = context.superBoss() ? Material.NETHERITE_AXE : Material.NETHERITE_SWORD;
            ItemStack stack = new ItemStack(weapon);
            stack.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpness);
            equipment.setItemInMainHand(stack);
            equipment.setItemInMainHandDropChance(0.0f);
        }
    }

}

