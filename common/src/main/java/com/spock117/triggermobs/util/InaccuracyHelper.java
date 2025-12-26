package com.spock117.triggermobs.util;

import com.spock117.triggermobs.TriggerMobs;
import net.minecraft.util.RandomSource;

public class InaccuracyHelper {
    // Tier 1: Accurate - ±3-5 degrees
    private static final float TIER1_MIN_INACCURACY = 3.0f;
    private static final float TIER1_MAX_INACCURACY = 5.0f;
    
    // Tier 2: Less accurate - ±12-15 degrees
    private static final float TIER2_MIN_INACCURACY = 12.0f;
    private static final float TIER2_MAX_INACCURACY = 15.0f;
    
    /**
     * Gets the probability of Tier 2 (less accurate) shots.
     * Calculated as (1.0 - tier1Probability) from config.
     * @return Probability of Tier 2 shots (0.0 to 1.0)
     */
    private static float getTier2Probability() {
        // Ensure tier1Probability is valid (defensive check)
        float tier1Prob = TriggerMobs.tier1Probability;
        if (tier1Prob < 0.0f || tier1Prob > 1.0f) {
            tier1Prob = 0.125f; // Default: 12.5%
        }
        return 1.0f - tier1Prob;
    }
    
    /**
     * Calculates a random pitch offset for inaccuracy with two tiers
     * @param random The random source
     * @return Pitch offset in degrees
     */
    public static float getPitchOffset(RandomSource random) {
        // Choose tier based on configurable probability
        float tier2Prob = getTier2Probability();
        boolean useTier2 = random.nextFloat() < tier2Prob;
        
        float minInaccuracy, maxInaccuracy;
        if (useTier2) {
            // Tier 2: Less accurate (±12-15 degrees)
            minInaccuracy = TIER2_MIN_INACCURACY;
            maxInaccuracy = TIER2_MAX_INACCURACY;
        } else {
            // Tier 1: Accurate (±3-5 degrees)
            minInaccuracy = TIER1_MIN_INACCURACY;
            maxInaccuracy = TIER1_MAX_INACCURACY;
        }
        
        float range = maxInaccuracy - minInaccuracy;
        float base = minInaccuracy + random.nextFloat() * range;
        // Randomly positive or negative
        return (random.nextBoolean() ? 1 : -1) * base;
    }
    
    /**
     * Calculates a random yaw offset for inaccuracy with two tiers
     * @param random The random source
     * @return Yaw offset in degrees
     */
    public static float getYawOffset(RandomSource random) {
        // Choose tier based on configurable probability
        float tier2Prob = getTier2Probability();
        boolean useTier2 = random.nextFloat() < tier2Prob;
        
        float minInaccuracy, maxInaccuracy;
        if (useTier2) {
            // Tier 2: Less accurate (±12-15 degrees)
            minInaccuracy = TIER2_MIN_INACCURACY;
            maxInaccuracy = TIER2_MAX_INACCURACY;
        } else {
            // Tier 1: Accurate (±3-5 degrees)
            minInaccuracy = TIER1_MIN_INACCURACY;
            maxInaccuracy = TIER1_MAX_INACCURACY;
        }
        
        float range = maxInaccuracy - minInaccuracy;
        float base = minInaccuracy + random.nextFloat() * range;
        // Randomly positive or negative
        return (random.nextBoolean() ? 1 : -1) * base;
    }
}

