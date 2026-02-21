package com.spock117.triggermobs.goals;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.holders.WeaponMode;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.network.ServerPlayHandler;
import com.nukateam.ntgl.common.network.message.C2SMessageShoot;
import com.nukateam.ntgl.common.util.trackers.EntityReloadTracker;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.ai.WeaponAIStrategy;
import com.spock117.triggermobs.ai.WeaponStrategyFactory;
import com.spock117.triggermobs.util.InaccuracyHelper;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
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
    
    // Strategy pattern: current weapon AI strategy
    private WeaponAIStrategy currentStrategy;
    private ItemStack lastWeaponStack = ItemStack.EMPTY; // Track weapon changes

    // Out-of-ammo fallback: when mob has no ammo and cannot reload, fall back to melee/ranged after N ticks
    private int outOfAmmoTicks;
    
    public MobGunAttackGoal(PathfinderMob mob, double speedModifier, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.strafeLeft = mob.getRandom().nextBoolean();
        // Initialize attack delay to prevent immediate first shot
        // Use a small random delay to stagger initial attacks
        this.attackDelay = 10 + mob.getRandom().nextInt(20); // 0.5-1.5 seconds initial delay
        // Initialize with generic strategy
        this.currentStrategy = new com.spock117.triggermobs.ai.strategies.GenericWeaponStrategy();
    }

    @Override
    public boolean canUse() {
        if (!this.isValidTarget() || !this.isHoldingGun()) {
            return false;
        }
        if (TriggerMobs.outOfAmmoFallbackTicks <= 0 && !hasAnyAmmoOrCanReload()) {
            return false;
        }
        return true;
    }

    private boolean isHoldingGun() {
        ItemStack mainHandStack = mob.getMainHandItem();
        ItemStack offHandStack = mob.getOffhandItem();
        boolean mainHand = mainHandStack.getItem() instanceof IWeapon;
        boolean offHand = offHandStack.getItem() instanceof IWeapon;
        return mainHand || offHand;
    }

    private boolean hasAnyAmmoOrCanReload() {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        if (mainHand.getItem() instanceof IWeapon && WeaponStateHelper.hasAmmo(mainHand)) return true;
        if (offHand.getItem() instanceof IWeapon && WeaponStateHelper.hasAmmo(offHand)) return true;
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.isValidTarget() || !this.isHoldingGun()) {
            return false;
        }
        if (!hasAnyAmmoOrCanReload()) {
            int fallbackTicks = TriggerMobs.outOfAmmoFallbackTicks;
            if (fallbackTicks > 0) {
                outOfAmmoTicks++;
                if (outOfAmmoTicks >= fallbackTicks) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            outOfAmmoTicks = 0;
        }
        return (this.canUse() || !this.mob.getNavigation().isDone());
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
        // Safety check: ensure mob and level are valid
        if (mob == null || !mob.isAlive() || mob.level() == null) {
            return;
        }
        
        var target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
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
        
        // Check if mob has gun with ammo (determines if we should control movement)
        ItemStack mainHandWeapon = mob.getMainHandItem();
        ItemStack offHandWeapon = mob.getOffhandItem();
        boolean hasMainGun = mainHandWeapon.getItem() instanceof IWeapon;
        boolean hasOffGun = offHandWeapon.getItem() instanceof IWeapon;
        
        // Determine which weapon to use and detect weapon changes
        InteractionHand handToUse = null;
        ItemStack weaponToUse = null;
        
        if (hasMainGun && hasOffGun) {
            // Dual wielding: check if both weapons can be dual wielded
            WeaponData mainData = new WeaponData(mainHandWeapon, mob);
            WeaponData offData = new WeaponData(offHandWeapon, mob);
            boolean mainCanDual = WeaponModifierHelper.isOneHanded(mainData);
            boolean offCanDual = WeaponModifierHelper.isOneHanded(offData);
            
            if (mainCanDual && offCanDual) {
                // Both can dual wield
                isDualWielding = true;
                if (useMainHand) {
                    handToUse = InteractionHand.MAIN_HAND;
                    weaponToUse = mainHandWeapon;
                } else {
                    handToUse = InteractionHand.OFF_HAND;
                    weaponToUse = offHandWeapon;
                }
            } else {
                // Can't dual wield, use main hand
                isDualWielding = false;
                handToUse = InteractionHand.MAIN_HAND;
                weaponToUse = mainHandWeapon;
            }
        } else if (hasMainGun) {
            isDualWielding = false;
            handToUse = InteractionHand.MAIN_HAND;
            weaponToUse = mainHandWeapon;
        } else if (hasOffGun) {
            isDualWielding = false;
            handToUse = InteractionHand.OFF_HAND;
            weaponToUse = offHandWeapon;
        } else {
            return; // No weapon in either hand
        }
        
        // Detect weapon changes and switch strategy if needed
        if (weaponToUse != null && !weaponToUse.isEmpty()) {
            // Check if weapon changed
            boolean weaponChanged = !ItemStack.isSameItem(weaponToUse, lastWeaponStack);
            
            if (weaponChanged) {
                // Always switch strategy when weapon changes (strategy will handle ammo checks)
                currentStrategy = WeaponStrategyFactory.createStrategy(weaponToUse);
                if (currentStrategy == null) {
                    // Fallback to generic if factory returns null
                    currentStrategy = new com.spock117.triggermobs.ai.strategies.GenericWeaponStrategy();
                }
                lastWeaponStack = weaponToUse.copy();
            }
        }
        
        // Ensure strategy is never null
        if (currentStrategy == null) {
            currentStrategy = new com.spock117.triggermobs.ai.strategies.GenericWeaponStrategy();
        }
        
        // Update dual wielding capability based on strategy
        if (isDualWielding && !currentStrategy.canDualWield()) {
            // Strategy doesn't support dual wielding, use main hand only
            isDualWielding = false;
            handToUse = InteractionHand.MAIN_HAND;
            weaponToUse = mainHandWeapon;
        }
        
        boolean hasGunWithAmmo = false;
        if (hasMainGun && WeaponStateHelper.hasAmmo(mainHandWeapon)) {
            hasGunWithAmmo = true;
        } else if (hasOffGun && WeaponStateHelper.hasAmmo(offHandWeapon)) {
            hasGunWithAmmo = true;
        }
        
        boolean canReload = !EntityReloadTracker.isReloading(mob);
        
        // Use strategy's ideal distance for range calculation
        float strategyMaxDistance = currentStrategy.getMaxDistance();
        float strategyMaxDistanceSqr = strategyMaxDistance * strategyMaxDistance;
        boolean isInRange = distanceSqr <= strategyMaxDistanceSqr;
                
        // Delegate movement to strategy
        currentStrategy.move(mob, target, distance, distanceSqr, hasGunWithAmmo, canReload, hasLineOfSight, seeTime);

        // Look at target
        this.mob.getLookControl().setLookAt(target, 60.0F, 60.0F);
        
        // Don't sprint while aiming
        this.mob.setSprinting(false);

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

        // Attack logic (aiReactionDelayTicks adds extra delay before first shot)
        int reactionThreshold = 5 + TriggerMobs.aiReactionDelayTicks;
        if (isInRange && this.seeTime >= reactionThreshold && attackDelay <= 0) {
            // Delegate shooting to strategy
            currentStrategy.shoot(mob, target, handToUse, weaponToUse);
            
            // Calculate attack delay using strategy
            WeaponData weaponData = new WeaponData(weaponToUse, mob);
            int calculatedDelay = currentStrategy.getAttackDelay(mob, weaponData);
            // Apply configurable fire rate multiplier (applies to all mobs)
            calculatedDelay = Math.max(1, (int) Math.ceil(calculatedDelay * TriggerMobs.fireRateDelayMultiplier));

            // For dual wielding, halve the delay
            if (isDualWielding) {
                calculatedDelay = calculatedDelay / 2;
            }
            
            attackDelay = Math.max(1, calculatedDelay); // Ensure at least 1 tick minimum
            
            currentStrategy.updateStrafeCooldown(mob);
            
            // Alternate hands for dual wielding
            if (isDualWielding) {
                useMainHand = !useMainHand;
            }
        }

        if (attackDelay > 0) {
            attackDelay--;
        }
    }

    // shoot() method removed - now delegated to strategy
    
    @Override
    public void start() {
        super.start();
        mob.setAggressive(true);
    }
    
    @Override
    public void stop() {
        super.stop();
        mob.setAggressive(false);
        this.seeTime = 0;
        this.attackDelay = 0;
        this.strafeCooldown = 0;
        this.lastWeaponStack = ItemStack.EMPTY;
        this.outOfAmmoTicks = 0;
    }
}

