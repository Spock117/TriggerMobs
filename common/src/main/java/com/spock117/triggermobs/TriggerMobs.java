package com.spock117.triggermobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TriggerMobs {
    public static final String MOD_ID = "triggermobs";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    // Config values (set from Forge side)
    public static int baseAttackIntervalTicks = 200; // Default: 10 seconds
    public static int attackIntervalVariance = 80; // Default: ±4 seconds

    public static void init() {
        LOGGER.info("TriggerMobs mod initialized");
    }
}
