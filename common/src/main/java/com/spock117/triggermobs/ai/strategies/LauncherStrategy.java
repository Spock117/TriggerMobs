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
 * AI strategy for Launcher weapons.
 * Characteristics: 4 rockets, long reload, long range, maintain distance.
 */
public class LauncherStrategy implements WeaponAIStrategy {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private static final float IDEAL_DISTANCE = 16.0F;
    private static final float MIN_DISTANCE = 10.0F;
    private static final float MAX_DISTANCE = 22.0F;
    private static final float STRAFE_SPEED = 0.1F;
    
    private boolean strafeLeft;
    private int strafeCooldown;
    private int updatePathDelay;
    
    public LauncherStrategy() {
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
                
                // Launcher: maintain long distance, back away if too close
                float forwardSpeed;
                if (distance < MIN_DISTANCE) {
                    forwardSpeed = -STRAFE_SPEED * 1.5F; // Back away more aggressively
                } else if (distance > MAX_DISTANCE) {
                    forwardSpeed = STRAFE_SPEED;
                } else {
                    forwardSpeed = 0.0F;
                }
                
                float sideSpeed = strafeLeft ? -STRAFE_SPEED : STRAFE_SPEED;
                
                mob.setZza(forwardSpeed);
                mob.setXxa(sideSpeed);
            } else {
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
        // Launcher: rate is 4 ticks
        int rate = WeaponModifierHelper.getRate(weaponData);
        if (rate <= 0) {
            rate = 4;
        }
        
        int variance = mob.getRandom().nextInt(5) - 2;
        return Math.max(rate + variance, 3);
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
        return false; // Launcher cannot be dual wielded
    }
    
    @Override
    public boolean shouldMaintainDistance() {
        return true; // Launcher should maintain long distance
    }
    
    public void updateStrafeCooldown(PathfinderMob mob) {
        strafeCooldown = 25 + mob.getRandom().nextInt(25);
    }
}

