package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeLootVisualSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeMobLootSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.util.SerializedItemStackCodec;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MobLootDropService {

    private final Plugin plugin;
    private final MobWaveMessageService messages;

    public MobLootDropService(Plugin plugin, MobWaveMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public MobHordeSessionRegistry.LootItem drop(
            UUID sessionId,
            HordeTypeDefinition definition,
            Location origin,
            ItemStack obfuscated,
            int slotIndex,
            MobHordeSessionRegistry registry
    ) {
        World world = origin.getWorld();
        if (world == null || obfuscated == null || obfuscated.getType().isAir()) {
            return null;
        }
        HordeMobLootSettings mobLoot = definition.settings().mobLoot;
        Location spawn = origin.clone().add(0.0, 0.35, 0.0);
        Item entity = world.dropItem(spawn, obfuscated);
        entity.setPickupDelay(Math.max(0, mobLoot.pickupDelayTicks));
        entity.setCanMobPickup(false);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        entity.setVelocity(new Vector(
                (random.nextDouble() - 0.5) * mobLoot.dropVelocityHorizontal,
                mobLoot.dropVelocityVertical,
                (random.nextDouble() - 0.5) * mobLoot.dropVelocityHorizontal
        ));
        TextDisplay label = spawnLabel(world, entity.getLocation(), definition);
        MobHordeSessionRegistry.LootItem lootItem = new MobHordeSessionRegistry.LootItem(
                entity.getUniqueId(),
                label == null ? null : label.getUniqueId(),
                slotIndex,
                Bukkit.getCurrentTick() + mobLoot.pickupDelayTicks
        );
        registry.addLootItem(sessionId, lootItem);
        return lootItem;
    }

    public ItemStack pickObfuscationMask(LootTableSettings loot) {
        List<ItemStack> masks = SerializedItemStackCodec.decodeAll(loot.obfuscationItemsBase64);
        if (masks.isEmpty()) {
            return new ItemStack(Material.COAL);
        }
        return masks.get(ThreadLocalRandom.current().nextInt(masks.size())).clone();
    }

    public void removeLabel(World world, UUID labelId) {
        if (world == null || labelId == null) {
            return;
        }
        var entity = Bukkit.getEntity(labelId);
        if (entity != null) {
            entity.remove();
        }
    }

    private TextDisplay spawnLabel(World world, Location anchor, HordeTypeDefinition definition) {
        HordeLootVisualSettings visual = definition.settings().lootVisual;
        if (!visual.itemLabelEnabled || visual.itemLabelKeys.isEmpty()) {
            return null;
        }
        String key = visual.itemLabelKeys.get(ThreadLocalRandom.current().nextInt(visual.itemLabelKeys.size()));
        Component text = messages.resolve(key, Map.of(
                "type_name", messages.resolvePlain(definition.settings().displayNameKey, Map.of())
        ));
        Location labelLocation = anchor.clone().add(0, visual.itemLabelOffsetY, 0);
        return world.spawn(labelLocation, TextDisplay.class, display -> {
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(visual.itemLabelSeeThrough);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setPersistent(false);
        });
    }
}
