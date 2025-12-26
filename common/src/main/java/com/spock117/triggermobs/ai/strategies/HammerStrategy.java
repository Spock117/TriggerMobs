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
 * AI strategy for Pneumatic Hammer weapons.
 * Characteristics: Melee weapon, close distance aggressively.
 */
public class HammerStrategy implements WeaponAIStrategy {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private static final float IDEAL_DISTANCE = 3.0F;
    private static final float MIN_DISTANCE = 1.0F;
    private static final float MAX_DISTANCE = 5.0F;
    private static final float STRAFE_SPEED = 0.15F; // Faster for melee
    
    private boolean strafeLeft;
    private int strafeCooldown;
    private int updatePathDelay;
    
    public HammerStrategy() {
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
                
                // Hammer: aggressive close combat
                float forwardSpeed;
                if (distance < IDEAL_DISTANCE) {
                    forwardSpeed = 0.0F; // Close enough, strafe
                } else {
                    forwardSpeed = STRAFE_SPEED * 2.0F; // Close distance aggressively
                }
                
                float sideSpeed = strafeLeft ? -STRAFE_SPEED : STRAFE_SPEED;
                
                mob.setZza(forwardSpeed);
                mob.setXxa(sideSpeed);
            } else {
                // Out of range: close distance aggressively
                --updatePathDelay;
                if (updatePathDelay <= 0) {
                    mob.getNavigation().moveTo(target, 0.8D); // Fast movement for melee
                    updatePathDelay = PATHFINDING_DELAY_RANGE.sample(mob.getRandom());
                }
            }
        }
    }
    
    @Override
    public void shoot(PathfinderMob mob, LivingEntity target, InteractionHand hand, ItemStack weapon) {
        // Hammer is a melee weapon, but we'll still use the shoot mechanism for consistency
        // The actual melee attack is handled by the weapon's melee action
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
        // Hammer: melee weapon, but we'll use a reasonable delay
        // Rate is 0 for melee, so we'll use a default melee attack speed
        int baseDelay = 20; // 1 second base delay for melee
        int variance = mob.getRandom().nextInt(10) - 5; // ±5 ticks
        return Math.max(baseDelay + variance, 15);
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
        return false; // Hammer cannot be dual wielded
    }
    
    @Override
    public boolean shouldMaintainDistance() {
        return false; // Hammer should close distance for melee
    }
    
    public void updateStrafeCooldown(PathfinderMob mob) {
        strafeCooldown = 15 + mob.getRandom().nextInt(15);
    }
}

