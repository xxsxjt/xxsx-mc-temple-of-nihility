package com.templenihility.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class GravityFieldManager {
    public static final double RANGE = 10.0;
    public static final int DURATION_TICKS = 20 * 8;
    private static final double CACHE_KEEP_RANGE_SQR = (RANGE + 2.0) * (RANGE + 2.0);
    private static final int SCAN_INTERVAL_TICKS = 4;
    private static final int MAX_TARGETS = 32;
    private static final Map<UUID, GravityField> FIELDS = new HashMap<>();

    public static void create(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.position()
            .add(0.0, 1.4, 0.0)
            .add(player.getLookAngle().normalize().scale(8.0));
        FIELDS.put(player.getUUID(), new GravityField(level, center));
    }

    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || FIELDS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, GravityField>> iterator = FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            GravityField field = iterator.next().getValue();
            if (field.level != level) {
                continue;
            }
            if (!field.tick()) {
                iterator.remove();
            }
        }
    }

    private static final class GravityField {
        private final ServerLevel level;
        private final Vec3 center;
        private final List<LivingEntity> targets = new ArrayList<>();
        private int age;

        private GravityField(ServerLevel level, Vec3 center) {
            this.level = level;
            this.center = center;
        }

        private boolean tick() {
            if (age++ >= DURATION_TICKS) {
                return false;
            }
            if (age % SCAN_INTERVAL_TICKS == 1) {
                scanTargets();
            }
            NihilityVisualEffects.gravityFieldPulse(level, center, age, targets.size());
            pullTargets();
            return true;
        }

        private void scanTargets() {
            targets.clear();
            AABB area = new AABB(
                center.x - RANGE, center.y - RANGE, center.z - RANGE,
                center.x + RANGE, center.y + RANGE, center.z + RANGE);
            for (Entity entity : level.getEntities((Entity) null, area,
                    entity -> entity instanceof LivingEntity && entity.isAlive())) {
                if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                    targets.add(living);
                    if (targets.size() >= MAX_TARGETS) {
                        break;
                    }
                }
            }
        }

        private void pullTargets() {
            Iterator<LivingEntity> iterator = targets.iterator();
            while (iterator.hasNext()) {
                LivingEntity living = iterator.next();
                if (!living.isAlive() || living.level() != level || living.position().distanceToSqr(center) > CACHE_KEEP_RANGE_SQR) {
                    iterator.remove();
                    continue;
                }

                if (age % 20 == 1) {
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, true, false, true));
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, false, true));
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, true));
                }

                Vec3 pull = center.subtract(living.position());
                double distanceSqr = pull.lengthSqr();
                if (distanceSqr <= 0.01) {
                    living.setDeltaMovement(living.getDeltaMovement().scale(0.4));
                    continue;
                }

                double distance = Math.sqrt(distanceSqr);
                Vec3 direction = pull.scale(1.0 / distance);
                double strength = Math.min(0.68, 0.12 + distance * 0.045);
                Vec3 velocity = living.getDeltaMovement().scale(0.55)
                    .add(direction.scale(strength))
                    .add(0.0, 0.06, 0.0);
                if (velocity.lengthSqr() > 0.9 * 0.9) {
                    velocity = velocity.normalize().scale(0.9);
                }
                living.setDeltaMovement(velocity);
                living.hurtMarked = true;
                living.resetFallDistance();
            }
        }
    }

    private GravityFieldManager() {
    }
}
