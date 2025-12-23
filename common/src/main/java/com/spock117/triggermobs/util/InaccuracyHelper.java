package com.spock117.triggermobs.util;

import net.minecraft.util.RandomSource;

public class InaccuracyHelper {
    // Tier 1 (1/3 chance): Accurate - ±3-5 degrees
    private static final float TIER1_MIN_INACCURACY = 3.0f;
    private static final float TIER1_MAX_INACCURACY = 5.0f;
    
    // Tier 2 (2/3 chance): Less accurate - ±12-15 degrees
    private static final float TIER2_MIN_INACCURACY = 12.0f;
    private static final float TIER2_MAX_INACCURACY = 15.0f;
    
    // Probability: 2/3 for tier 2 (less accurate), 1/3 for tier 1 (accurate)
    private static final float TIER2_PROBABILITY = 2.0f / 3.0f;
    
    /**
     * Calculates a random pitch offset for inaccuracy with two tiers
     * @param random The random source
     * @return Pitch offset in degrees
     */
    public static float getPitchOffset(RandomSource random) {
        // Choose tier: 2/3 chance for less accurate, 1/3 for accurate
        boolean useTier2 = random.nextFloat() < TIER2_PROBABILITY;
        
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
        // Choose tier: 2/3 chance for less accurate, 1/3 for accurate
        boolean useTier2 = random.nextFloat() < TIER2_PROBABILITY;
        
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

