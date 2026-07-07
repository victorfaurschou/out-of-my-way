package com.victorfaurschou.outofmyway.mixin;

import com.victorfaurschou.outofmyway.OutOfMyWay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TamableAnimal.class)
public class TamableAnimalMixin {

    @Inject(method = "feed", at = @At("HEAD"))
    private void onFeed(Player player, InteractionHand hand, ItemStack stack, float f1, float f2, CallbackInfo ci) {
        OutOfMyWay.resetPauseTimer((TamableAnimal) (Object) this);
    }

    // relocate vanilla teleports to land behind the player instead of at their feet
    @Inject(method = "maybeTeleportTo", at = @At("HEAD"), cancellable = true)
    private void onMaybeTeleportTo(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        TamableAnimal self = (TamableAnimal) (Object) this;
        if (!OutOfMyWay.isManagedAnimal(self)) return;
        if (!OutOfMyWay.isTypeEnabled(self)) return;
        if (OutOfMyWay.isTemporarilyDisabled(self)) return;
        if (self.isOrderedToSit()) return;
        if (self.getTarget() != null) return;

        LivingEntity owner = self.getOwner();
        if (!(owner instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        Vec3 target = OutOfMyWay.computeBehindTarget(player);
        Vec3 safe = OutOfMyWay.findSafePos(level, self, new Vec3(target.x, player.getY(), target.z));
        if (safe != null) {
            self.teleportTo(safe.x, safe.y, safe.z);
            self.getNavigation().stop();
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }

}
