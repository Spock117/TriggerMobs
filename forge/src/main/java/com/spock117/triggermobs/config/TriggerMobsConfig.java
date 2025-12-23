package com.spock117.triggermobs.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class TriggerMobsConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    
    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
    
    public static class Common {
        public final ForgeConfigSpec.IntValue baseAttackIntervalTicks;
        public final ForgeConfigSpec.IntValue attackIntervalVariance;
        
        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("TriggerMobs mob attack configuration").push("mob_attack");
            
            this.baseAttackIntervalTicks = builder
                .comment("Base attack interval in ticks (20 ticks = 1 second). Default: 200 ticks (10 seconds). For dual wielding, this is halved.")
                .defineInRange("baseAttackIntervalTicks", 200, 20, 1000);
            
            this.attackIntervalVariance = builder
                .comment("Random variance in ticks added to attack interval (±this value). Default: 80 ticks (±4 seconds)")
                .defineInRange("attackIntervalVariance", 80, 0, 200);
            
            builder.pop();
        }
    }
}

