package com.spock117.triggermobs.spawn;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.config.weapon.Modules;
import com.nukateam.ntgl.common.data.attachment.IAttachment;
import com.nukateam.ntgl.common.data.holders.AmmoHolder;
import com.nukateam.ntgl.common.data.holders.AttachmentType;
import com.nukateam.ntgl.common.foundation.init.NtglComponents;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.util.util.FuelUtils;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import com.spock117.triggermobs.config.TriggerMobsConfig;
import com.spock117.triggermobs.util.GunAiDebug;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.Item;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks and assigns Create:Gunsmithing (CGS) weapons to mobs on spawn.
 * Supports single and dual wield, with random attachment assignment.
 */
public class GunPicker {

    private static final String CGS_NAMESPACE = "cgs";
    private static final String TCONSTRUCT_NAMESPACE = "tconstruct";
    private static final String TINKERS_THINGS_NAMESPACE = "tinkers_things";

    // Basic one-handed weapons
    private static final String[] BASIC_WEAPONS = {"flintlock", "revolver", "shotgun"};
    // Advanced weapons (two-handed except shotgun). Nailgun/blazegun omitted here (Create fuel); use mobWeaponOverrides.
    private static final String[] ADVANCED_WEAPONS = {"gatling", "launcher", "hammer"};

    private static final String TAG_CHECKED = "triggermobs_cgs_checked";
    private static final String TAG_APPLIED = "triggermobs_cgs_applied";
    private static final String TC_E_APPLIED = "tconstruct_emergence_applied";

    private static final TagKey<EntityType<?>> VALID_GUN_MOBS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("triggermobs", "valid_gun_mobs"));

    private static final String GUARD_ENTITY_ID = "guardvillagers:guard";

    private static Map<String, List<String>> parsedOverrides;

    /**
     * Returns true if the mob is a guard villager.
     */
    private static boolean isGuard(PathfinderMob mob) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return key != null && GUARD_ENTITY_ID.equals(key.toString());
    }

    private static boolean isTinkersWeaponStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String ns = id.getNamespace();
        return TCONSTRUCT_NAMESPACE.equals(ns) || TINKERS_THINGS_NAMESPACE.equals(ns);
    }

    private static boolean isCgsWeaponStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IWeapon)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && CGS_NAMESPACE.equals(id.getNamespace());
    }

    /**
     * Checks if CGS weapon spawning should be skipped for this mob (e.g. TConstruct-Emergence already equipped).
     * For guards, does not skip when they have TConstruct (allows overwriting with CGS when guard spawning is enabled).
     */
    public static boolean shouldSkipCGSAssignment(PathfinderMob mob) {
        // Skip if TConstruct-Emergence already equipped this mob
        if (mob.getTags().contains(TC_E_APPLIED)) {
            return true;
        }
        // Guards: allow CGS assignment even when holding TConstruct (we may overwrite)
        if (isGuard(mob) && TriggerMobsConfig.COMMON.guardCgsSpawningEnabled.get()) {
            return false;
        }
        ItemStack mainHand = mob.getMainHandItem();
        if (!mainHand.isEmpty() && isTinkersWeaponStack(mainHand)) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether the mob is eligible for CGS weapon assignment (in valid_gun_mobs tag or mobWeaponOverrides).
     * Used by TC-E bow-mob split to decide if we handle the mob at HIGH priority.
     */
    public static boolean isEligibleForCGS(PathfinderMob mob) {
        if (isGuard(mob)) return false;
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        return mob.getType().is(VALID_GUN_MOBS) || getParsedOverrides().containsKey(mobId);
    }

    /**
     * Checks if mob has already been processed by CGS spawning.
     */
    public static boolean isAlreadyChecked(PathfinderMob mob) {
        return mob.getTags().contains(TAG_CHECKED);
    }

    /**
     * Marks mob as checked.
     */
    public static void markChecked(PathfinderMob mob) {
        mob.addTag(TAG_CHECKED);
    }

    /**
     * Marks mob as having received CGS weapon.
     */
    public static void markApplied(PathfinderMob mob) {
        mob.addTag(TAG_APPLIED);
    }

    /**
     * Invalidates the parsed mob weapon override cache. Call on config reload.
     */
    public static void invalidateWeaponOverrideCache() {
        parsedOverrides = null;
    }

    /**
     * Tries to assign a CGS weapon to the mob. Returns true if a weapon was assigned.
     * Guards use {@code guardCgsSpawningEnabled}, {@code guardWeaponChance}, and guard weapon pool from {@code mobWeaponOverrides}.
     */
    public static boolean tryAssignWeapon(PathfinderMob mob) {
        return tryAssignWeapon(mob, false);
    }

    /**
     * Same as tryAssignWeapon but when skipChanceRoll is true, the weaponChance roll is skipped for non-guards.
     * Used when the caller has already decided this mob should get a CGS weapon (e.g. TC-E bow-mob split).
     */
    public static boolean tryAssignWeapon(PathfinderMob mob, boolean skipChanceRoll) {
        if (!TriggerMobsConfig.COMMON.cgsSpawningEnabled.get()) {
            return false;
        }
        ResourceLocation typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (typeKey == null) {
            return false;
        }
        String mobId = typeKey.toString();
        boolean isGuard = isGuard(mob);

        if (isGuard) {
            if (!TriggerMobsConfig.COMMON.guardCgsSpawningEnabled.get()) {
                return false;
            }
            if (mob.getRandom().nextDouble() > TriggerMobsConfig.COMMON.guardWeaponChance.get()) {
                return false;
            }
        } else {
            if (!skipChanceRoll && mob.getRandom().nextDouble() > TriggerMobsConfig.COMMON.weaponChance.get()) {
                return false;
            }
            boolean inTag = mob.getType().is(VALID_GUN_MOBS);
            boolean inOverride = getParsedOverrides().containsKey(mobId);
            if (!inTag && !inOverride) {
                return false;
            }
        }

        ItemStack mainHand = mob.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof IWeapon) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(mainHand.getItem());
            if (id != null && CGS_NAMESPACE.equals(id.getNamespace())) {
                // Already holding CGS — still prep ammo/fuel so loot/datapack guns can fire
                prepareMobWeapon(mainHand, mob);
                if (!mob.getOffhandItem().isEmpty() && isCgsWeaponStack(mob.getOffhandItem())) {
                    prepareMobWeapon(mob.getOffhandItem(), mob);
                }
                markApplied(mob);
                GunAiDebug.logSpawn(mob, mainHand, "already_holding");
                return true;
            }
        }

        Item weaponItem = pickWeapon(mob);
        if (weaponItem == null || weaponItem == Items.AIR) {
            return false;
        }

        ItemStack weaponStack = new ItemStack(weaponItem);
        applyRandomAttachments(weaponStack, mob);
        prepareMobWeapon(weaponStack, mob);

        double dropChance = TriggerMobsConfig.COMMON.dropChanceOverride.get();
        boolean isOneHanded = WeaponModifierHelper.isOneHanded(new WeaponData(weaponStack, mob));

        // Dual wield: same weapon in both hands (InControl-style)
        if (isOneHanded && mob.getRandom().nextDouble() < TriggerMobsConfig.COMMON.dualWieldChance.get()) {
            ItemStack secondStack = new ItemStack(weaponItem);
            applyRandomAttachments(secondStack, mob);
            prepareMobWeapon(secondStack, mob);

            mob.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
            mob.setItemInHand(InteractionHand.OFF_HAND, secondStack);
            mob.setDropChance(EquipmentSlot.MAINHAND, (float) dropChance);
            mob.setDropChance(EquipmentSlot.OFFHAND, (float) dropChance);
            markApplied(mob);
            return true;
        }

        mob.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
        mob.setDropChance(EquipmentSlot.MAINHAND, (float) dropChance);
        markApplied(mob);
        return true;
    }

    private static Item pickWeapon(PathfinderMob mob) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String mobId = key != null ? key.toString() : "";
        List<String> pool = getWeaponPoolForMob(mobId);
        if (pool.isEmpty()) {
            return null;
        }
        List<Item> candidates = new ArrayList<>();
        for (String entry : pool) {
            ResourceLocation weaponId = ResourceLocation.tryParse(entry);
            if (weaponId == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(weaponId);
            if (item != null && item != Items.AIR) {
                candidates.add(item);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(mob.getRandom().nextInt(candidates.size()));
    }

    private static List<String> getWeaponPoolForMob(String mobId) {
        Map<String, List<String>> overrides = getParsedOverrides();
        if (overrides.containsKey(mobId)) {
            return overrides.get(mobId);
        }
        return getDefaultWeaponPool();
    }

    private static List<String> getDefaultWeaponPool() {
        boolean allowAdvanced = TriggerMobsConfig.COMMON.allowAdvancedWeapons.get();
        List<String> pool = new ArrayList<>();
        for (String id : BASIC_WEAPONS) {
            pool.add(CGS_NAMESPACE + ":" + id);
        }
        if (allowAdvanced) {
            for (String id : ADVANCED_WEAPONS) {
                pool.add(CGS_NAMESPACE + ":" + id);
            }
        }
        return pool;
    }

    private static Map<String, List<String>> getParsedOverrides() {
        List<? extends String> config = TriggerMobsConfig.COMMON.mobWeaponOverrides.get();
        if (config == null) return Map.of();
        if (parsedOverrides != null) return parsedOverrides;
        parsedOverrides = new HashMap<>();
        for (String entry : config) {
            if (entry == null || !entry.contains("=")) continue;
            int eq = entry.indexOf('=');
            String mobId = entry.substring(0, eq).trim();
            String gunsStr = entry.substring(eq + 1).trim();
            if (mobId.isEmpty() || gunsStr.isEmpty()) continue;
            List<String> guns = new ArrayList<>();
            for (String g : gunsStr.split(",")) {
                String gun = g.trim();
                if (!gun.isEmpty()) guns.add(gun);
            }
            if (!guns.isEmpty()) parsedOverrides.put(mobId, guns);
        }
        return parsedOverrides;
    }

    private static void applyRandomAttachments(ItemStack weaponStack, PathfinderMob mob) {
        int maxSlots = TriggerMobsConfig.COMMON.maxAttachmentSlots.get();
        if (maxSlots <= 0) return;
        if (!(weaponStack.getItem() instanceof IWeapon)) return;

        WeaponData data = new WeaponData(weaponStack, null);
        var config = WeaponModifierHelper.getConfig(data);
        if (config == null) return;

        Map<AttachmentType, ArrayList<Modules.Attachment>> attachments = config.getModules().getAttachments();
        if (attachments == null || attachments.isEmpty()) return;

        List<Map.Entry<AttachmentType, ArrayList<Modules.Attachment>>> slots = new ArrayList<>(attachments.entrySet());
        shuffleList(slots, mob.getRandom());

        int applied = 0;
        List<ItemStack> appliedAttachments = new ArrayList<>();
        for (Map.Entry<AttachmentType, ArrayList<Modules.Attachment>> entry : slots) {
            if (applied >= maxSlots) break;
            ArrayList<Modules.Attachment> options = entry.getValue();
            if (options == null || options.isEmpty()) continue;

            Modules.Attachment option = options.get(mob.getRandom().nextInt(options.size()));
            ResourceLocation itemId = option.getItemId();
            if (itemId == null) continue;

            Item attachmentItem = BuiltInRegistries.ITEM.get(itemId);
            if (attachmentItem == null || attachmentItem == Items.AIR) continue;
            if (!(attachmentItem instanceof IAttachment<?>)) continue;

            ItemStack attachmentStack = new ItemStack(attachmentItem);
            appliedAttachments.add(attachmentStack);
            applied++;
        }
        if (!appliedAttachments.isEmpty()) {
            WeaponStateHelper.writeAttachments(appliedAttachments, new WeaponData(weaponStack, mob));
        }
    }

    /**
     * Marks mob-spawned guns as ignoring magazine depletion and tops up ammo plus Create/CGS fuel tanks.
     */
    private static void prepareMobWeapon(ItemStack weaponStack, PathfinderMob mob) {
        setInfiniteAmmo(weaponStack);
        WeaponData data = new WeaponData(weaponStack, mob);
        WeaponStateHelper.fillAmmo(data);
        seedMobFuels(data);
        GunAiDebug.logSpawn(mob, weaponStack, "prepared");
    }

    private static void setInfiniteAmmo(ItemStack weaponStack) {
        weaponStack.set(NtglComponents.IGNORE_AMMO, true);
    }

    /** Fill every fuel slot to max so NTGL/CGS mandatory-fuel checks pass for mob shooters. */
    private static void seedMobFuels(WeaponData data) {
        for (AmmoHolder fuelType : WeaponModifierHelper.getAllFuel(data)) {
            int max = WeaponModifierHelper.getMaxFuel(fuelType.getId(), data);
            int current = FuelUtils.getFuel(data.weapon, fuelType);
            if (current < max) {
                FuelUtils.addFuel(data, fuelType, max - current);
            }
        }
    }

    private static <T> void shuffleList(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
