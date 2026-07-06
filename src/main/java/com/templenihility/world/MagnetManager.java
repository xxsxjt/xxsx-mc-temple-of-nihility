package com.templenihility.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MagnetManager {
    public static final double RANGE = 8.0;
    private static final double RANGE_SQR = RANGE * RANGE;
    private static final double CACHE_KEEP_RANGE_SQR = (RANGE + 2.0) * (RANGE + 2.0);
    private static final double PICKUP_DISTANCE_SQR = 1.45 * 1.45;
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int MAX_TARGETS = 48;
    private static final Map<UUID, MagnetState> STATES = new HashMap<>();

    public static void tick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        MagnetState state = STATES.computeIfAbsent(player.getUUID(), uuid -> new MagnetState());
        if (state.lastTick == player.tickCount) {
            return;
        }
        state.lastTick = player.tickCount;

        if (player.tickCount >= state.nextScanTick) {
            scan(player, state);
            state.nextScanTick = player.tickCount + SCAN_INTERVAL_TICKS;
        }
        pullCachedTargets(player, state);
    }

    public static void clear(Player player) {
        STATES.remove(player.getUUID());
    }

    private static void scan(Player player, MagnetState state) {
        state.targets.clear();
        for (Entity nearby : player.level().getEntities(player, player.getBoundingBox().inflate(RANGE),
                entity -> entity instanceof ItemEntity && entity.isAlive() && entity.distanceToSqr(player) <= RANGE_SQR)) {
            state.targets.add((ItemEntity) nearby);
            if (state.targets.size() >= MAX_TARGETS) {
                break;
            }
        }
    }

    private static void pullCachedTargets(Player player, MagnetState state) {
        Vec3 target = player.position().add(0.0, 0.85, 0.0);
        Iterator<ItemEntity> iterator = state.targets.iterator();
        while (iterator.hasNext()) {
            ItemEntity item = iterator.next();
            if (!item.isAlive() || item.level() != player.level() || item.distanceToSqr(player) > CACHE_KEEP_RANGE_SQR) {
                iterator.remove();
                continue;
            }

            if (item.hasPickUpDelay()) {
                item.setNoPickUpDelay();
            }

            Vec3 pull = target.subtract(item.position());
            double distanceSqr = pull.lengthSqr();
            if (distanceSqr <= PICKUP_DISTANCE_SQR) {
                item.playerTouch(player);
                continue;
            }

            double distance = Math.sqrt(distanceSqr);
            Vec3 direction = pull.scale(1.0 / distance);
            double strength = Math.min(0.22, 0.075 + distance * 0.018);
            Vec3 velocity = item.getDeltaMovement().scale(0.82).add(direction.scale(strength));
            if (velocity.lengthSqr() > 0.42 * 0.42) {
                velocity = velocity.normalize().scale(0.42);
            }
            item.setDeltaMovement(velocity);
            item.hurtMarked = true;
        }
    }

    private static final class MagnetState {
        private final List<ItemEntity> targets = new ArrayList<>();
        private int nextScanTick;
        private int lastTick = -1;
    }

    private MagnetManager() {
    }
}
