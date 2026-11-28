package com.templenihility.entity;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.action.CreatureProjectileDefensePolicy;
import com.templenihility.init.ModParticles;
import com.templenihility.world.NihilityVisualEffects;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Archetype: Spatial Monolith Assassin (虚无使者 · 空间裂隙双碑刺客).
 * Glides smoothly in low hover while orbiting targets, deflects incoming projectiles,
 * detaches its Twin Runic Monoliths to flank and slam inward in a high-speed pincer clamp,
 * and blinks through void rifts behind enemies for sudden twin-monolith scissor backstabs!
 */
public class NihilityEnvoy extends NihilityCreature {
    private static final EntityDataAccessor<Long> DATA_DEFLECT_UNTIL = SynchedEntityData.defineId(
        NihilityEnvoy.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_MONOLITHS_DEPLOYED = SynchedEntityData.defineId(
        NihilityEnvoy.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BACKSTAB_TICKS = SynchedEntityData.defineId(
        NihilityEnvoy.class, EntityDataSerializers.INT);

    private long nextProjectileDeflectGameTime;
    private int deployedMonolithCount;
    private int monolithSafetyTicks;

    public NihilityEnvoy(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected boolean usesDefaultMeleeGoal() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new EnvoySpatialGoal(this));
    }

    @Override
    public int getTier() {
        return 3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEFLECT_UNTIL, 0L);
        builder.define(DATA_MONOLITHS_DEPLOYED, false);
        builder.define(DATA_BACKSTAB_TICKS, 0);
    }

    public boolean areMonolithsDeployed() {
        return entityData.get(DATA_MONOLITHS_DEPLOYED);
    }

    public int getBackstabTicks() {
        return entityData.get(DATA_BACKSTAB_TICKS);
    }

    public void onMonolithReturned() {
        if (deployedMonolithCount > 0) {
            deployedMonolithCount--;
        }
        if (deployedMonolithCount <= 0) {
            deployedMonolithCount = 0;
            entityData.set(DATA_MONOLITHS_DEPLOYED, false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        int backstab = getBackstabTicks();
        if (backstab > 0) {
            entityData.set(DATA_BACKSTAB_TICKS, backstab - 1);
        }
        if (monolithSafetyTicks > 0) {
            monolithSafetyTicks--;
            if (monolithSafetyTicks == 0) {
                deployedMonolithCount = 0;
                entityData.set(DATA_MONOLITHS_DEPLOYED, false);
            }
        }
        if (getCreatureAction() == ActionType.ENVOY_RIFT_DIRECTION
            && getCreatureActionPhase() == Phase.WINDUP
            && tickCount % 3 == 0
            && getCreatureActionTarget() != null) {
            NihilityVisualEffects.envoyRiftTelegraph(this, getCreatureActionTarget());
        }
    }

    @Override
    protected ActionType selectCreatureAction(LivingEntity target, long gameTime) {
        double distanceSquared = distanceToSqr(target);
        if (distanceSquared <= 3.5 * 3.5) {
            return ActionType.ENVOY_BACKSTEP;
        }
        return distanceSquared >= 5.5 * 5.5 && distanceSquared <= 14.0 * 14.0
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

    private void deployTwinMonolithPincer(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
        Vec3 toTarget = center.subtract(position()).multiply(1.0, 0.0, 1.0);
        if (toTarget.lengthSqr() < 1.0E-5) {
            toTarget = new Vec3(0.0, 0.0, 1.0);
        }
        Vec3 fwd = toTarget.normalize();
        Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x).normalize();

        float clampDmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15F;
        Vec3 leftFlank = center.add(side.scale(3.8));
        Vec3 rightFlank = center.add(side.scale(-3.8));

        level.addFreshEntity(VoidSpellEntity.createMonolithPillar(level, this, target, leftFlank, center, clampDmg));
        level.addFreshEntity(VoidSpellEntity.createMonolithPillar(level, this, target, rightFlank, center, clampDmg));
        deployedMonolithCount = 2;
        monolithSafetyTicks = 40;
        entityData.set(DATA_MONOLITHS_DEPLOYED, true);

        level.sendParticles(ModParticles.NIHILITY_SHARD.get(), leftFlank.x, leftFlank.y, leftFlank.z, 8, 0.25, 0.6, 0.25, 0.06);
        level.sendParticles(ModParticles.NIHILITY_SHARD.get(), rightFlank.x, rightFlank.y, rightFlank.z, 8, 0.25, 0.6, 0.25, 0.06);
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 0.9F, 1.4F);
    }

    private boolean performRiftBackstab(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() < 1.0E-5) {
            look = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        }
        if (look.lengthSqr() < 1.0E-5) {
            return false;
        }
        Vec3 behindDir = look.normalize().scale(-1.0);
        Vec3 dest = null;
        AABB baseBox = getBoundingBox();
        for (double dist : new double[] {2.4, 1.9, 2.9}) {
            Vec3 candidate = target.position().add(behindDir.scale(dist));
            AABB moved = baseBox.move(candidate.subtract(position())).deflate(0.03);
            if (level.noCollision(this, moved) && level.noBorderCollision(this, moved)) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) {
            return false;
        }
        Vec3 from = position();
        level.sendParticles(ModParticles.NIHILITY_SHARD.get(), from.x, from.y + 1.0, from.z, 12, 0.35, 0.6, 0.35, 0.08);
        teleportTo(dest.x, dest.y, dest.z);
        getLookControl().setLookAt(target, 90.0F, 90.0F);
        entityData.set(DATA_BACKSTAB_TICKS, 14);

        float strikeDmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.45F;
        if (dealVoidStrike(level, target, strikeDmg, 1.35F, true)) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, false, false));
        }
        // Smooth follow-through evade back slightly after the scissor thrust
        Vec3 evade = behindDir.scale(0.55);
        setDeltaMovement(evade.x, 0.16, evade.z);
        hurtMarked = true;

        level.sendParticles(ModParticles.NIHILITY_SHARD.get(), dest.x, dest.y + 1.0, dest.z, 16, 0.4, 0.6, 0.4, 0.12);
        level.playSound(null, dest.x, dest.y, dest.z,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.95F, 1.3F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 1.25F);
        return true;
    }

    public boolean tryDeflectProjectile(Projectile projectile) {
        if (projectile == null || !projectile.isAlive()) {
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
                double speed = Math.max(0.85, Math.min(3.0, incoming.length() * 1.35));
                reflected = reflected.normalize().scale(speed);
            } else {
                reflected = incoming.scale(-1.25);
            }
        } else {
            reflected = incoming.scale(-1.25);
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
        if (distanceToSqr(target) > 16.0 * 16.0) {
            NihilityVisualEffects.envoyRiftDirection(this, target, origin, origin, false);
            return;
        }
        float riftDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.45F;
        dealVoidStrike(level, target, riftDamage, 1.3F, true);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, false, false));

        Vec3 forward = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() <= 1.0E-6) {
            NihilityVisualEffects.envoyRiftDirection(this, target, origin, origin, true);
            return;
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        if (((getId() + level.getGameTime()) & 1L) == 0L) {
            side = side.scale(-1.0);
        }
        Vec3 destination = findRiftDestination(level, target, side);
        if (destination == null) {
            NihilityVisualEffects.envoyRiftDirection(this, target, origin, origin, true);
            return;
        }
        target.teleportTo(destination.x, destination.y, destination.z);
        Vec3 actualDestination = target.position();
        boolean moved = actualDestination.distanceToSqr(origin) >= 0.25;
        if (moved) {
            target.setDeltaMovement(target.getDeltaMovement().scale(0.25));
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
        entityData.set(DATA_MONOLITHS_DEPLOYED, false);
        entityData.set(DATA_BACKSTAB_TICKS, 0);
    }

    private static final class EnvoySpatialGoal extends Goal {
        private final NihilityEnvoy envoy;
        private int pincerCooldown = 20;
        private int backstabCooldown = 55;
        private int strafeDir = 1;
        private int strafeSwitch = 25;

        EnvoySpatialGoal(NihilityEnvoy envoy) {
            this.envoy = envoy;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = envoy.getTarget();
            return target != null && target.isAlive() && envoy.getDisposition().isCombat();
        }

        @Override
        public void tick() {
            LivingEntity target = envoy.getTarget();
            if (target == null || !target.isAlive() || !(envoy.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            envoy.getLookControl().setLookAt(target, 45.0F, 45.0F);
            if (pincerCooldown > 0) pincerCooldown--;
            if (backstabCooldown > 0) backstabCooldown--;
            if (--strafeSwitch <= 0) {
                strafeDir = -strafeDir;
                strafeSwitch = 22 + envoy.getRandom().nextInt(22);
            }

            double distSq = envoy.distanceToSqr(target);
            // Smooth agile spatial strafing at 5.5..11 blocks
            if (distSq > 11.5 * 11.5) {
                envoy.getNavigation().moveTo(target, 1.32);
            } else if (distSq < 5.0 * 5.0) {
                envoy.getNavigation().stop();
                envoy.getMoveControl().strafe(-0.85F, strafeDir * 0.95F);
            } else {
                envoy.getNavigation().stop();
                envoy.getMoveControl().strafe(0.15F, strafeDir * 0.98F);
            }

            // Priority 1: Twin-Monolith Pincer Clamp when docked!
            if (pincerCooldown <= 0 && !envoy.areMonolithsDeployed() && distSq <= 16.0 * 16.0) {
                envoy.deployTwinMonolithPincer(serverLevel, target);
                pincerCooldown = 68;
                return;
            }

            // Priority 2: Rift Backstab Blink!
            if (backstabCooldown <= 0 && !envoy.areMonolithsDeployed() && distSq >= 3.2 * 3.2 && distSq <= 14.0 * 14.0) {
                if (envoy.performRiftBackstab(serverLevel, target)) {
                    backstabCooldown = 95;
                } else {
                    backstabCooldown = 25;
                }
            }
        }
    }
}
