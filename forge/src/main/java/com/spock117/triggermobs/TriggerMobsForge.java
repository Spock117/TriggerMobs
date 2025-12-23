package com.spock117.triggermobs;

import com.spock117.triggermobs.config.TriggerMobsConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TriggerMobs.MOD_ID)
public class TriggerMobsForge {
    public TriggerMobsForge() {
        TriggerMobs.init();
        
        // Register config - this will create the config file automatically in .minecraft/config/ or <server_folder>/config/
        // Using COMMON type so config is accessible and not world-specific
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TriggerMobsConfig.COMMON_SPEC);
        
        // Initialize config values immediately (they will be updated when config loads/reloads)
        initializeConfigValues();
        
        // Config events must be registered to the mod event bus, not the Forge event bus
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }
    
    @SubscribeEvent
    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(TriggerMobs.MOD_ID) && event.getConfig().getType() == ModConfig.Type.COMMON) {
            updateConfigValues();
        }
    }
    
    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(TriggerMobs.MOD_ID) && event.getConfig().getType() == ModConfig.Type.COMMON) {
            updateConfigValues();
        }
    }
    
    private void initializeConfigValues() {
        updateConfigValues();
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
                TriggerMobs.LOGGER.info("TriggerMobs config loaded and applied: baseAttackIntervalTicks={}, attackIntervalVariance={}", 
                    TriggerMobs.baseAttackIntervalTicks, TriggerMobs.attackIntervalVariance);
            } else {
                throw new NullPointerException("Config not initialized - COMMON or baseAttackIntervalTicks is null");
            }
        } catch (Exception e) {
            // Fallback to hardcoded defaults if config can't be read
            TriggerMobs.baseAttackIntervalTicks = 200;
            TriggerMobs.attackIntervalVariance = 80;
            TriggerMobs.LOGGER.error("Failed to load TriggerMobs config, using defaults: baseAttackIntervalTicks=200, attackIntervalVariance=80. Error: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
