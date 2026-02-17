package com.spock117.triggermobs.events;

import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.config.TriggerMobsConfig;
import com.spock117.triggermobs.util.ModDetectionHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When Create:Gunsmithing (CGS) is present, the guard_armor loot table is modified:
 * - NTGL gun pools are always removed.
 * - CGS gun pools are removed when guard CGS spawning is enabled in TriggerMobs,
 *   so TriggerMobs is the sole source of guard CGS weapons (TConstruct weapons from
 *   datapack remain).
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
    private static final String[] CGS_POOL_NAMES = {
        "cgs_flintlock", "cgs_revolver", "cgs_shotgun", "cgs_nailgun",
        "cgs_gatling", "cgs_blazegun", "cgs_launcher"
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
            if (TriggerMobsConfig.COMMON.guardCgsSpawningEnabled.get()) {
                for (String poolName : CGS_POOL_NAMES) {
                    event.getTable().removePool(poolName);
                }
            }
        } catch (RuntimeException e) {
            // Table may already be frozen or pool missing; ignore
        }
    }
}
