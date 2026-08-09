package com.templenihility.action;

import com.templenihility.entity.NihilityGrandEnvoy;
import com.templenihility.world.NihilityVisualEffects;
import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GrandEnvoyDomainService {
    public static void tickDomain(ServerLevel level, NihilityGrandEnvoy envoy) {
        if (!envoy.isDomainActive(level.getGameTime()) || envoy.tickCount % 5 != 0) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(envoy.getDomainOrigin());
        double radius = GrandEnvoyActionPolicy.DOMAIN_RADIUS;
        AABB area = new AABB(center, center).inflate(radius, 4.0, radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area,
            candidate -> candidate.isAlive() && !candidate.isSpectator() && !candidate.isCreative()
                && candidate.distanceToSqr(center) <= radius * radius)) {
            if (!player.isSprinting()) {
                continue;
            }
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(
                movement.x * GrandEnvoyActionPolicy.DOMAIN_SPRINT_DRAG,
                movement.y,
                movement.z * GrandEnvoyActionPolicy.DOMAIN_SPRINT_DRAG);
            player.hurtMarked = true;
            player.causeFoodExhaustion(GrandEnvoyActionPolicy.DOMAIN_SPRINT_EXHAUSTION);
            NihilityVisualEffects.grandEnvoyDomainDrag(player, center);
        }

        if (envoy.tickCount % 20 == 0) {
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
