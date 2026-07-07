package com.victorfaurschou.outofmyway.mixin;

import com.victorfaurschou.outofmyway.OutOfMyWay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityPushMixin {

    // When animal.pushEntities() runs, it calls doPush(player) → player.push(animal),
    // so this=player and other=animal. Cancel so the player isn't shunted around.
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity other, CallbackInfo ci) {
        if (!(other instanceof TamableAnimal ta)) return;
        if (OutOfMyWay.isManagedAnimal(ta) && OutOfMyWay.isTypeEnabled(ta)
                && OutOfMyWay.isActivelyRelocating(ta.getUUID())) {
            ci.cancel();
        }
    }
}
