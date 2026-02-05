package com.spock117.triggermobs.events;

import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.util.ModDetectionHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When Create:Gunsmithing (CGS) is present, the guard_armor loot table is modified
 * to remove the NTGL gun pool so guards only spawn with CGS guns from the gun pools.
 */
@Mod.EventBusSubscriber(modid = TriggerMobs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuardLootTableEvents {

    private static final ResourceLocation GUARD_ARMOR = new ResourceLocation("guardvillagers", "entities/guard_armor");
    private static final String[] NTGL_POOL_NAMES = {
        "triggermobs_ntgl_gun_pool",
        "triggermobs_ntgl_onehanded_pool",
        "triggermobs_ntgl_twohanded_pool",
        "ntgl_pistol10mm", "ntgl_revolver", "ntgl_pipepistol", "ntgl_piperevolver",
        "ntgl_classic10mm", "ntgl_scout10mm", "ntgl_laser_pistol",
        "ntgl_flamer", "ntgl_shotgun", "ntgl_powdergun", "ntgl_laser_rifle",
        "ntgl_minigun", "ntgl_gatling"
    };

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!event.getName().equals(GUARD_ARMOR)) {
            return;
        }
        if (!ModDetectionHelper.isGuardVillagersLoaded()) {
            return;
        }
        if (!ModDetectionHelper.isCreateGunsmithingLoaded()) {
            return;
        }
        try {
            for (String poolName : NTGL_POOL_NAMES) {
                event.getTable().removePool(poolName);
            }
        } catch (RuntimeException e) {
            // Table may already be frozen or pool missing; ignore
        }
    }
}
