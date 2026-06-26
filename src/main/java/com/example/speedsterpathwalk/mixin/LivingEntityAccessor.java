package com.example.speedsterpathwalk.mixin;

import net.minecraft.entity.LimbAnimator;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Client animation helper accessor.
 *
 * Server-side teleport/substep movement does not always produce normal client
 * limb animation. This accessor lets the client-side speedwalk assist nudge the
 * vanilla limb animator while the speedwalk packet state is active.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("limbAnimator")
    LimbAnimator speedster_pathwalk$getLimbAnimator();
}
