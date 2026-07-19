package com.spock117.triggermobs.util;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.holders.AmmoHolder;
import com.nukateam.ntgl.common.network.ServerPlayHandler;
import com.nukateam.ntgl.common.network.message.weapon.C2SMessageShoot;
import com.nukateam.ntgl.common.util.util.FuelUtils;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import com.spock117.triggermobs.TriggerMobs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optional diagnostic logging for "chases but never fires" and CGS spawn ammo/fuel prep.
 * Enable via {@code debug.debugGunAi} / {@code debug.debugGunSpawn} in triggermobs-common.toml.
 */
public final class GunAiDebug {
    private static final int TICK_INTERVAL = 40; // ~2 seconds
    private static final Map<UUID, Integer> lastTickLog = new HashMap<>();
    private static final Map<UUID, AtomicInteger> shotCounts = new HashMap<>();

    private GunAiDebug() {}

    public static boolean gunAi() {
        return TriggerMobs.debugGunAi;
    }

    public static boolean gunSpawn() {
        return TriggerMobs.debugGunSpawn;
    }

    public static void logSpawn(LivingEntity mob, ItemStack weapon, String phase) {
        if (!gunSpawn() || weapon == null || weapon.isEmpty()) {
            return;
        }
        WeaponData data = new WeaponData(weapon, mob);
        boolean ignore = WeaponStateHelper.isAmmoIgnored(data);
        TriggerMobs.LOGGER.info("[TM-spawn] {} {} weapon={} ammo={} ignoreAmmo={} fuels=[{}] {}{}",
                mobLabel(mob),
                phase,
                itemId(weapon),
                WeaponStateHelper.getAmmoCount(data),
                ignore,
                fuelSummary(data),
                FuelUtils.hasFuel(data, false) ? "fuelOk" : "FUEL_BLOCK",
                ignore ? " (magazine should never empty)" : " (magazine WILL empty — expect reload)");
    }

    /**
     * Throttled status when a mob is in gun AI but did not shoot this tick.
     */
    public static void logBlocked(Mob mob, ItemStack weapon, String reason, Object... args) {
        if (!gunAi() || mob == null || !shouldLog(mob)) {
            return;
        }
        String detail = args.length == 0 ? reason : String.format(reason, args);
        WeaponData data = weapon != null && !weapon.isEmpty() ? new WeaponData(weapon, mob) : null;
        int shots = shotCount(mob.getUUID());
        String ammoNote = "";
        if (data != null) {
            boolean ignore = WeaponStateHelper.isAmmoIgnored(data);
            int ammo = WeaponStateHelper.getAmmoCount(data);
            if (detail.startsWith("no_ammo") || detail.startsWith("reloading")) {
                ammoNote = ignore
                        ? " | WARN: ignoreAmmo=true but AI thinks empty/reloading"
                        : " | expected: ignoreAmmo=false so magazine depletes";
            } else if (detail.startsWith("attack_delay")) {
                ammoNote = " | NOT out of ammo — waiting between shots (fireRateDelayMultiplier="
                        + TriggerMobs.fireRateDelayMultiplier + ")";
            }
            TriggerMobs.LOGGER.info("[TM-ai] {} BLOCKED: {} | shotsFired={} weapon={} ammo={} ignoreAmmo={} fuels=[{}] seeTarget={}{}",
                    mobLabel(mob),
                    detail,
                    shots,
                    itemId(weapon),
                    ammo,
                    ignore,
                    fuelSummary(data),
                    mob.getTarget() != null ? mob.getTarget().getName().getString() : "none",
                    ammoNote);
        } else {
            TriggerMobs.LOGGER.info("[TM-ai] {} BLOCKED: {} | shotsFired={} weapon=none",
                    mobLabel(mob), detail, shots);
        }
    }

    public static void logNextShotDelay(Mob mob, int delayTicks) {
        if (!gunAi() || mob == null) {
            return;
        }
        double seconds = delayTicks / 20.0;
        TriggerMobs.LOGGER.info("[TM-ai] {} NEXT_SHOT_IN {} ticks (~{}s) — mob will chase but not fire until then (not ammo)",
                mobLabel(mob), delayTicks, String.format("%.1f", seconds));
    }

    public static void handleShoot(C2SMessageShoot msg, LivingEntity shooter, ItemStack weapon, String strategy) {
        int ammoBefore = -1;
        boolean ignoreBefore = false;
        boolean fuelBefore = true;
        if (gunAi() && shooter instanceof Mob && weapon != null && !weapon.isEmpty()) {
            WeaponData data = new WeaponData(weapon, shooter);
            ammoBefore = WeaponStateHelper.getAmmoCount(data);
            ignoreBefore = WeaponStateHelper.isAmmoIgnored(data);
            fuelBefore = FuelUtils.hasFuel(data, false);
            int n = shotCounts.computeIfAbsent(shooter.getUUID(), u -> new AtomicInteger()).incrementAndGet();
            TriggerMobs.LOGGER.info("[TM-ai] {} SHOOT#{} via {} weapon={} ammoBefore={} ignoreAmmo={} fuels=[{}] {}",
                    mobLabel(shooter),
                    n,
                    strategy,
                    itemId(weapon),
                    ammoBefore,
                    ignoreBefore,
                    fuelSummary(data),
                    fuelBefore ? "fuelOk" : "FUEL_WILL_CANCEL");
        }
        try {
            ServerPlayHandler.handleShoot(msg, shooter);
        } catch (Exception e) {
            if (gunAi()) {
                TriggerMobs.LOGGER.error("[TM-ai] {} handleShoot threw for {}: {}",
                        mobLabel(shooter), itemId(weapon), e.toString());
            }
            return;
        }
        if (gunAi() && weapon != null && !weapon.isEmpty()) {
            WeaponData after = new WeaponData(weapon, shooter);
            int ammoAfter = WeaponStateHelper.getAmmoCount(after);
            boolean fuelAfter = FuelUtils.hasFuel(after, false);
            if (ignoreBefore && ammoAfter < ammoBefore) {
                TriggerMobs.LOGGER.warn("[TM-ai] {} ammo fell {}→{} despite ignoreAmmo=true (NTGL bug or component lost)",
                        mobLabel(shooter), ammoBefore, ammoAfter);
            } else if (!ignoreBefore && ammoAfter < ammoBefore) {
                TriggerMobs.LOGGER.info("[TM-ai] {} ammo depleted {}→{} (ignoreAmmo=false — will reload when empty)",
                        mobLabel(shooter), ammoBefore, ammoAfter);
            } else if (ignoreBefore) {
                TriggerMobs.LOGGER.info("[TM-ai] {} ammo unchanged {} (ignoreAmmo working)",
                        mobLabel(shooter), ammoAfter);
            }
            if (fuelBefore && !fuelAfter) {
                TriggerMobs.LOGGER.warn("[TM-ai] {} fuel ran out after this shot — further shots will be cancelled until refilled",
                        mobLabel(shooter));
            } else if (!fuelAfter) {
                TriggerMobs.LOGGER.warn("[TM-ai] {} still missing mandatory fuel after shot attempt",
                        mobLabel(shooter));
            }
        }
    }

    private static int shotCount(UUID id) {
        AtomicInteger n = shotCounts.get(id);
        return n == null ? 0 : n.get();
    }

    private static boolean shouldLog(Mob mob) {
        int now = mob.tickCount;
        Integer last = lastTickLog.get(mob.getUUID());
        if (last != null && now - last < TICK_INTERVAL) {
            return false;
        }
        lastTickLog.put(mob.getUUID(), now);
        if (lastTickLog.size() > 256) {
            lastTickLog.clear();
            shotCounts.clear();
        }
        return true;
    }

    private static String mobLabel(LivingEntity entity) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return (type != null ? type : "unknown") + "#" + entity.getId();
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : stack.getItem().toString();
    }

    private static String fuelSummary(WeaponData data) {
        StringBuilder sb = new StringBuilder();
        for (AmmoHolder fuel : WeaponModifierHelper.getAllFuel(data)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            int current = FuelUtils.getFuel(data.weapon, fuel);
            int max = WeaponModifierHelper.getMaxFuel(fuel.getId(), data);
            boolean mandatory = WeaponModifierHelper.isFuelMandatory(fuel.getId(), data);
            sb.append(fuel.getId()).append('=').append(current).append('/').append(max);
            if (mandatory) {
                sb.append("*");
            }
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }
}
