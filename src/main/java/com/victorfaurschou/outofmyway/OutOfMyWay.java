package com.victorfaurschou.outofmyway;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OutOfMyWay implements ModInitializer {

    public static final String MOD_ID = "out-of-my-way";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Within this distance we manage positioning; beyond it, vanilla teleport (relocated by mixin) handles catch-up
    public static final double SCAN_RADIUS = 15.0;
    private static final double ARRIVE_THRESHOLD_SQ = 1.5 * 1.5;

    private static long interactDisableMs() { return OutOfMyWayConfig.itemHoldDisableDelay * 1000L; }
    private static final ConcurrentHashMap<UUID, Long> disabledUntilMs = new ConcurrentHashMap<>();

    // Animals our mod is currently navigating — collision with players disabled for these
    private static final Set<UUID> activelyRelocating =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static boolean isActivelyRelocating(UUID id) {
        return activelyRelocating.contains(id);
    }

    @Override
    public void onInitialize() {
        OutOfMyWayConfig.load();
        ServerTickEvents.START_LEVEL_TICK.register(this::onLevelTick);
    }

    private void onLevelTick(ServerLevel level) {
        if (!OutOfMyWayConfig.enabled) return;
        // Match vanilla FollowOwnerGoal's recalculation cadence
        if (level.getGameTime() % 10 != 0) return;

        // Periodically evict expired interact-disable entries
        if (level.getGameTime() % 200 == 0) {
            long now = System.currentTimeMillis();
            disabledUntilMs.entrySet().removeIf(e -> e.getValue() < now);
        }

        for (ServerPlayer player : level.players()) {
            Vec3 look = player.getLookAngle();
            Vec3 hLook = new Vec3(look.x, 0, look.z);
            if (hLook.lengthSqr() < 1e-6) continue;
            hLook = hLook.normalize();

            Vec3 target = behindTarget(player, hLook);

            double scanRadius = OutOfMyWayConfig.inView
                    ? Math.max(SCAN_RADIUS, OutOfMyWayConfig.inViewDistance)
                    : SCAN_RADIUS;
            AABB scanBox = player.getBoundingBox().inflate(scanRadius);
            for (TamableAnimal animal : level.getEntitiesOfClass(TamableAnimal.class, scanBox)) {
                if (!isOwnedBy(animal, player)) continue;

                UUID aid = animal.getUUID();

                if (!isTypeEnabled(animal)) { activelyRelocating.remove(aid); continue; }

                updateInteractDisable(player, animal);

                if (isTemporarilyDisabled(animal)) { activelyRelocating.remove(aid); continue; }
                if (animal.isOrderedToSit()) { activelyRelocating.remove(aid); continue; }
                if (animal.getTarget() != null) { activelyRelocating.remove(aid); continue; }
                if (animal.isPassenger()) { activelyRelocating.remove(aid); continue; }

                double dx = animal.getX() - player.getX();
                double dz = animal.getZ() - player.getZ();
                double distSq = dx * dx + dz * dz;
                float minDist = OutOfMyWayConfig.minDistance;
                boolean inZone = distSq < minDist * minDist;
                double inViewR = OutOfMyWayConfig.inViewDistance;
                boolean inFront = OutOfMyWayConfig.inView
                        && distSq < inViewR * inViewR
                        && isInFrontOf(dx, dz, hLook);
                if (!inZone && !inFront) { activelyRelocating.remove(aid); continue; }

                Vec3 animalTarget = stableOffset(target, animal, hLook);

                double tdx = animal.getX() - animalTarget.x;
                double tdz = animal.getZ() - animalTarget.z;
                if (tdx * tdx + tdz * tdz < ARRIVE_THRESHOLD_SQ) {
                    activelyRelocating.remove(aid);
                    continue;
                }

                activelyRelocating.add(aid);
                animal.getNavigation().moveTo(
                        animalTarget.x, player.getY(), animalTarget.z,
                        OutOfMyWayConfig.speedMultiplier);
            }
        }
    }

    // Each animal gets a stable lateral offset so multiple animals don't stack at the same point
    private static Vec3 stableOffset(Vec3 base, TamableAnimal animal, Vec3 hLook) {
        long bits = animal.getUUID().getLeastSignificantBits();
        double lateral = ((bits & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL - 0.5) * 4.8;
        Vec3 right = new Vec3(-hLook.z, 0, hLook.x);
        return base.add(right.scale(lateral));
    }

    private static Vec3 behindTarget(ServerPlayer player, Vec3 hLook) {
        return player.position().subtract(hLook.scale(OutOfMyWayConfig.minDistance));
    }

    public static Vec3 computeBehindTarget(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 hLook = new Vec3(look.x, 0, look.z);
        if (hLook.lengthSqr() < 1e-6) hLook = new Vec3(0, 0, 1);
        return behindTarget(player, hLook.normalize());
    }

    private static void updateInteractDisable(ServerPlayer player, TamableAnimal animal) {
        if (!isHoldingTamingItem(player, animal)) return;
        UUID aid = animal.getUUID();
        disabledUntilMs.merge(aid, System.currentTimeMillis() + interactDisableMs(), Math::max);
        animal.getNavigation().stop();
    }

    public static boolean isTemporarilyDisabled(TamableAnimal animal) {
        Long until = disabledUntilMs.get(animal.getUUID());
        return until != null && System.currentTimeMillis() < until;
    }

    public static void resetPauseTimer(TamableAnimal animal) {
        disabledUntilMs.merge(animal.getUUID(),
                System.currentTimeMillis() + interactDisableMs(), Math::max);
        animal.getNavigation().stop();
    }

    public static boolean isTypeEnabled(TamableAnimal animal) {
        if (animal instanceof Wolf)   return OutOfMyWayConfig.enableWolves;
        if (animal instanceof Cat)    return OutOfMyWayConfig.enableCats;
        if (animal instanceof Parrot) return OutOfMyWayConfig.enableParrots;
        return false;
    }

    public static boolean isInFrontOf(double dx, double dz, Vec3 hLook) {
        return dx * hLook.x + dz * hLook.z > 0;
    }

    public static boolean isOwnedBy(TamableAnimal animal, ServerPlayer player) {
        LivingEntity owner = animal.getOwner();
        return owner != null && owner.getUUID().equals(player.getUUID());
    }

    public static boolean isManagedAnimal(TamableAnimal animal) {
        return (animal instanceof Wolf || animal instanceof Cat || animal instanceof Parrot)
                && animal.isTame();
    }

    private static boolean isHoldingTamingItem(ServerPlayer player, TamableAnimal animal) {
        return isTamingItem(player.getMainHandItem(), animal)
                || isTamingItem(player.getOffhandItem(), animal);
    }

    private static boolean isTamingItem(ItemStack stack, TamableAnimal animal) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (animal instanceof Wolf)   return item == Items.BONE
                || isInCustomList(item, OutOfMyWayConfig.customWolfItems);
        if (animal instanceof Cat)    return item == Items.COD || item == Items.SALMON;
        if (animal instanceof Parrot) return item == Items.WHEAT_SEEDS || item == Items.MELON_SEEDS
                || item == Items.PUMPKIN_SEEDS || item == Items.BEETROOT_SEEDS
                || item == Items.TORCHFLOWER_SEEDS || item == Items.PITCHER_POD;
        return false;
    }

    private static boolean isInCustomList(Item item, List<String> ids) {
        if (ids.isEmpty()) return false;
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        String keyStr = key.toString();
        return ids.contains(keyStr);
    }

    // Searches outward from center for a position with solid ground below and no block collision
    public static Vec3 findSafePos(ServerLevel level, Entity entity, Vec3 center) {
        float w = entity.getBbWidth();
        float h = entity.getBbHeight();
        for (int r = 0; r <= 5; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) < r && Math.abs(dz) < r) continue;
                    for (int dy = 2; dy >= -5; dy--) {
                        double cx = center.x + dx;
                        double cy = center.y + dy;
                        double cz = center.z + dz;
                        AABB aabb = new AABB(cx - w / 2, cy, cz - w / 2,
                                             cx + w / 2, cy + h, cz + w / 2);
                        BlockPos below = BlockPos.containing(cx, cy - 0.1, cz);
                        if (level.noCollision(entity, aabb)
                                && level.getBlockState(below)
                                        .isFaceSturdy(level, below, Direction.UP)) {
                            return new Vec3(cx, cy, cz);
                        }
                    }
                }
            }
        }
        return null;
    }
}
