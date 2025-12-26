package com.spock117.triggermobs.ai.strategies;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.network.ServerPlayHandler;
import com.nukateam.ntgl.common.network.message.C2SMessageShoot;
import com.nukateam.ntgl.common.data.holders.WeaponMode;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.spock117.triggermobs.ai.WeaponAIStrategy;
import com.spock117.triggermobs.util.InaccuracyHelper;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/**
 * AI strategy for Shotgun weapons.
 * Characteristics: 2 shots, fast reload, close range, aggressive movement.
 */
public class ShotgunStrategy implements WeaponAIStrategy {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private static final float IDEAL_DISTANCE = 6.0F;
    private static final float MIN_DISTANCE = 3.0F;
    private static final float MAX_DISTANCE = 10.0F;
    private static final float STRAFE_SPEED = 0.12F; // Slightly faster for close combat
    
    private boolean strafeLeft;
    private int strafeCooldown;
    private int updatePathDelay;
    
    public ShotgunStrategy() {
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
        
        if (hasAmmo && canReload) {
            float maxDistanceSqr = MAX_DISTANCE * MAX_DISTANCE;
            boolean isInRange = distanceSqr <= maxDistanceSqr;
            
            if (isInRange && seeTime >= 5) {
                mob.getNavigation().stop();
                
                // Shotgun: aggressive close-range combat
                float forwardSpeed;
                if (distance < IDEAL_DISTANCE) {
                    // Close enough, strafe
                    forwardSpeed = 0.0F;
                } else {
                    // Close distance aggressively
                    forwardSpeed = STRAFE_SPEED * 1.5F;
                }
                
                float sideSpeed = strafeLeft ? -STRAFE_SPEED : STRAFE_SPEED;
                
                mob.setZza(forwardSpeed);
                mob.setXxa(sideSpeed);
            } else {
                // Out of range: close distance aggressively
                --updatePathDelay;
                if (updatePathDelay <= 0) {
                    mob.getNavigation().moveTo(target, 0.7D); // Faster movement
                    updatePathDelay = PATHFINDING_DELAY_RANGE.sample(mob.getRandom());
                }
            }
        }
    }
    
    @Override
    public void shoot(PathfinderMob mob, LivingEntity target, InteractionHand hand, ItemStack weapon) {
        float yaw = mob.getViewYRot(1.0F);
        float pitch = mob.getViewXRot(1.0F);
        
        float pitchOffset = InaccuracyHelper.getPitchOffset(mob.getRandom());
        float yawOffset = InaccuracyHelper.getYawOffset(mob.getRandom());
        
        float finalYaw = yaw + yawOffset;
        float finalPitch = pitch + pitchOffset;
        
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
        
        mob.swing(hand);
    }
    
    @Override
    public int getAttackDelay(PathfinderMob mob, WeaponData weaponData) {
        // Shotgun: rate is 2 ticks (very fast)
        int rate = WeaponModifierHelper.getRate(weaponData);
        if (rate <= 0) {
            rate = 2;
        }
        
        int variance = mob.getRandom().nextInt(3) - 1; // ±1 tick
        return Math.max(rate + variance, 1);
    }
    
    @Override
    public float getIdealDistance() {
        return IDEAL_DISTANCE;
    }
    
    @Override
    public float getMinDistance() {
        return MIN_DISTANCE;
    }
    
    @Override
    public float getMaxDistance() {
        return MAX_DISTANCE;
    }
    
    @Override
    public boolean canDualWield() {
        return false; // Shotgun cannot be dual wielded
    }
    
    @Override
    public boolean shouldMaintainDistance() {
        return false; // Shotgun should close distance
    }
    
    public void updateStrafeCooldown(PathfinderMob mob) {
        strafeCooldown = 15 + mob.getRandom().nextInt(15);
    }
}

