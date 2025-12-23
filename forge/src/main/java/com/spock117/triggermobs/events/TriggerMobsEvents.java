package com.spock117.triggermobs.events;

import com.spock117.triggermobs.TriggerMobs;
import com.spock117.triggermobs.goals.MobGunAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TriggerMobs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TriggerMobsEvents {
    
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only handle on server side (works in both single-player and multiplayer)
        // In single-player, Minecraft runs an integrated server, so this code still executes
        // We skip client-side only to avoid duplicate goal registration
        if (event.getLevel().isClientSide()) {
            return;
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
}

