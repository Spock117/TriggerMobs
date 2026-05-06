package com.spock117.triggermobs.events;

import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.config.TriggerMobsConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Gates TriggerMobs loot tables used by Loot Integrations behind config flags.
 * Loot Integrations pulls items from these source tables; when a table is emptied
 * here, no items are injected for that category even though the datapack JSONs exist.
 */
@EventBusSubscriber(modid = TriggerMobs.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class LootIntegrationsLootEvents {

    private static final ResourceLocation LOOT_GUNS = ResourceLocation.fromNamespaceAndPath(TriggerMobs.MOD_ID, "loot/triggermobs_loot_guns");
    private static final ResourceLocation LOOT_ATTACHMENTS = ResourceLocation.fromNamespaceAndPath(TriggerMobs.MOD_ID, "loot/triggermobs_loot_attachments");
    private static final ResourceLocation LOOT_AMMO = ResourceLocation.fromNamespaceAndPath(TriggerMobs.MOD_ID, "loot/triggermobs_loot_ammo");

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (name == null) {
            return;
        }

        try {
            if (name.equals(LOOT_GUNS) && !TriggerMobsConfig.COMMON.includeLootGuns.get()) {
                event.getTable().removePool("main");
            } else if (name.equals(LOOT_ATTACHMENTS) && !TriggerMobsConfig.COMMON.includeLootAttachments.get()) {
                event.getTable().removePool("main");
            } else if (name.equals(LOOT_AMMO) && !TriggerMobsConfig.COMMON.includeLootAmmo.get()) {
                event.getTable().removePool("main");
            }
        } catch (RuntimeException e) {
            // Table may already be frozen or modified; ignore to avoid hard crashes
            TriggerMobs.LOGGER.debug("Failed to adjust TriggerMobs loot table {} for Loot Integrations config gating: {}", name, e.toString());
        }
    }
}

