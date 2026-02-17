package com.spock117.triggermobs.events;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.config.TriggerMobsConfig;
import com.spock117.triggermobs.goals.MobGunAttackGoal;
import com.spock117.triggermobs.spawn.GunPicker;
import com.spock117.triggermobs.util.ModDetectionHelper;
import com.spock117.triggermobs.util.MobItemPickupHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = TriggerMobs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TriggerMobsEvents {
    
    // Set of mob registry names that should have CanPickUpLoot enabled
    private static final Set<String> MOBS_WITH_LOOT_PICKUP = Set.of(
        "minecraft:zombie",
        "minecraft:zombie_villager",
        "minecraft:husk",
        "minecraft:drowned",
        "minecraft:skeleton",
        "minecraft:stray",
        "minecraft:wither_skeleton",
        "minecraft:pillager",
        "minecraft:vindicator",
        "minecraft:evoker",
        "minecraft:witch",
        "minecraft:villager",
        "minecraft:piglin",
        "minecraft:piglin_brute",
        "minecraft:zombified_piglin",
        "guardvillagers:guard"
    );

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only handle on server side (works in both single-player and multiplayer)
        // In single-player, Minecraft runs an integrated server, so this code still executes
        // We skip client-side only to avoid duplicate goal registration
        // LOW priority so we run after TConstruct-Emergence (avoids conflicts)
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        // Enable CanPickUpLoot for specified mobs
        if (event.getEntity() instanceof Mob mob) {
            ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
            if (entityTypeKey != null && MOBS_WITH_LOOT_PICKUP.contains(entityTypeKey.toString())) {
                mob.setCanPickUpLoot(true);
            }
        }
        
        // CGS gun spawning: equip mobs with Create:Gunsmithing weapons (after TC-E to avoid conflicts)
        if (event.getEntity() instanceof PathfinderMob pathfinderMob) {
            if (!pathfinderMob.getPersistentData().getBoolean("apoth.boss")) {
                if (GunPicker.isAlreadyChecked(pathfinderMob)) {
                    // Already processed - goal is already added by Monster/Guard block below
                } else {
                    GunPicker.markChecked(pathfinderMob);
                    if (!GunPicker.shouldSkipCGSAssignment(pathfinderMob)
                            && ModDetectionHelper.isCreateGunsmithingLoaded()) {
                        GunPicker.tryAssignWeapon(pathfinderMob);
                    }
                }
            }
        }
        
        // Only add goal to hostile mobs (Monster)
        if (event.getEntity() instanceof Monster monster) {
            try {
                MobGunAttackGoal goal = new MobGunAttackGoal(monster, 0.6D, 16.0F); // Reduced speed: 0.6 instead of 1.0
                monster.goalSelector.addGoal(3, goal);
            } catch (Exception e) {
                // Silently handle errors
            }
        }

        // Guard Villagers: when mod is loaded, add gun goal to guards (priority 2 so it runs before melee/ranged)
        if (ModDetectionHelper.isGuardVillagersLoaded() && event.getEntity() instanceof PathfinderMob pathfinderMob
                && "guardvillagers:guard".equals(ForgeRegistries.ENTITY_TYPES.getKey(pathfinderMob.getType()).toString())) {
            try {
                MobGunAttackGoal goal = new MobGunAttackGoal(pathfinderMob, 0.6D, 16.0F);
                pathfinderMob.goalSelector.addGoal(2, goal);
            } catch (Exception e) {
                // Silently handle errors
            }
        }
    }

    private static final String CGS_NAMESPACE = "cgs";
    private static final String TAG_APPLIED = "triggermobs_cgs_applied";

    /**
     * Force CGS weapon drops using dropChanceOverride so drops are reliable regardless of
     * vanilla/Guard loot logic. Removes any existing drop of the same weapon to avoid duplicates,
     * then adds our drop if the roll passes.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof PathfinderMob) || !entity.getTags().contains(TAG_APPLIED)) {
            return;
        }
        if (!ModDetectionHelper.isCreateGunsmithingLoaded()) {
            return;
        }
        // Guards drop from guardInventory in their own dropCustomDeathLoot; we don't add here to avoid double drop
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId != null && "guardvillagers:guard".equals(typeId.toString())) {
            return;
        }
        double dropChance = TriggerMobsConfig.COMMON.dropChanceOverride.get();
        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();
        if (mainHand.isEmpty() && offHand.isEmpty()) {
            return;
        }
        var drops = event.getDrops();
        // Remove any existing drop that matches our CGS weapons (e.g. from Guard's dropCustomDeathLoot)
        drops.removeIf(itemEntity -> {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) return false;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !CGS_NAMESPACE.equals(id.getNamespace())) return false;
            return ItemStack.isSameItemSameTags(stack, mainHand) || ItemStack.isSameItemSameTags(stack, offHand);
        });
        var random = entity.getRandom();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        Level level = entity.level();
        if (!mainHand.isEmpty() && isCgsWeapon(mainHand) && random.nextDouble() < dropChance) {
            drops.add(new ItemEntity(level, x, y, z, mainHand.copy()));
            entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (!offHand.isEmpty() && isCgsWeapon(offHand) && random.nextDouble() < dropChance) {
            drops.add(new ItemEntity(level, x, y, z, offHand.copy()));
            entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static boolean isCgsWeapon(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IWeapon)) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && CGS_NAMESPACE.equals(id.getNamespace());
    }
    
    /**
     * Handle mob item pickup behavior using TickEvent.
     * This runs every server tick to check mobs and manage their items.
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // Only run on server side and during END phase
        if (event.level.isClientSide()) {
            return;
        }
        
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Level level = event.level;
        
        // Only check overworld to avoid checking all dimensions (can be expanded later)
        if (!level.dimension().location().equals(net.minecraft.resources.ResourceLocation.parse("minecraft:overworld"))) {
            return;
        }
        
        // Iterate through all mobs in loaded chunks
        // Check around players to find loaded areas, then get mobs from those areas
        List<Mob> mobs = new java.util.ArrayList<>();
        
        // Get all players to find loaded areas
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            // Check area around each player (64 block radius)
            AABB playerArea = player.getBoundingBox().inflate(64.0D);
            List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, playerArea);
            for (Mob mob : nearbyMobs) {
                if (mob.isAlive() && !mob.isRemoved() && !mobs.contains(mob)) {
                    mobs.add(mob);
                }
            }
        }
        
        // Also check spawn area if no players found
        if (mobs.isEmpty() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.core.BlockPos spawnPos = serverLevel.getSharedSpawnPos();
            AABB spawnArea = new AABB(spawnPos).inflate(128.0D);
            List<Mob> spawnMobs = level.getEntitiesOfClass(Mob.class, spawnArea);
            mobs.addAll(spawnMobs);
        }
        for (Mob mob : mobs) {
            // Only handle mobs that should have custom pickup behavior
            if (!MobItemPickupHelper.shouldHandlePickup(mob)) {
                continue;
            }
            
            // Check and drop non-weapon, non-tool items from hands
            ItemStack mainHand = mob.getMainHandItem();
            ItemStack offHand = mob.getOffhandItem();

            if (!mainHand.isEmpty() && !MobItemPickupHelper.isWeaponOrTool(mainHand)) {
                mob.spawnAtLocation(mainHand.copy());
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                mainHand = ItemStack.EMPTY;
            }

            if (!offHand.isEmpty() && !MobItemPickupHelper.isWeaponOrTool(offHand)) {
                mob.spawnAtLocation(offHand.copy());
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                offHand = ItemStack.EMPTY;
            }

            // Two-handed NTGL weapons: only in main hand, off hand must be empty
            if (!mainHand.isEmpty() && mainHand.getItem() instanceof IWeapon
                    && !WeaponModifierHelper.isOneHanded(new WeaponData(mainHand, mob)) && !offHand.isEmpty()) {
                mob.spawnAtLocation(offHand.copy());
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                offHand = ItemStack.EMPTY;
            }
            if (!offHand.isEmpty() && offHand.getItem() instanceof IWeapon
                    && !WeaponModifierHelper.isOneHanded(new WeaponData(offHand, mob))) {
                mob.spawnAtLocation(offHand.copy());
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                offHand = ItemStack.EMPTY;
            }
            
            // Check nearby items and prevent pickup of non-weapon/non-tool items
            AABB searchBox = mob.getBoundingBox().inflate(1.5D); // Check 1.5 blocks around mob
            List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, searchBox);
            
            for (ItemEntity itemEntity : nearbyItems) {
                if (itemEntity.isRemoved() || !itemEntity.isAlive()) {
                    continue;
                }
                
                ItemStack itemStack = itemEntity.getItem();
                if (itemStack.isEmpty()) {
                    continue;
                }
                
                double distance = mob.distanceToSqr(itemEntity);
                if (distance >= 2.0D) {
                    continue;
                }

                // Pick up weapon/tool from ground when hand is empty (e.g. guards and other mobs)
                // Two-handed NTGL weapons go in main hand only; other hand must be empty. One-handed can dual-wield.
                if (MobItemPickupHelper.isWeaponOrTool(itemStack)) {
                    boolean isGun = itemStack.getItem() instanceof IWeapon;
                    boolean isTwoHandedGun = isGun && !WeaponModifierHelper.isOneHanded(new WeaponData(itemStack, mob));

                    if (mainHand.isEmpty()) {
                        mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack.copy());
                        itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                        if (isTwoHandedGun && !offHand.isEmpty()) {
                            mob.spawnAtLocation(offHand.copy());
                            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                        }
                        mainHand = mob.getMainHandItem();
                        offHand = mob.getOffhandItem();
                        continue;
                    }
                    if (offHand.isEmpty()) {
                        // Two-handed guns only in main hand: swap main to gun and clear off
                        if (isTwoHandedGun) {
                            mob.spawnAtLocation(mainHand.copy());
                            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack.copy());
                            itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                            mainHand = mob.getMainHandItem();
                            offHand = mob.getOffhandItem();
                            continue;
                        }
                        mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, itemStack.copy());
                        itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                        offHand = mob.getOffhandItem();
                        continue;
                    }
                    // Prefer swapping to a gun when holding something other than a gun (e.g. sword/crossbow)
                    if (isGun) {
                        if (!(mainHand.getItem() instanceof IWeapon)) {
                            mob.spawnAtLocation(mainHand.copy());
                            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack.copy());
                            if (isTwoHandedGun && !offHand.isEmpty()) {
                                mob.spawnAtLocation(offHand.copy());
                                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                            }
                            itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                            mainHand = mob.getMainHandItem();
                            offHand = mob.getOffhandItem();
                            continue;
                        }
                        if (!(offHand.getItem() instanceof IWeapon) && !isTwoHandedGun) {
                            mob.spawnAtLocation(offHand.copy());
                            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, itemStack.copy());
                            itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                            offHand = mob.getOffhandItem();
                            continue;
                        }
                    }
                }

                // If mob is close enough to pick up the item and it's not a weapon/tool, remove it
                if (!MobItemPickupHelper.isWeaponOrTool(itemStack)) {
                    itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
                
                // Handle NTGL weapon dual-wielding pickup
                if (itemStack.getItem() instanceof IWeapon && distance < 2.0D) {
                    boolean hasMainNTGL = mainHand.getItem() instanceof IWeapon;
                    boolean hasOffNTGL = offHand.getItem() instanceof IWeapon;
                    
                    if (hasMainNTGL || hasOffNTGL) {
                        WeaponData existingWeaponData = hasMainNTGL 
                            ? new WeaponData(mainHand, mob) 
                            : new WeaponData(offHand, mob);
                        WeaponData newWeaponData = new WeaponData(itemStack, mob);
                        
                        boolean existingIsOneHanded = WeaponModifierHelper.isOneHanded(existingWeaponData);
                        boolean newIsOneHanded = WeaponModifierHelper.isOneHanded(newWeaponData);
                        
                        if (existingIsOneHanded && newIsOneHanded) {
                            // Both are one-handed - allow dual wielding
                            // Remove item from world and equip manually
                            itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                            
                            if (hasMainNTGL && offHand.isEmpty()) {
                                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, itemStack.copy());
                            } else if (hasOffNTGL && mainHand.isEmpty()) {
                                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack.copy());
                            } else if (hasMainNTGL && !hasOffNTGL) {
                                if (!offHand.isEmpty()) {
                                    mob.spawnAtLocation(offHand.copy());
                                }
                                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, itemStack.copy());
                            } else if (hasOffNTGL && !hasMainNTGL) {
                                if (!mainHand.isEmpty()) {
                                    mob.spawnAtLocation(mainHand.copy());
                                }
                                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, itemStack.copy());
                            }
                        } else {
                            // At least one is two-handed - prevent pickup
                            itemEntity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                        }
                    }
                }
            }
        }
    }
}
