package com.spock117.triggermobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TriggerMobs {
    public static final String MOD_ID = "triggermobs";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    // Config values (set from Forge side)
    public static int baseAttackIntervalTicks = 200; // Default: 10 seconds
    public static int attackIntervalVariance = 80; // Default: ±4 seconds
    public static float aiAccuracyNerf = 1.0f; // 1.0 = baseline inaccuracy, higher = dumber zombies
    public static float guardAccuracyNerf = 0.7f; // Separate nerf for guard villagers (typically more skilled)
    public static int aiReactionDelayTicks = 0;
    /** Multiplier for attack delay (fire rate). > 1.0 = slower firing. Default 4.0. */
    public static double fireRateDelayMultiplier = 4.0;
    public static int outOfAmmoFallbackTicks = 100;

    // Loot Integrations config mirrors (set from Forge side)
    public static boolean includeLootGuns = true;
    public static boolean includeLootAttachments = true;
    public static boolean includeLootAmmo = true;

    public static void init() {
        LOGGER.info("TriggerMobs mod initialized");
    }
}
