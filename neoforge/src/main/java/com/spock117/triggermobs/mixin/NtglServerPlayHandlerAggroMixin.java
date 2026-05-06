package com.spock117.triggermobs.mixin;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.network.ServerPlayHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NTGL {@link ServerPlayHandler} runs a hostile sweep on every gunshot ({@code aggroMobs}),
 * assigning {@link net.minecraft.world.entity.LivingEntity#setLastHurtByMob} within sound range —
 * including for non-player mobs. That makes allied hostiles converge on allied gunners after one shot.
 */
/** NTGL ships Mojmap names; disable remapping so Mixin does not resolve against obfuscated MC names. */
@Mixin(value = ServerPlayHandler.class, remap = false)
public final class NtglServerPlayHandlerAggroMixin {

    private NtglServerPlayHandlerAggroMixin() {}

    @Inject(method = "aggroMobs", at = @At("HEAD"), cancellable = true, remap = false)
    private static void triggermobs$skipBroadHostileAggroForNonPlayers(WeaponData data, Level level, CallbackInfo ci) {
        if (!(data.wielder instanceof Player)) {
            ci.cancel();
        }
    }
}
