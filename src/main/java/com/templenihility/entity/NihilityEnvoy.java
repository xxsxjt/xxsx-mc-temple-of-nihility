package com.templenihility.entity;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.action.CreatureProjectileDefensePolicy;
import com.templenihility.world.NihilityVisualEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NihilityEnvoy extends NihilityCreature {
    private static final EntityDataAccessor<Long> DATA_DEFLECT_UNTIL = SynchedEntityData.defineId(
        NihilityEnvoy.class, EntityDataSerializers.LONG);

    private long nextProjectileDeflectGameTime;

    public NihilityEnvoy(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int getTier() {
        return 3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEFLECT_UNTIL, 0L);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()
            && getCreatureAction() == ActionType.ENVOY_RIFT_DIRECTION
            && getCreatureActionPhase() == Phase.WINDUP
            && tickCount % 3 == 0
            && getCreatureActionTarget() != null) {
            NihilityVisualEffects.envoyRiftTelegraph(this, getCreatureActionTarget());
        }
    }

    @Override
    protected ActionType selectCreatureAction(LivingEntity target, long gameTime) {
        double distanceSquared = distanceToSqr(target);
        if (distanceSquared <= 3.25 * 3.25) {
            return ActionType.ENVOY_BACKSTEP;
        }
        return distanceSquared >= 4.0 * 4.0 && distanceSquared <= 9.0 * 9.0
            ? ActionType.ENVOY_RIFT_DIRECTION
            : ActionType.NONE;
    }

    @Override
    protected void applyCustomCreatureAction(ServerLevel level, ActionType action,
                                             LivingEntity target, long elapsed) {
        if (action == ActionType.ENVOY_RIFT_DIRECTION && markCreatureActionApplied()) {
            applyRiftDirection(level, target);
        }
    }

    public boolean tryDeflectProjectile(Projectile projectile) {
        if (projectile == null || !projectile.isAlive() || hasActiveCreatureAction()) {
            return false;
        }
        Entity owner = projectile.getOwner();
        boolean friendlyProjectile = owner instanceof NihilityCreature;
        long gameTime = level().getGameTime();
        if (!CreatureProjectileDefensePolicy.envoyCanDeflect(
            gameTime >= nextProjectileDeflectGameTime,
            projectile.getDeltaMovement().lengthSqr(),
            friendlyProjectile)) {
            return false;
        }

        Vec3 incoming = projectile.getDeltaMovement();
        Vec3 reflected;
        if (owner != null && owner.isAlive()) {
            Vec3 target = owner.position().add(0.0, owner.getBbHeight() * 0.55, 0.0);
            reflected = target.subtract(projectile.position());
            if (reflected.lengthSqr() > 1.0E-6) {
                double speed = Math.max(0.6, Math.min(2.5, incoming.length() * 1.05));
                reflected = reflected.normalize().scale(speed);
            } else {
                reflected = incoming.scale(-1.0);
            }
        } else {
            reflected = incoming.scale(-1.0);
        }
        if (reflected.lengthSqr() <= 1.0E-6) {
            return false;
        }

        nextProjectileDeflectGameTime = gameTime + CreatureProjectileDefensePolicy.ENVOY_DEFLECT_COOLDOWN_TICKS;
        entityData.set(DATA_DEFLECT_UNTIL,
            gameTime + CreatureProjectileDefensePolicy.ENVOY_DEFLECT_REACTION_TICKS);
        projectile.setOwner(this);
        projectile.setDeltaMovement(reflected);
        projectile.setPos(projectile.position().add(reflected.normalize().scale(0.5)));
        projectile.hurtMarked = true;
        NihilityVisualEffects.envoyProjectileDeflect(this, projectile.position(), reflected);
        return true;
    }

    public boolean isProjectileDeflecting() {
        return level().getGameTime() < entityData.get(DATA_DEFLECT_UNTIL);
    }

    private void applyRiftDirection(ServerLevel level, LivingEntity target) {
        Vec3 origin = target.position();
        if (distanceToSqr(target) > 12.0 * 12.0) {
            NihilityVisualEffects.envoyRiftDirection(this, target, origin, origin, false);
            return;
        }
        Vec3 forward = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() <= 1.0E-6) {
            return;
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        if (((getId() + level.getGameTime()) & 1L) == 0L) {
            side = side.scale(-1.0);
        }

        Vec3 destination = findRiftDestination(level, target, side);
        if (destination == null) {
            NihilityVisualEffects.envoyRiftDirection(this, target, origin, origin, false);
            return;
        }
        target.teleportTo(destination.x, destination.y, destination.z);
        Vec3 actualDestination = target.position();
        boolean moved = actualDestination.distanceToSqr(origin) >= 0.25;
        if (moved) {
            target.setDeltaMovement(target.getDeltaMovement().scale(0.35));
            target.hurtMarked = true;
        }
        NihilityVisualEffects.envoyRiftDirection(this, target, origin, actualDestination, moved);
    }

    private Vec3 findRiftDestination(ServerLevel level, LivingEntity target, Vec3 side) {
        Vec3 origin = target.position();
        AABB baseBox = target.getBoundingBox();
        for (double direction : new double[] {1.0, -1.0}) {
            for (double distance : new double[] {2.75, 2.0, 1.25}) {
                Vec3 candidate = origin.add(side.scale(direction * distance));
                AABB moved = baseBox.move(candidate.subtract(origin)).deflate(0.03);
                if (level.noCollision(target, moved) && level.noBorderCollision(target, moved)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("TempleEnvoyDeflectCooldownUntil", nextProjectileDeflectGameTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nextProjectileDeflectGameTime = input.getLongOr("TempleEnvoyDeflectCooldownUntil", 0L);
        entityData.set(DATA_DEFLECT_UNTIL, 0L);
    }
}
