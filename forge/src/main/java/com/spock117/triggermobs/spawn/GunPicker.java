package com.spock117.triggermobs.spawn;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.holders.AmmoHolder;
import com.nukateam.ntgl.common.data.holders.WeaponMode;
import com.nukateam.ntgl.common.data.config.weapon.Modules;
import com.nukateam.ntgl.common.data.attachment.IAttachment;
import com.nukateam.ntgl.common.data.holders.AttachmentType;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import com.spock117.triggermobs.config.TriggerMobsConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Picks and assigns Create:Gunsmithing (CGS) weapons to mobs on spawn.
 * Supports single and dual wield, with random attachment assignment.
 */
public class GunPicker {

    private static final String CGS_NAMESPACE = "cgs";
    private static final String TCONSTRUCT_NAMESPACE = "tconstruct";
    private static final String TINKERS_THINGS_NAMESPACE = "tinkers_things";
    private static final String RECRUITS_MOD_NAMESPACE = "recruits";
    private static final String RECRUITS_CROSSBOWMAN_ID = "recruits:crossbowman";

    // Basic one-handed weapons
    private static final String[] BASIC_WEAPONS = {"flintlock", "revolver", "shotgun"};
    // Advanced weapons (two-handed except shotgun). Nailgun excluded: requires Create fuel. Blazegun excluded: not usable by mobs.
    private static final String[] ADVANCED_WEAPONS = {"gatling", "launcher", "hammer"};

    private static final String TAG_CHECKED = "triggermobs_cgs_checked";
    private static final String TAG_APPLIED = "triggermobs_cgs_applied";
    private static final String TC_E_APPLIED = "tconstruct_emergence_applied";

    private static final TagKey<EntityType<?>> VALID_GUN_MOBS = TagKey.create(
            Registries.ENTITY_TYPE, new ResourceLocation("triggermobs", "valid_gun_mobs"));

    private static final String GUARD_ENTITY_ID = "guardvillagers:guard";

    private static Map<String, List<String>> parsedOverrides;

    /**
     * Returns true if the mob is a guard villager.
     */
    private static boolean isGuard(PathfinderMob mob) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return key != null && GUARD_ENTITY_ID.equals(key.toString());
    }

    private static boolean isRecruitsCrossbowman(PathfinderMob mob) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return key != null && RECRUITS_CROSSBOWMAN_ID.equals(key.toString());
    }

    /** Villager Recruits entity types ({@code recruits:*}). */
    private static boolean isRecruitsEntity(PathfinderMob mob) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return key != null && RECRUITS_MOD_NAMESPACE.equals(key.getNamespace());
    }

    private static boolean isTinkersWeaponStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
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
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && CGS_NAMESPACE.equals(id.getNamespace());
    }

    /**
     * Main-hand items that {@code recruitsTinkersReplaceWithCgs} may replace with a CGS gun (vanilla melee/ranged or
     * tconstruct / tinkers_things tools), excluding stacks that are already CGS weapons.
     */
    private static boolean isRecruitMainHandCgsReplaceCandidate(ItemStack stack) {
        if (stack.isEmpty() || isCgsWeaponStack(stack)) {
            return false;
        }
        if (isTinkersWeaponStack(stack)) {
            return true;
        }
        Item item = stack.getItem();
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
            return true;
        }
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) {
            return true;
        }
        // Fallback for modded swords/axes not in vanilla tags
        return item instanceof SwordItem || item instanceof AxeItem;
    }

    /**
     * Checks if CGS weapon spawning should be skipped for this mob (e.g. TConstruct-Emergence already equipped).
     * For guards, does not skip when they have TConstruct (allows overwriting with CGS when guard spawning is enabled).
     * For {@code recruits:*} with a Tinkers main hand, does not skip when {@code recruitsTinkersReplaceWithCgs} is on so
     * {@link #tryAssignWeapon} can evaluate replacement. (Vanilla recruit weapons do not force a skip here.)
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
            if (isRecruitsEntity(mob) && TriggerMobsConfig.COMMON.recruitsTinkersReplaceWithCgs.get()) {
                return false;
            }
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
        String mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString();
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
     * Villager Recruits with a vanilla or Tinkers main-hand weapon use {@code recruitsTinkersReplaceWithCgs} and
     * {@code recruitsTinkersReplaceChance} to optionally replace main hand with a CGS gun.
     */
    public static boolean tryAssignWeapon(PathfinderMob mob) {
        return tryAssignWeapon(mob, false);
    }

    /**
     * Same as tryAssignWeapon but when skipChanceRoll is true, the weaponChance roll is skipped for non-guards.
     * The recruit main-hand replacement path rolls {@code recruitsTinkersReplaceChance} when applicable and ignores
     * skipChanceRoll for that roll; if the roll fails, normal {@code weaponChance} / tag logic still runs.
     * Used when the caller has already decided this mob should get a CGS weapon (e.g. TC-E bow-mob split).
     */
    public static boolean tryAssignWeapon(PathfinderMob mob, boolean skipChanceRoll) {
        if (!TriggerMobsConfig.COMMON.cgsSpawningEnabled.get()) {
            return false;
        }
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (typeKey == null) {
            return false;
        }
        String mobId = typeKey.toString();
        boolean isGuard = isGuard(mob);
        boolean tinkersRecruitReplace = false;

        if (isGuard) {
            if (!TriggerMobsConfig.COMMON.guardCgsSpawningEnabled.get()) {
                return false;
            }
            if (mob.getRandom().nextDouble() > TriggerMobsConfig.COMMON.guardWeaponChance.get()) {
                return false;
            }
        } else {
            if (isRecruitsEntity(mob) && TriggerMobsConfig.COMMON.recruitsTinkersReplaceWithCgs.get()) {
                ItemStack mainProbe = mob.getMainHandItem();
                if (isRecruitMainHandCgsReplaceCandidate(mainProbe)
                        && mob.getRandom().nextDouble() <= TriggerMobsConfig.COMMON.recruitsTinkersReplaceChance.get()) {
                    tinkersRecruitReplace = true;
                }
            }
            if (!tinkersRecruitReplace) {
                if (!skipChanceRoll && mob.getRandom().nextDouble() > TriggerMobsConfig.COMMON.weaponChance.get()) {
                    return false;
                }
                boolean inTag = mob.getType().is(VALID_GUN_MOBS);
                boolean inOverride = getParsedOverrides().containsKey(mobId);
                if (!inTag && !inOverride) {
                    return false;
                }
            }
        }

        ItemStack mainHand = mob.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof IWeapon) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
            if (id != null && CGS_NAMESPACE.equals(id.getNamespace())) {
                // CGS default / prior tagging may still set IgnoreAmmo; strip for recruits using inventory ammo.
                if (isRecruitsEntity(mob)) {
                    stripIgnoreAmmo(mainHand);
                    tryGiveRecruitInventoryAmmo(mob, mainHand);
                }
                markApplied(mob);
                return true;
            }
        }

        Item weaponItem = pickWeapon(mob);
        if (weaponItem == null || weaponItem == Items.AIR) {
            return false;
        }

        ItemStack weaponStack = new ItemStack(weaponItem);
        applyRandomAttachments(weaponStack, mob);
        if (!isRecruitsEntity(mob)) {
            setInfiniteAmmo(weaponStack);
        } else {
            // Item defaults or attachments must not leave IgnoreAmmo or shots never consume ammo for recruits
            // (see ServerPlayHandler.handleShoot).
            stripIgnoreAmmo(weaponStack);
        }
        WeaponStateHelper.fillAmmo(new WeaponData(weaponStack, mob));

        double dropChance = TriggerMobsConfig.COMMON.dropChanceOverride.get();
        boolean isOneHanded = WeaponModifierHelper.isOneHanded(new WeaponData(weaponStack, mob));

        // recruits crossbowman: vanilla crossbow in main -> CGS gun in offhand unless main-replacement roll already
        // placed a CGS gun in main. Do not dual-wield recruits.
        if (isRecruitsCrossbowman(mob) && !tinkersRecruitReplace) {
            if (!mob.getOffhandItem().isEmpty()) {
                return false;
            }
            mob.setItemInHand(InteractionHand.OFF_HAND, weaponStack);
            mob.setDropChance(EquipmentSlot.OFFHAND, (float) dropChance);
            tryGiveRecruitInventoryAmmo(mob, mob.getOffhandItem());
            markApplied(mob);
            return true;
        }

        // Dual wield: same weapon in both hands (InControl-style); not used for recruits
        if (isOneHanded && !isRecruitsEntity(mob) && mob.getRandom().nextDouble() < TriggerMobsConfig.COMMON.dualWieldChance.get()) {
            ItemStack secondStack = new ItemStack(weaponItem);
            applyRandomAttachments(secondStack, mob);
            setInfiniteAmmo(secondStack);
            WeaponStateHelper.fillAmmo(new WeaponData(secondStack, mob));

            mob.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
            mob.setItemInHand(InteractionHand.OFF_HAND, secondStack);
            mob.setDropChance(EquipmentSlot.MAINHAND, (float) dropChance);
            mob.setDropChance(EquipmentSlot.OFFHAND, (float) dropChance);
            markApplied(mob);
            return true;
        }

        mob.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
        mob.setDropChance(EquipmentSlot.MAINHAND, (float) dropChance);
        if (isRecruitsEntity(mob)) {
            tryGiveRecruitInventoryAmmo(mob, mob.getMainHandItem());
        }
        markApplied(mob);
        return true;
    }

    private static Item pickWeapon(PathfinderMob mob) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        String mobId = key != null ? key.toString() : "";
        List<String> pool = getWeaponPoolForMob(mobId);
        if (pool.isEmpty()) return null;
        String chosen = pool.get(mob.getRandom().nextInt(pool.size()));
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(chosen));
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
        for (Map.Entry<AttachmentType, ArrayList<Modules.Attachment>> entry : slots) {
            if (applied >= maxSlots) break;
            ArrayList<Modules.Attachment> options = entry.getValue();
            if (options == null || options.isEmpty()) continue;

            Modules.Attachment option = options.get(mob.getRandom().nextInt(options.size()));
            ResourceLocation itemId = option.getItemId();
            if (itemId == null) continue;

            Item attachmentItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (attachmentItem == null || attachmentItem == Items.AIR) continue;
            if (!(attachmentItem instanceof IAttachment<?>)) continue;

            ItemStack attachmentStack = new ItemStack(attachmentItem);
            WeaponStateHelper.saveAttachment(weaponStack, attachmentStack);
            applied++;
        }
    }

    /**
     * Adds reserve ammo to a recruit's inventory (Recruits {@code getInventory().addItem}) for CGS/NTGL reload.
     * Magazine is filled separately via {@link WeaponStateHelper#fillAmmo}.
     */
    private static void tryGiveRecruitInventoryAmmo(PathfinderMob mob, ItemStack gunStack) {
        if (!isRecruitsEntity(mob) || gunStack.isEmpty() || !(gunStack.getItem() instanceof IWeapon)) {
            return;
        }
        if (TriggerMobsConfig.COMMON.recruitsCgsSpawnAmmoAmount == null) {
            return;
        }
        int total = TriggerMobsConfig.COMMON.recruitsCgsSpawnAmmoAmount.get();
        if (total <= 0 || WeaponStateHelper.isAmmoIgnored(gunStack)) {
            return;
        }
        WeaponData data = new WeaponData(gunStack, mob).setWeaponMode(WeaponMode.PRIMARY);
        Set<AmmoHolder> ammoItems = WeaponModifierHelper.getAmmoItems(data);
        if (ammoItems == null || ammoItems.isEmpty()) {
            return;
        }
        AmmoHolder holder = WeaponStateHelper.getCurrentAmmo(data);
        Item ammoItem = ForgeRegistries.ITEMS.getValue(holder.getId());
        if (ammoItem == null || ammoItem == Items.AIR) {
            return;
        }
        Object inventory;
        Method addItemMethod;
        try {
            inventory = mob.getClass().getMethod("getInventory").invoke(mob);
            addItemMethod = inventory.getClass().getMethod("addItem", ItemStack.class);
        } catch (Throwable t) {
            return;
        }
        int remaining = total;
        int maxStack = Math.max(1, new ItemStack(ammoItem).getMaxStackSize());
        for (int attempts = 0; remaining > 0 && attempts < 64; attempts++) {
            int n = Math.min(remaining, maxStack);
            ItemStack toAdd = new ItemStack(ammoItem, n);
            try {
                ItemStack leftover = (ItemStack) addItemMethod.invoke(inventory, toAdd);
                int notAdded = leftover == null ? 0 : leftover.getCount();
                int added = n - notAdded;
                if (added <= 0) {
                    break;
                }
                remaining -= added;
            } catch (Throwable t) {
                break;
            }
        }
    }

    private static void setInfiniteAmmo(ItemStack weaponStack) {
        weaponStack.getOrCreateTag().putBoolean("IgnoreAmmo", true);
    }

    /** NTGL skips {@link WeaponStateHelper#consumeAmmo} when this tag is present. */
    private static void stripIgnoreAmmo(ItemStack weaponStack) {
        if (weaponStack.isEmpty()) {
            return;
        }
        var tag = weaponStack.getTag();
        if (tag != null && tag.contains("IgnoreAmmo", Tag.TAG_BYTE)) {
            tag.remove("IgnoreAmmo");
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
