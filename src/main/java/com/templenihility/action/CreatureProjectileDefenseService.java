package com.templenihility.action;

import com.templenihility.entity.NihilityCreature;
import com.templenihility.entity.NihilityEnvoy;
import com.templenihility.entity.NihilityFollower;
import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

public final class CreatureProjectileDefenseService {
    public static void handle(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)
            || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }

        Entity hitEntity = hit.getEntity();
        if (hitEntity instanceof NihilityEnvoy envoy && envoy.tryDeflectProjectile(projectile)) {
            event.setCanceled(true);
            return;
        }

        if (!(hitEntity instanceof NihilityCreature protectedAlly) || !protectedAlly.isAlive()) {
            return;
        }
        Entity owner = projectile.getOwner();
        boolean friendlyProjectile = owner instanceof NihilityCreature;
        double healthRatio = protectedAlly.getMaxHealth() <= 0.0F
            ? 1.0
            : protectedAlly.getHealth() / protectedAlly.getMaxHealth();
        NihilityFollower guard = level.getEntitiesOfClass(
                NihilityFollower.class,
                protectedAlly.getBoundingBox().inflate(CreatureProjectileDefensePolicy.FOLLOWER_GUARD_RADIUS),
                follower -> follower != protectedAlly
                    && follower.isAlive()
                    && follower.canGuardProjectile())
            .stream()
            .filter(follower -> CreatureProjectileDefensePolicy.followerCanGuard(
                protectedAlly.getTier(), healthRatio, true, friendlyProjectile))
            .min(Comparator.comparingDouble(follower -> follower.distanceToSqr(protectedAlly)))
            .orElse(null);
        if (guard != null && guard.guardProjectile(projectile, protectedAlly)) {
            event.setCanceled(true);
        }
    }

    private CreatureProjectileDefenseService() {
    }
}
