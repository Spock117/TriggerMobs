package com.spock117.triggermobs;

import com.spock117.triggermobs.config.TriggerMobsConfig;
import com.spock117.triggermobs.spawn.GunPicker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

@Mod(TriggerMobs.MOD_ID)
public class TriggerMobsForge {
    public TriggerMobsForge(IEventBus modEventBus, ModContainer container) {
        TriggerMobs.init();

        container.registerConfig(ModConfig.Type.COMMON, TriggerMobsConfig.COMMON_SPEC);

        // Values mirror TriggerMobs static defaults until ModConfigEvent.Loading runs;
        // do not call .get() here or Forge logs "Cannot get config value before config is loaded."
        modEventBus.register(this);
    }
    
    @SubscribeEvent
    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(TriggerMobs.MOD_ID) && event.getConfig().getType() == ModConfig.Type.COMMON) {
            GunPicker.invalidateWeaponOverrideCache();
            updateConfigValues();
        }
    }
    
    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(TriggerMobs.MOD_ID) && event.getConfig().getType() == ModConfig.Type.COMMON) {
            GunPicker.invalidateWeaponOverrideCache();
            updateConfigValues();
        }
    }
    
    private void updateConfigValues() {
        try {
            if (TriggerMobsConfig.COMMON != null && TriggerMobsConfig.COMMON.baseAttackIntervalTicks != null) {
                int baseInterval = TriggerMobsConfig.COMMON.baseAttackIntervalTicks.get();
                int variance = TriggerMobsConfig.COMMON.attackIntervalVariance.get();
                
                // Log raw values for debugging
                TriggerMobs.LOGGER.debug("Raw config values - baseInterval: {}, variance: {}", baseInterval, variance);
                
                // Ensure values are within valid ranges (defensive check)
                // If config returns 0 or invalid values, use defaults
                if (baseInterval <= 0 || baseInterval < 20) {
                    TriggerMobs.LOGGER.warn("baseAttackIntervalTicks value {} is invalid (must be >= 20), using default 200", baseInterval);
                    baseInterval = 200;
                }
                if (baseInterval > 1000) {
                    TriggerMobs.LOGGER.warn("baseAttackIntervalTicks value {} is too high, using maximum 1000", baseInterval);
                    baseInterval = 1000;
                }
                if (variance < 0) {
                    TriggerMobs.LOGGER.warn("attackIntervalVariance value {} is negative, using default 80", variance);
                    variance = 80;
                }
                if (variance > 200) {
                    TriggerMobs.LOGGER.warn("attackIntervalVariance value {} is too high, using maximum 200", variance);
                    variance = 200;
                }
                
                TriggerMobs.baseAttackIntervalTicks = baseInterval;
                TriggerMobs.attackIntervalVariance = variance;
                
                if (TriggerMobsConfig.COMMON.aiAccuracyNerf != null) {
                    TriggerMobs.aiAccuracyNerf = TriggerMobsConfig.COMMON.aiAccuracyNerf.get().floatValue();
                }
                if (TriggerMobsConfig.COMMON.guardAccuracyNerf != null) {
                    TriggerMobs.guardAccuracyNerf = TriggerMobsConfig.COMMON.guardAccuracyNerf.get().floatValue();
                }
                if (TriggerMobsConfig.COMMON.aiReactionDelayTicks != null) {
                    TriggerMobs.aiReactionDelayTicks = TriggerMobsConfig.COMMON.aiReactionDelayTicks.get();
                }
                if (TriggerMobsConfig.COMMON.fireRateDelayMultiplier != null) {
                    TriggerMobs.fireRateDelayMultiplier = TriggerMobsConfig.COMMON.fireRateDelayMultiplier.get();
                }
                if (TriggerMobsConfig.COMMON.outOfAmmoFallbackTicks != null) {
                    TriggerMobs.outOfAmmoFallbackTicks = TriggerMobsConfig.COMMON.outOfAmmoFallbackTicks.get();
                }
                if (TriggerMobsConfig.COMMON.includeLootGuns != null) {
                    TriggerMobs.includeLootGuns = TriggerMobsConfig.COMMON.includeLootGuns.get();
                }
                if (TriggerMobsConfig.COMMON.includeLootAttachments != null) {
                    TriggerMobs.includeLootAttachments = TriggerMobsConfig.COMMON.includeLootAttachments.get();
                }
                if (TriggerMobsConfig.COMMON.includeLootAmmo != null) {
                    TriggerMobs.includeLootAmmo = TriggerMobsConfig.COMMON.includeLootAmmo.get();
                }
                TriggerMobs.LOGGER.info("TriggerMobs config loaded: baseAttackIntervalTicks={}, attackIntervalVariance={}, aiAccuracyNerf={}, guardAccuracyNerf={}, aiReactionDelayTicks={}, fireRateDelayMultiplier={}, outOfAmmoFallbackTicks={}, includeLootGuns={}, includeLootAttachments={}, includeLootAmmo={}",
                    TriggerMobs.baseAttackIntervalTicks, TriggerMobs.attackIntervalVariance,
                    TriggerMobs.aiAccuracyNerf, TriggerMobs.guardAccuracyNerf, TriggerMobs.aiReactionDelayTicks, TriggerMobs.fireRateDelayMultiplier, TriggerMobs.outOfAmmoFallbackTicks,
                    TriggerMobs.includeLootGuns, TriggerMobs.includeLootAttachments, TriggerMobs.includeLootAmmo);
            } else {
                throw new NullPointerException("Config not initialized - COMMON or baseAttackIntervalTicks is null");
            }
        } catch (Exception e) {
            // Fallback to hardcoded defaults if config can't be read
            TriggerMobs.baseAttackIntervalTicks = 200;
            TriggerMobs.attackIntervalVariance = 80;
            TriggerMobs.aiAccuracyNerf = 1.0f;
            TriggerMobs.guardAccuracyNerf = 0.7f;
            TriggerMobs.aiReactionDelayTicks = 0;
            TriggerMobs.fireRateDelayMultiplier = 4.0;
            TriggerMobs.outOfAmmoFallbackTicks = 100;
            TriggerMobs.LOGGER.error("Failed to load TriggerMobs config, using defaults. Error: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
