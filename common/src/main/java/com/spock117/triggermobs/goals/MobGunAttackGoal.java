package com.spock117.triggermobs.goals;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.holders.WeaponMode;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.network.ServerPlayHandler;
import com.nukateam.ntgl.common.network.message.C2SMessageShoot;
import com.nukateam.ntgl.common.util.trackers.EntityReloadTracker;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.util.InaccuracyHelper;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class MobGunAttackGoal extends Goal {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private final PathfinderMob mob;
    private final double speedModifier;
    private final float attackRadiusSqr;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;
    private boolean strafeLeft;
    private int strafeCooldown;
    // Base attack interval: configurable via config file (default: 200 ticks / 10 seconds)
    // For dual wielding, this is halved
    private boolean useMainHand = true; // For dual wielding: alternate between hands
    private boolean isDualWielding = false;
    
    public MobGunAttackGoal(PathfinderMob mob, double speedModifier, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.strafeLeft = mob.getRandom().nextBoolean();
        // Initialize attack delay to prevent immediate first shot
        // Use a small random delay to stagger initial attacks
        this.attackDelay = 10 + mob.getRandom().nextInt(20); // 0.5-1.5 seconds initial delay
    }

    @Override
    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingGun();
    }

    private boolean isHoldingGun() {
        ItemStack mainHandStack = mob.getMainHandItem();
        ItemStack offHandStack = mob.getOffhandItem();
        boolean mainHand = mainHandStack.getItem() instanceof IWeapon;
        boolean offHand = offHandStack.getItem() instanceof IWeapon;
        
        
        return mainHand || offHand;
    }

    @Override
    public boolean canContinueToUse() {
        return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingGun();
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }


    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var target = this.mob.getTarget();
        if (target == null) {
            return;
        }

        // Update strafe direction
        if (strafeCooldown > 0) {
            strafeCooldown--;
            if (strafeCooldown == 0) {
                strafeLeft = mob.getRandom().nextBoolean();
            }
        }

        var hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
        var flag1 = this.seeTime > 0;

        if (hasLineOfSight != flag1) this.seeTime = 0;

        if (hasLineOfSight)
            ++this.seeTime;
        else --this.seeTime;

        double distance = this.mob.distanceTo(target);
        double distanceSqr = this.mob.distanceToSqr(target);
        boolean isInRange = distanceSqr <= attackRadiusSqr;
        
        // Check if mob has gun with ammo (determines if we should control movement)
        ItemStack mainHandWeapon = mob.getMainHandItem();
        ItemStack offHandWeapon = mob.getOffhandItem();
        boolean hasMainGun = mainHandWeapon.getItem() instanceof IWeapon;
        boolean hasOffGun = offHandWeapon.getItem() instanceof IWeapon;
        boolean hasGunWithAmmo = false;
        
        if (hasMainGun && WeaponStateHelper.hasAmmo(mainHandWeapon)) {
            hasGunWithAmmo = true;
        } else if (hasOffGun && WeaponStateHelper.hasAmmo(offHandWeapon)) {
            hasGunWithAmmo = true;
        }
        
        boolean canReload = !EntityReloadTracker.isReloading(mob);
        
        // Movement logic - only override when we have gun with ammo and can reload
        // Otherwise, let default behavior handle it (don't interfere)
        if (hasGunWithAmmo && canReload) {
            // Has gun with ammo: control movement to maintain distance
            if (isInRange && this.seeTime >= 5) {
                // In range: stop navigation and strafe
                this.mob.getNavigation().stop();
                
                // Strafe speed - use much smaller values to avoid teleporting
                // These values are movement inputs, not speeds - they get multiplied by movement attribute
                float strafeSpeed = 0.1F; // Reduced from 0.5F - much slower strafing
                
                // If too close (< 10 blocks), back away; otherwise move forward slightly
                float forwardSpeed = distance < 10.0 ? -strafeSpeed : strafeSpeed;
                
                // Sideways strafe
                float sideSpeed = strafeLeft ? -strafeSpeed : strafeSpeed;
                
                // Use direct movement control - do NOT call setSpeed() as it's for navigation
                // setZza and setXxa are movement inputs (0.0 to 1.0), they get processed by MoveControl
                // Using very small values to prevent excessive speed
                this.mob.setZza(forwardSpeed);
                this.mob.setXxa(sideSpeed);
            } else {
                // Out of range: move towards target using navigation
                --this.updatePathDelay;
                if (this.updatePathDelay <= 0) {
                    this.mob.getNavigation().moveTo(target, this.speedModifier);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
                }
            }
        }
        // If no gun with ammo or can't reload: don't interfere with movement
        // Let other goals (like melee attack) handle movement naturally

        // Look at target
        this.mob.getLookControl().setLookAt(target, 60.0F, 60.0F);
        
        // Don't sprint while aiming
        this.mob.setSprinting(false);

        // Dual wielding support: check both hands (already checked above, reuse)
        isDualWielding = hasMainGun && hasOffGun;
        
        // If dual wielding, alternate between hands
        InteractionHand handToUse = null;
        ItemStack weaponToUse = null;
        
        if (isDualWielding) {
            // Dual wielding: alternate between hands
            if (useMainHand) {
                handToUse = InteractionHand.MAIN_HAND;
                weaponToUse = mainHandWeapon;
            } else {
                handToUse = InteractionHand.OFF_HAND;
                weaponToUse = offHandWeapon;
            }
        } else if (hasMainGun) {
            handToUse = InteractionHand.MAIN_HAND;
            weaponToUse = mainHandWeapon;
        } else if (hasOffGun) {
            handToUse = InteractionHand.OFF_HAND;
            weaponToUse = offHandWeapon;
        } else {
            return; // No weapon in either hand
        }

        HumanoidArm arm = handToUse == InteractionHand.MAIN_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;

        // Check if entity is reloading (EntityReloadTracker only tracks one reload at a time)
        boolean wasReloading = EntityReloadTracker.isReloading(mob);
        boolean isReloading = wasReloading;
        
        // Check ammo for the weapon we're using
        boolean hasAmmo = WeaponStateHelper.hasAmmo(weaponToUse);
        int ammoCount = WeaponStateHelper.getAmmoCount(new WeaponData(weaponToUse, mob));
        
        // Check if reload just completed
        if (wasReloading && !EntityReloadTracker.isReloading(mob)) {
            isReloading = false;
        }
        
        if (!hasAmmo) {
            // No ammo, start reload if not already reloading
            if (!isReloading) {
                EntityReloadTracker.addTracker(mob, arm);
                isReloading = true;
            }
            // Don't allow firing while reloading - return early
            return;
        }
        
        // If reloading (any hand), don't allow firing - wait for reload to complete
        if (isReloading) {
            return;
        }

        // Attack logic
        if (isInRange && this.seeTime >= 5 && attackDelay <= 0) {
            shoot(target, handToUse);
            
            // Calculate attack interval: configurable base, halved for dual wielding, with randomization
            // Use config values with fallback to defaults
            int baseIntervalTicks = TriggerMobs.baseAttackIntervalTicks;
            int varianceTicks = TriggerMobs.attackIntervalVariance;
            
            // Defensive check: ensure we have valid values (should have been validated during config load)
            if (baseIntervalTicks <= 0 || baseIntervalTicks < 20) {
                TriggerMobs.LOGGER.warn("Invalid baseAttackIntervalTicks value {} detected in MobGunAttackGoal, using default 200", baseIntervalTicks);
                baseIntervalTicks = 200;
            }
            if (varianceTicks < 0) {
                TriggerMobs.LOGGER.warn("Invalid attackIntervalVariance value {} detected in MobGunAttackGoal, using default 80", varianceTicks);
                varianceTicks = 80;
            }
            
            int baseInterval = isDualWielding ? baseIntervalTicks / 2 : baseIntervalTicks;
            // Add randomization: ±variance ticks
            int randomOffset = mob.getRandom().nextInt(varianceTicks * 2 + 1) - varianceTicks;
            int calculatedDelay = baseInterval + randomOffset;
            attackDelay = Math.max(20, calculatedDelay); // Ensure at least 20 ticks (1 second) minimum
            
            // Log if delay seems unusually low (for debugging)
            if (attackDelay < 40) {
                TriggerMobs.LOGGER.debug("Mob {} calculated attack delay: {} ticks (baseInterval: {}, variance: {}, isDualWielding: {})", 
                    mob.getDisplayName().getString(), attackDelay, baseIntervalTicks, varianceTicks, isDualWielding);
            }
            
            strafeCooldown = 20 + mob.getRandom().nextInt(20); // Change strafe direction periodically
            
            // Alternate hands for dual wielding
            if (isDualWielding) {
                useMainHand = !useMainHand;
            }
        }

        if (attackDelay > 0) {
            attackDelay--;
        }
    }

    private void shoot(LivingEntity target, InteractionHand hand) {
        // Calculate base rotation
        float yaw = mob.getViewYRot(1.0F);
        float pitch = mob.getViewXRot(1.0F);

        // Add inaccuracy (moderate level)
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
                pitchOffset, // randP
                yawOffset,   // randY
                hand,
                WeaponMode.PRIMARY
        );

        try {
            // Handle shoot directly (for mobs, we bypass the network layer)
            ServerPlayHandler.handleShoot(msg, mob);
        } catch (Exception e) {
            // Silently handle errors
        }
        
        // Swing hand for animation
        mob.swing(hand);
    }
    
    @Override
    public void start() {
        super.start();
    }
    
    @Override
    public void stop() {
        super.stop();
        if (mob instanceof Monster monster) {
            monster.setAggressive(false);
        }
        this.seeTime = 0;
        this.attackDelay = 0;
        this.strafeCooldown = 0;
    }
}

