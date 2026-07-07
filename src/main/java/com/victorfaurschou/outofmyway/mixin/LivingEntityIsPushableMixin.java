package com.victorfaurschou.outofmyway.mixin;

import com.victorfaurschou.outofmyway.OutOfMyWay;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityIsPushableMixin {

    // isPushable is overridden in LivingEntity. Return false while our mod is actively
    // relocating the animal so players can't push it mid-path and deflect it.
    @Inject(method = "isPushable", at = @At("RETURN"), cancellable = true)
    private void onIsPushable(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!((Object) this instanceof TamableAnimal ta)) return;
        if (OutOfMyWay.isManagedAnimal(ta) && OutOfMyWay.isTypeEnabled(ta)
                && OutOfMyWay.isActivelyRelocating(ta.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
