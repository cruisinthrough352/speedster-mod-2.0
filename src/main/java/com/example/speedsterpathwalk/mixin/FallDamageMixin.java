package com.example.speedsterpathwalk.mixin;

import com.example.speedsterpathwalk.server.ServerSpeedwalkRunner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class FallDamageMixin {
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void speedster_pathwalk$cancelSpeedwalkFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof PlayerEntity player) || player.getWorld().isClient) {
            return;
        }

        if (ServerSpeedwalkRunner.isFallDamageProtected(player.getUuid())) {
            player.fallDistance = 0.0F;
            cir.setReturnValue(false);
        }
    }
}
