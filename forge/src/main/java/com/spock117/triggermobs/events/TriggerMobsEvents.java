package com.spock117.triggermobs.events;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.goals.MobGunAttackGoal;
import com.spock117.triggermobs.util.MobItemPickupHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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
        "minecraft:zombified_piglin"
    );
    
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only handle on server side (works in both single-player and multiplayer)
        // In single-player, Minecraft runs an integrated server, so this code still executes
        // We skip client-side only to avoid duplicate goal registration
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
        
        // Only add goal to hostile mobs (Monster)
        if (event.getEntity() instanceof Monster monster) {
            try {
                MobGunAttackGoal goal = new MobGunAttackGoal(monster, 0.6D, 16.0F); // Reduced speed: 0.6 instead of 1.0
                monster.goalSelector.addGoal(3, goal);
            } catch (Exception e) {
                // Silently handle errors
            }
        }
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
            }
            
            if (!offHand.isEmpty() && !MobItemPickupHelper.isWeaponOrTool(offHand)) {
                mob.spawnAtLocation(offHand.copy());
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
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
                
                // If mob is close enough to pick up the item and it's not a weapon/tool, remove it
                double distance = mob.distanceToSqr(itemEntity);
                if (distance < 2.0D && !MobItemPickupHelper.isWeaponOrTool(itemStack)) {
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
