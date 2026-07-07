package com.victorfaurschou.outofmyway.mixin;

import com.victorfaurschou.outofmyway.OutOfMyWay;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public class AnimalMobInteractMixin {

    // Catches feeding interactions that don't go through TamableAnimal.feed —
    // e.g. feeding a full-health pup to speed up growth, which skips feed() and
    // goes through Animal.mobInteract's canAgeUp path instead.
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        Animal self = (Animal) (Object) this;
        if (!(self instanceof TamableAnimal tamable)) return;
        if (!OutOfMyWay.isManagedAnimal(tamable)) return;
        OutOfMyWay.resetPauseTimer(tamable);
    }
}
