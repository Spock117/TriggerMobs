package com.spock117.triggermobs.util;

import com.spock117.triggermobs.TriggerMobs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

public class InaccuracyHelper {
    // Base inaccuracy range at 1.0 multiplier: ±4-8 degrees (between old tier1 and tier2)
    private static final float BASE_MIN_DEGREES = 4.0f;
    private static final float BASE_MAX_DEGREES = 8.0f;

    private static final String GUARD_DESCRIPTION_ID = "entity.guardvillagers.guard";

    /**
     * Returns the accuracy nerf multiplier for the given shooter. Guards use guardAccuracyNerf.
     */
    public static float getAccuracyNerfFor(LivingEntity shooter) {
        if (shooter != null && GUARD_DESCRIPTION_ID.equals(shooter.getType().getDescriptionId())) {
            return Math.max(0.1f, TriggerMobs.guardAccuracyNerf);
        }
        return Math.max(0.1f, TriggerMobs.aiAccuracyNerf);
    }
    
    /**
     * Calculates a random pitch offset for inaccuracy.
     * Final spread = base range (±4-8 degrees) * accuracy nerf (guardAccuracyNerf for guards, aiAccuracyNerf otherwise).
     * @param random The random source
     * @param shooter The entity shooting (used to pick guard vs regular nerf)
     * @return Pitch offset in degrees
     */
    public static float getPitchOffset(RandomSource random, LivingEntity shooter) {
        float base = BASE_MIN_DEGREES + random.nextFloat() * (BASE_MAX_DEGREES - BASE_MIN_DEGREES);
        float nerf = getAccuracyNerfFor(shooter);
        return (random.nextBoolean() ? 1 : -1) * base * nerf;
    }
    
    /**
     * Calculates a random yaw offset for inaccuracy.
     * Final spread = base range (±4-8 degrees) * accuracy nerf (guardAccuracyNerf for guards, aiAccuracyNerf otherwise).
     * @param random The random source
     * @param shooter The entity shooting (used to pick guard vs regular nerf)
     * @return Yaw offset in degrees
     */
    public static float getYawOffset(RandomSource random, LivingEntity shooter) {
        float base = BASE_MIN_DEGREES + random.nextFloat() * (BASE_MAX_DEGREES - BASE_MIN_DEGREES);
        float nerf = getAccuracyNerfFor(shooter);
        return (random.nextBoolean() ? 1 : -1) * base * nerf;
    }
}

