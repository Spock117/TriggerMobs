package com.spock117.triggermobs.goals;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.holders.WeaponMode;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import com.spock117.triggermobs.compat.RecruitsNtglInventoryReload;
import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.ai.WeaponAIStrategy;
import com.spock117.triggermobs.ai.WeaponStrategyFactory;
import com.spock117.triggermobs.ai.strategies.GenericWeaponStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.EnumSet;

/**
 * triggermobs-only gun goal for {@code recruits:crossbowman}.
 * <p>
 * Uses NTGL/CGS shooting strategies while respecting the recruits command state via reflection.
 * When the weapon runs out of ammo long enough to trigger fallback, this goal stops so the
 * recruits vanilla bow/crossbow goals can take over.
 * <p>
 * Ammo checks use {@link WeaponStateHelper#hasAmmo(WeaponData)} with this mob as wielder
 * (same idea as NTGL {@code GunAttackGoal#hasAmmo}).
 */
public class RecruitsMusketTypeGunGoal extends Goal {
    private final PathfinderMob mob;
    private final float attackRadius;

    private int seeTime;
    private int attackDelay;

    private int outOfAmmoTicks;
    /** Throttle reload attempts; NTGL mob reload would refill for free — we use inventory instead. */
    private int reloadCooldown;

    private WeaponAIStrategy currentStrategy;
    private ItemStack lastWeaponStack = ItemStack.EMPTY;

    // Keep LOOK only; we don't want to interfere with recruits pathing/movement.
    // (Recruits ranged goals also typically use LOOK-only.)
    @Override
    public EnumSet<Flag> getFlags() {
        return EnumSet.of(Flag.LOOK);
    }

    public RecruitsMusketTypeGunGoal(PathfinderMob mob, float attackRadius) {
        this.mob = mob;
        this.attackRadius = attackRadius;

        this.setFlags(EnumSet.of(Flag.LOOK));
        this.currentStrategy = new GenericWeaponStrategy();
        this.attackDelay = 10 + mob.getRandom().nextInt(20);
    }

    @Override
    public boolean canUse() {
        if (!isHoldingGun()) {
            return false;
        }

        if (!isRecruitsCommandedToShoot()) {
            return false;
        }

        if (TriggerMobs.outOfAmmoFallbackTicks <= 0 && !hasAnyAmmo()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isHoldingGun() || !isRecruitsCommandedToShoot()) {
            return false;
        }

        // Out-of-ammo fallback: stop after N ticks with no ammo so recruits can take over.
        if (!hasAnyAmmo()) {
            int fallbackTicks = TriggerMobs.outOfAmmoFallbackTicks;
            if (fallbackTicks <= 0) {
                return false;
            }
            outOfAmmoTicks++;
            return outOfAmmoTicks < fallbackTicks;
        }

        outOfAmmoTicks = 0;
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!mob.isAlive()) return;

        boolean shouldRanged = invokeBoolean(mob, "getShouldRanged", false);
        boolean shouldStrategicFire = invokeBoolean(mob, "getShouldStrategicFire", false);
        BlockPos strategicPos = invokeBlockPos(mob, "getStrategicFirePos");

        ItemStack weaponToUse = getWeaponToUse();
        if (weaponToUse.isEmpty() || !(weaponToUse.getItem() instanceof IWeapon)) {
            return;
        }

        if (!weaponToUse.equals(lastWeaponStack)) {
            WeaponAIStrategy strategy = WeaponStrategyFactory.createStrategy(weaponToUse);
            currentStrategy = strategy != null ? strategy : new GenericWeaponStrategy();
            lastWeaponStack = weaponToUse.copy();
        }
        if (currentStrategy == null) currentStrategy = new GenericWeaponStrategy();

        InteractionHand handToUse = getHandForWeapon(weaponToUse);

        // Do not use EntityReloadTracker: it calls fillAmmo() for mobs (infinite magazine).
        if (!gunHasLoadedAmmo(weaponToUse)) {
            if (reloadCooldown > 0) {
                reloadCooldown--;
            } else {
                reloadCooldown = 8;
                RecruitsNtglInventoryReload.tryReloadFromRecruitInventory(mob, weaponToUse);
            }
            return;
        }

        LivingEntity target = mob.getTarget();

        // Decide aim mode.
        boolean strategic = shouldStrategicFire && strategicPos != null;
        if (strategic) {
            // Strategic fire uses a fixed block position even when there is no target.
            // Strategies themselves only care about mob view yaw/pitch.
            mob.getLookControl().setLookAt(
                    strategicPos.getX() + 0.5D,
                    strategicPos.getY() + 1.0D,
                    strategicPos.getZ() + 0.5D,
                    60.0F,
                    60.0F
            );

            double aimDistSqr = mob.distanceToSqr(
                    strategicPos.getX() + 0.5D,
                    strategicPos.getY() + 1.0D,
                    strategicPos.getZ() + 0.5D
            );
            aimAndMaybeShoot(strategyMaxDistanceSqr(weaponToUse), aimDistSqr, true, handToUse, weaponToUse, mob);
        } else if (shouldRanged && target != null && target.isAlive()) {
            boolean hasLineOfSight = mob.getSensing().hasLineOfSight(target);
            updateSeeTime(hasLineOfSight);

            double distanceSqr = mob.distanceToSqr(target);
            float maxDist = currentStrategy.getMaxDistance();
            double maxDistSqr = maxDist * maxDist;

            aimAndMaybeShoot(maxDistSqr, distanceSqr, hasLineOfSight, handToUse, weaponToUse, target);
        }
    }

    private void updateSeeTime(boolean hasLineOfSight) {
        boolean flag1 = this.seeTime > 0;
        if (hasLineOfSight != flag1) {
            this.seeTime = 0;
        }
        if (hasLineOfSight) ++this.seeTime;
        else --this.seeTime;
    }

    private void aimAndMaybeShoot(double maxDistanceSqr, double distanceSqr, boolean hasLineOfSight, InteractionHand handToUse, ItemStack weaponToUse, LivingEntity targetForShoot) {
        boolean isInRange = distanceSqr <= maxDistanceSqr;

        // Aim: strategies shoot based on current view yaw/pitch, so update LookControl first.
        // Strategic fire uses coords; normal uses the target entity.
        if (targetForShoot != mob) {
            mob.getLookControl().setLookAt(targetForShoot, 60.0F, 60.0F);
        } else {
            // Strategic fire: keep a simple "seen long enough" timer based on range.
            this.seeTime = isInRange ? this.seeTime + 1 : 0;
        }

        mob.setSprinting(false);

        int reactionThreshold = 5 + TriggerMobs.aiReactionDelayTicks;
        if (isInRange && (hasLineOfSight || targetForShoot == mob) && this.seeTime >= reactionThreshold && attackDelay <= 0) {
            currentStrategy.shoot(mob, targetForShoot, handToUse, weaponToUse);

            WeaponData delayData = new WeaponData(weaponToUse, mob).setWeaponMode(WeaponMode.PRIMARY);
            int calculatedDelay = currentStrategy.getAttackDelay(mob, delayData);
            calculatedDelay = Math.max(1, (int) Math.ceil(calculatedDelay * TriggerMobs.fireRateDelayMultiplier));
            attackDelay = Math.max(1, calculatedDelay);

            currentStrategy.updateStrafeCooldown(mob);
        }

        if (attackDelay > 0) {
            attackDelay--;
        }
    }

    private double strategyMaxDistanceSqr(ItemStack weaponToUse) {
        if (currentStrategy == null) return attackRadius * attackRadius;
        float maxDist = currentStrategy.getMaxDistance();
        return maxDist * maxDist;
    }

    private boolean isHoldingGun() {
        return isHoldingGunInEitherHand();
    }

    private boolean isHoldingGunInEitherHand() {
        ItemStack main = mob.getMainHandItem();
        ItemStack off = mob.getOffhandItem();
        return (main.getItem() instanceof IWeapon) || (off.getItem() instanceof IWeapon);
    }

    private ItemStack getWeaponToUse() {
        ItemStack off = mob.getOffhandItem();
        if (off.getItem() instanceof IWeapon) return off;
        ItemStack main = mob.getMainHandItem();
        if (main.getItem() instanceof IWeapon) return main;
        return ItemStack.EMPTY;
    }

    private InteractionHand getHandForWeapon(ItemStack weaponToUse) {
        if (mob.getOffhandItem() == weaponToUse) return InteractionHand.OFF_HAND;
        if (mob.getMainHandItem() == weaponToUse) return InteractionHand.MAIN_HAND;
        // Fallback: prefer off-hand if it is a gun.
        return mob.getOffhandItem().getItem() instanceof IWeapon ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private boolean isRecruitsCommandedToShoot() {
        boolean shouldRanged = invokeBoolean(mob, "getShouldRanged", false);
        boolean shouldStrategicFire = invokeBoolean(mob, "getShouldStrategicFire", false);
        BlockPos strategicPos = invokeBlockPos(mob, "getStrategicFirePos");
        return shouldRanged || (shouldStrategicFire && strategicPos != null);
    }

    private boolean hasAnyAmmo() {
        ItemStack weapon = getWeaponToUse();
        return gunHasLoadedAmmo(weapon);
    }

    /**
     * Same condition as NTGL {@code WeaponStateHelper#hasAmmo(WeaponData)} (see Resources/NukaTeamGunLib).
     * Build {@link WeaponData} with this mob as wielder — required for correct {@link WeaponStateHelper#getAmmoCount}.
     * <p>
     * Note: some NTGL jars only bridge {@code hasAmmo(ItemStack)}; inlining avoids overload mismatch.
     */
    private boolean gunHasLoadedAmmo(ItemStack weapon) {
        if (weapon.isEmpty() || !(weapon.getItem() instanceof IWeapon)) {
            return false;
        }
        var data = new WeaponData(weapon, mob).setWeaponMode(WeaponMode.PRIMARY);
        return WeaponStateHelper.isAmmoIgnored(weapon) || WeaponStateHelper.getAmmoCount(data) > 0;
    }

    private static boolean invokeBoolean(Object instance, String methodName, boolean defaultValue) {
        try {
            Method m = instance.getClass().getMethod(methodName);
            Object result = m.invoke(instance);
            return result instanceof Boolean b ? b : defaultValue;
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    private static BlockPos invokeBlockPos(Object instance, String methodName) {
        try {
            Method m = instance.getClass().getMethod(methodName);
            Object result = m.invoke(instance);
            return (result instanceof BlockPos pos) ? pos : null;
        } catch (Throwable t) {
            return null;
        }
    }
}

