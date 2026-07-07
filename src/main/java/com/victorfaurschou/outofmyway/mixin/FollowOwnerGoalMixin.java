package com.victorfaurschou.outofmyway.mixin;

import com.victorfaurschou.outofmyway.OutOfMyWay;
import com.victorfaurschou.outofmyway.OutOfMyWayConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FollowOwnerGoal.class)
public class FollowOwnerGoalMixin {

    @Shadow @Final private TamableAnimal tamable;
    @Shadow private LivingEntity owner;

    private static boolean isInFrontOf(ServerPlayer player, double dx, double dz) {
        Vec3 look = player.getLookAngle();
        Vec3 hLook = new Vec3(look.x, 0, look.z);
        if (hLook.lengthSqr() < 1e-6) return false;
        hLook = hLook.normalize();
        return OutOfMyWay.isInFrontOf(dx, dz, hLook);
    }

    // Suppress vanilla follow when we're actively managing the animal so it doesn't fight our nudge.
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void onCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (!(owner instanceof ServerPlayer player)) return;
        if (!OutOfMyWay.isManagedAnimal(tamable)) return;
        if (!OutOfMyWay.isTypeEnabled(tamable)) return;
        if (OutOfMyWay.isTemporarilyDisabled(tamable)) return;
        if (tamable.isOrderedToSit()) return;

        double dx = tamable.getX() - player.getX();
        double dz = tamable.getZ() - player.getZ();
        double distSq = dx * dx + dz * dz;
        float minDist = OutOfMyWayConfig.minDistance;
        boolean inZone  = distSq < minDist * minDist;
        double inViewR  = OutOfMyWayConfig.inViewDistance;
        boolean inFront = OutOfMyWayConfig.inView
                && distSq < inViewR * inViewR
                && isInFrontOf(player, dx, dz);
        if (inZone || inFront || OutOfMyWay.isActivelyRelocating(tamable.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
