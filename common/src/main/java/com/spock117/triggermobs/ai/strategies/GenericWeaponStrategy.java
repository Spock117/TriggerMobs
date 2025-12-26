package com.spock117.triggermobs.ai.strategies;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.network.ServerPlayHandler;
import com.nukateam.ntgl.common.network.message.C2SMessageShoot;
import com.nukateam.ntgl.common.data.holders.WeaponMode;
import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.ai.WeaponAIStrategy;
import com.spock117.triggermobs.util.InaccuracyHelper;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/**
 * Generic weapon strategy that uses the default AI behavior for non-CGS weapons.
 * This maintains backward compatibility with existing behavior.
 */
public class GenericWeaponStrategy implements WeaponAIStrategy {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private static final float DEFAULT_STRAFE_SPEED = 0.1F;
    private static final float DEFAULT_IDEAL_DISTANCE = 10.0F;
    private static final float DEFAULT_MIN_DISTANCE = 4.0F;
    private static final float DEFAULT_MAX_DISTANCE = 16.0F;
    
    private boolean strafeLeft;
    private int strafeCooldown;
    private int updatePathDelay;
    
    public GenericWeaponStrategy() {
        this.strafeLeft = false;
        this.strafeCooldown = 0;
        this.updatePathDelay = 0;
    }
    
    @Override
    public void move(PathfinderMob mob, LivingEntity target, double distance, double distanceSqr,
                     boolean hasAmmo, boolean canReload, boolean hasLineOfSight, int seeTime) {
        // Update strafe direction
        if (strafeCooldown > 0) {
            strafeCooldown--;
            if (strafeCooldown == 0) {
                strafeLeft = mob.getRandom().nextBoolean();
            }
        }
        
        // Only control movement when we have gun with ammo and can reload
        if (hasAmmo && canReload) {
            float attackRadiusSqr = DEFAULT_MAX_DISTANCE * DEFAULT_MAX_DISTANCE;
            boolean isInRange = distanceSqr <= attackRadiusSqr;
            
            if (isInRange && seeTime >= 5) {
                // In range: stop navigation and strafe
                mob.getNavigation().stop();
                
                // If too close, back away; otherwise move forward slightly
                float forwardSpeed = distance < DEFAULT_IDEAL_DISTANCE ? -DEFAULT_STRAFE_SPEED : DEFAULT_STRAFE_SPEED;
                
                // Sideways strafe
                float sideSpeed = strafeLeft ? -DEFAULT_STRAFE_SPEED : DEFAULT_STRAFE_SPEED;
                
                mob.setZza(forwardSpeed);
                mob.setXxa(sideSpeed);
            } else {
                // Out of range: move towards target using navigation
                --updatePathDelay;
                if (updatePathDelay <= 0) {
                    mob.getNavigation().moveTo(target, 0.6D);
                    updatePathDelay = PATHFINDING_DELAY_RANGE.sample(mob.getRandom());
                }
            }
        }
    }
    
    @Override
    public void shoot(PathfinderMob mob, LivingEntity target, InteractionHand hand, ItemStack weapon) {
        // Calculate base rotation
        float yaw = mob.getViewYRot(1.0F);
        float pitch = mob.getViewXRot(1.0F);
        
        // Add inaccuracy
        float pitchOffset = InaccuracyHelper.getPitchOffset(mob.getRandom());
        float yawOffset = InaccuracyHelper.getYawOffset(mob.getRandom());
        
        // Apply inaccuracy to rotation
        float finalYaw = yaw + yawOffset;
        float finalPitch = pitch + pitchOffset;
        
        // Create shoot message
        var msg = new C2SMessageShoot(
                mob.getId(),
                finalYaw,
                finalPitch,
                pitchOffset,
                yawOffset,
                hand,
                WeaponMode.PRIMARY
        );
        
        try {
            ServerPlayHandler.handleShoot(msg, mob);
        } catch (Exception e) {
            // Silently handle errors
        }
        
        // Swing hand for animation
        mob.swing(hand);
    }
    
    @Override
    public int getAttackDelay(PathfinderMob mob, WeaponData weaponData) {
        // Use config values with fallback to defaults
        int baseIntervalTicks = TriggerMobs.baseAttackIntervalTicks;
        int varianceTicks = TriggerMobs.attackIntervalVariance;
        
        // Defensive check
        if (baseIntervalTicks <= 0 || baseIntervalTicks < 20) {
            baseIntervalTicks = 200;
        }
        if (varianceTicks < 0) {
            varianceTicks = 80;
        }
        
        // Add randomization: ±variance ticks
        int randomOffset = mob.getRandom().nextInt(varianceTicks * 2 + 1) - varianceTicks;
        int calculatedDelay = baseIntervalTicks + randomOffset;
        return Math.max(20, calculatedDelay); // Ensure at least 20 ticks minimum
    }
    
    @Override
    public float getIdealDistance() {
        return DEFAULT_IDEAL_DISTANCE;
    }
    
    @Override
    public float getMinDistance() {
        return DEFAULT_MIN_DISTANCE;
    }
    
    @Override
    public float getMaxDistance() {
        return DEFAULT_MAX_DISTANCE;
    }
    
    @Override
    public boolean canDualWield() {
        return false; // Generic strategy doesn't support dual wielding
    }
    
    @Override
    public boolean shouldMaintainDistance() {
        return true;
    }
    
    public void updateStrafeCooldown(PathfinderMob mob) {
        strafeCooldown = 20 + mob.getRandom().nextInt(20);
    }
}

