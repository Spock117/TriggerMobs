package com.spock117.triggermobs.ai;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.holders.AttachmentType;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps a base weapon strategy and adjusts behavior based on attachments.
 * Scope: increases max distance. Drum/auto: reduces attack delay.
 */
public class AttachmentAwareStrategy implements WeaponAIStrategy {

    private final WeaponAIStrategy base;
    private final ItemStack weapon;

    public AttachmentAwareStrategy(WeaponAIStrategy base, ItemStack weapon) {
        this.base = base;
        this.weapon = weapon;
    }

    @Override
    public void move(PathfinderMob mob, LivingEntity target, double distance, double distanceSqr,
                     boolean hasAmmo, boolean canReload, boolean hasLineOfSight, int seeTime) {
        base.move(mob, target, distance, distanceSqr, hasAmmo, canReload, hasLineOfSight, seeTime);
    }

    @Override
    public void shoot(PathfinderMob mob, LivingEntity target, InteractionHand hand, ItemStack weapon) {
        base.shoot(mob, target, hand, weapon);
    }

    @Override
    public int getAttackDelay(PathfinderMob mob, WeaponData weaponData) {
        int delay = base.getAttackDelay(mob, weaponData);
        if (WeaponStateHelper.hasAttachmentEquipped(weapon, AttachmentType.MAGAZINE)) {
            delay = Math.max(1, (int) (delay * 0.7));
        }
        return delay;
    }

    @Override
    public float getIdealDistance() {
        float ideal = base.getIdealDistance();
        if (WeaponStateHelper.hasAttachmentEquipped(weapon, AttachmentType.SCOPE)) {
            ideal *= 1.25f;
        }
        return ideal;
    }

    @Override
    public float getMinDistance() {
        return base.getMinDistance();
    }

    @Override
    public float getMaxDistance() {
        float max = base.getMaxDistance();
        if (WeaponStateHelper.hasAttachmentEquipped(weapon, AttachmentType.SCOPE)) {
            max *= 1.25f;
        }
        return max;
    }

    @Override
    public boolean canDualWield() {
        return base.canDualWield();
    }

    @Override
    public boolean shouldMaintainDistance() {
        return base.shouldMaintainDistance();
    }

    @Override
    public void updateStrafeCooldown(PathfinderMob mob) {
        base.updateStrafeCooldown(mob);
    }
}
