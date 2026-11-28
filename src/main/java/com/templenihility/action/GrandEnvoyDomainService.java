package com.templenihility.action;

import com.templenihility.entity.NihilityCreature;
import com.templenihility.entity.NihilityGrandEnvoy;
import com.templenihility.world.NihilityVisualEffects;
import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GrandEnvoyDomainService {

    public static void tickDomain(ServerLevel level, NihilityGrandEnvoy envoy) {
        if (!envoy.isDomainActive(level.getGameTime()) || envoy.tickCount % 5 != 0) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(envoy.getDomainOrigin());
        double radius = GrandEnvoyActionPolicy.DOMAIN_RADIUS;
        double radiusSq = radius * radius;
        AABB area = new AABB(center, center).inflate(radius, 5.0, radius);
        boolean pulseDamageTick = envoy.tickCount % 20 == 0;
        boolean allyHealTick = envoy.tickCount % 40 == 0;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
            candidate -> candidate.isAlive() && candidate.distanceToSqr(center) <= radiusSq)) {
            if (entity instanceof NihilityCreature ally) {
                if (allyHealTick && ally.getHealth() < ally.getMaxHealth()) {
                    ally.heal(Math.max(4.0F, ally.getMaxHealth() * 0.03F));
                }
                continue;
            }
            if (entity instanceof ServerPlayer player) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                if (player.isSprinting()) {
                    Vec3 movement = player.getDeltaMovement();
                    player.setDeltaMovement(
                        movement.x * GrandEnvoyActionPolicy.DOMAIN_SPRINT_DRAG,
                        movement.y,
                        movement.z * GrandEnvoyActionPolicy.DOMAIN_SPRINT_DRAG);
                    player.hurtMarked = true;
                    player.causeFoodExhaustion(GrandEnvoyActionPolicy.DOMAIN_SPRINT_EXHAUSTION);
                    NihilityVisualEffects.grandEnvoyDomainDrag(player, center);
                }
                if (pulseDamageTick && (envoy.getTarget() == player || envoy.getDisposition().isCombat())) {
                    envoy.dealVoidStrike(level, player, GrandEnvoyActionPolicy.DOMAIN_PULSE_DAMAGE * 0.65F, 0.8F, false);
                }
            } else if (entity == envoy.getTarget()
                || (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() instanceof NihilityCreature)) {
                // Suppress hostile modded bosses & mobs inside the Void Domain!
                Vec3 movement = entity.getDeltaMovement();
                entity.setDeltaMovement(
                    movement.x * GrandEnvoyActionPolicy.DOMAIN_SPRINT_DRAG,
                    movement.y,
                    movement.z * GrandEnvoyActionPolicy.DOMAIN_SPRINT_DRAG);
                entity.hurtMarked = true;
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 1, false, false));
                if (pulseDamageTick) {
                    envoy.dealVoidStrike(level, entity, GrandEnvoyActionPolicy.DOMAIN_PULSE_DAMAGE, 0.9F, false);
                }
            }
        }
        if (pulseDamageTick) {
            NihilityVisualEffects.grandEnvoyDomainPulse(level, center, radius);
        }
    }

    public static NihilityGrandEnvoy findContainingDomain(ServerLevel level, Vec3 position) {
        double searchRadius = GrandEnvoyActionPolicy.DOMAIN_RADIUS * 3.0;
        AABB search = new AABB(position, position).inflate(searchRadius, 8.0, searchRadius);
        return level.getEntitiesOfClass(NihilityGrandEnvoy.class, search,
                envoy -> envoy.isDomainActive(level.getGameTime())
                    && position.distanceToSqr(Vec3.atCenterOf(envoy.getDomainOrigin()))
                        <= GrandEnvoyActionPolicy.DOMAIN_RADIUS * GrandEnvoyActionPolicy.DOMAIN_RADIUS)
            .stream()
            .min(Comparator.comparingDouble(envoy ->
                position.distanceToSqr(Vec3.atCenterOf(envoy.getDomainOrigin()))))
            .orElse(null);
    }

    private GrandEnvoyDomainService() {
    }
}
