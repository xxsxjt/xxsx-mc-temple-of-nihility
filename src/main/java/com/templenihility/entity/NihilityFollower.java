package com.templenihility.entity;

import com.templenihility.action.CreatureActionPolicy.ActionType;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Archetype: Pure Heavy Halberd Melee Vanguard (虚无随从 · 纯近战黑曜重戟先锋).
 * Zero floating spells — relies on true polearm reach (3.6 blocks), a 160-degree heavy
 * horizontal halberd sweep, couched vanguard intercept charges, and an active Halberd
 * Parry Stance that absorbs 65% damage and instantly punishes melee attackers with a
 * crushing overhead Counter-Cleave!
 */
public class NihilityFollower extends NihilityCreature {
    public static final int STANCE_IDLE = 0;
    public static final int STANCE_SWEEP_WINDUP = 1;
    public static final int STANCE_SWEEP_ACTIVE = 2;
    public static final int STANCE_GUARD_PARRY = 3;
    public static final int STANCE_COUNTER_SLAM = 4;

    private static final EntityDataAccessor<Long> DATA_GUARD_UNTIL = SynchedEntityData.defineId(
        NihilityFollower.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_HALBERD_STANCE = SynchedEntityData.defineId(
        NihilityFollower.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STANCE_TICKS = SynchedEntityData.defineId(
        NihilityFollower.class, EntityDataSerializers.INT);

    private long nextProjectileGuardGameTime;
    private float burstDamageWindow;
    private long burstWindowResetTick;
    private long nextParryAllowedTick;

    public NihilityFollower(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected boolean usesDefaultMeleeGoal() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new FollowerHalberdGoal(this));
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GUARD_UNTIL, 0L);
        builder.define(DATA_HALBERD_STANCE, STANCE_IDLE);
        builder.define(DATA_STANCE_TICKS, 0);
    }

    public int getHalberdStance() {
        return entityData.get(DATA_HALBERD_STANCE);
    }

    public int getStanceTicks() {
        return entityData.get(DATA_STANCE_TICKS);
    }

    private void setHalberdStance(int stance, int duration) {
        entityData.set(DATA_HALBERD_STANCE, stance);
        entityData.set(DATA_STANCE_TICKS, duration);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int ticks = getStanceTicks();
        int stance = getHalberdStance();
        if (ticks > 0) {
            ticks--;
            entityData.set(DATA_STANCE_TICKS, ticks);
            if (ticks == 0) {
                if (stance == STANCE_SWEEP_WINDUP) {
                    executeHalberdSweep(serverLevel);
                } else {
                    entityData.set(DATA_HALBERD_STANCE, STANCE_IDLE);
                }
            }
        }
    }

    @Override
    protected ActionType selectCreatureAction(LivingEntity target, long gameTime) {
        if (getHalberdStance() != STANCE_IDLE) {
            return ActionType.NONE;
        }
        double distanceSqr = distanceToSqr(target);
        return distanceSqr >= 3.5 * 3.5 && distanceSqr <= 7.8 * 7.8
            ? ActionType.FOLLOWER_INTERCEPT
            : ActionType.NONE;
    }

    private void beginHalberdSweep(ServerLevel level, LivingEntity target) {
        getLookControl().setLookAt(target, 45.0F, 45.0F);
        setHalberdStance(STANCE_SWEEP_WINDUP, 8);
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.7F, 0.72F);
    }

    private void executeHalberdSweep(ServerLevel level) {
        setHalberdStance(STANCE_SWEEP_ACTIVE, 10);
        swing(InteractionHand.MAIN_HAND);

        Vec3 look = getLookAngle().multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() < 1.0E-5) {
            look = new Vec3(0.0, 0.0, 1.0);
        }
        Vec3 fwd = look.normalize();
        // Lunge slightly forward with the heavy polearm sweep
        setDeltaMovement(fwd.x * 0.36, getDeltaMovement().y, fwd.z * 0.36);
        hurtMarked = true;

        float sweepDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.18F;
        double reach = 3.85;
        AABB area = getBoundingBox().inflate(reach, 1.6, reach);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e.isAlive() && e != this && !(e instanceof NihilityCreature) && canAttack(e))) {
            Vec3 toVictim = victim.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (toVictim.lengthSqr() > reach * reach) {
                continue;
            }
            if (toVictim.lengthSqr() > 1.0E-5 && toVictim.normalize().dot(fwd) < -0.18) {
                continue; // 160-degree forward crescent arc
            }
            if (dealVoidStrike(level, victim, sweepDamage, 1.05F, true)) {
                Vec3 push = toVictim.lengthSqr() > 1.0E-5 ? toVictim.normalize() : fwd;
                victim.setDeltaMovement(push.x * 0.72, Math.max(0.24, victim.getDeltaMovement().y + 0.16), push.z * 0.72);
                victim.hurtMarked = true;
            }
        }

        // Wide cyan-obsidian crescent slash arc particles!
        double baseYaw = Math.atan2(fwd.z, fwd.x);
        for (int i = -6; i <= 6; i++) {
            double angle = baseYaw + i * 0.21;
            double rx = getX() + Math.cos(angle) * 3.1;
            double rz = getZ() + Math.sin(angle) * 3.1;
            level.sendParticles(ModParticles.NIHILITY_SHARD.get(), rx, getY() + 1.15, rz, 2, 0.08, 0.08, 0.08, 0.03);
        }
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.1F, 0.78F);
    }

    private void executeCounterCleave(ServerLevel level, LivingEntity attacker) {
        setHalberdStance(STANCE_COUNTER_SLAM, 14);
        swing(InteractionHand.MAIN_HAND);
        getLookControl().setLookAt(attacker, 60.0F, 60.0F);
        float counterDmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.65F;
        if (dealVoidStrike(level, attacker, counterDmg, 1.35F, true)) {
            Vec3 push = attacker.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-5) {
                push = push.normalize();
                attacker.setDeltaMovement(push.x * 0.92, 0.34, push.z * 0.92);
                attacker.hurtMarked = true;
            }
            attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
        }
        level.sendParticles(ModParticles.CHAOS_GLINT.get(),
            attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 14, 0.45, 0.45, 0.45, 0.08);
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.9F, 1.15F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean parrying = getHalberdStance() == STANCE_GUARD_PARRY
            && amount > 0.0F
            && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        float adjusted = parrying ? amount * 0.35F : amount;
        boolean hurt = super.hurtServer(level, source, adjusted);
        if (hurt && amount > 0.0F && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (parrying) {
                level.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.0F, 0.95F);
                if (source.getEntity() instanceof LivingEntity attacker
                    && attacker != this
                    && !(attacker instanceof NihilityCreature)
                    && attacker.isAlive()
                    && distanceToSqr(attacker) <= 4.6 * 4.6) {
                    executeCounterCleave(level, attacker);
                }
            } else {
                long now = level.getGameTime();
                if (now > burstWindowResetTick) {
                    burstDamageWindow = 0.0F;
                    burstWindowResetTick = now + 60L;
                }
                burstDamageWindow += amount;
                if (burstDamageWindow >= 45.0F && now >= nextParryAllowedTick && getHalberdStance() == STANCE_IDLE) {
                    burstDamageWindow = 0.0F;
                    nextParryAllowedTick = now + 120L;
                    setHalberdStance(STANCE_GUARD_PARRY, 26);
                    level.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 0.85F, 1.25F);
                }
            }
        }
        return hurt;
    }

    public boolean canGuardProjectile() {
        return isAlive() && !hasActiveCreatureAction()
            && level().getGameTime() >= nextProjectileGuardGameTime;
    }

    public boolean guardProjectile(Projectile projectile, NihilityCreature protectedAlly) {
        if (!canGuardProjectile() || projectile == null || !projectile.isAlive()) {
            return false;
        }
        long gameTime = level().getGameTime();
        nextProjectileGuardGameTime = gameTime + CreatureProjectileDefensePolicy.FOLLOWER_GUARD_COOLDOWN_TICKS;
        entityData.set(DATA_GUARD_UNTIL,
            gameTime + CreatureProjectileDefensePolicy.FOLLOWER_GUARD_REACTION_TICKS);
        NihilityVisualEffects.followerProjectileGuard(this, protectedAlly, projectile.position());
        projectile.discard();
        return true;
    }

    public boolean isProjectileGuarding() {
        return level().getGameTime() < entityData.get(DATA_GUARD_UNTIL);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("TempleFollowerGuardCooldownUntil", nextProjectileGuardGameTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nextProjectileGuardGameTime = input.getLongOr("TempleFollowerGuardCooldownUntil", 0L);
        entityData.set(DATA_GUARD_UNTIL, 0L);
        setHalberdStance(STANCE_IDLE, 0);
    }

    private static final class FollowerHalberdGoal extends Goal {
        private final NihilityFollower follower;
        private int sweepCooldown;

        FollowerHalberdGoal(NihilityFollower follower) {
            this.follower = follower;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = follower.getTarget();
            return target != null && target.isAlive() && follower.getDisposition().isCombat();
        }

        @Override
        public void tick() {
            LivingEntity target = follower.getTarget();
            if (target == null || !target.isAlive() || !(follower.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            follower.getLookControl().setLookAt(target, 35.0F, 35.0F);
            if (sweepCooldown > 0) sweepCooldown--;

            int stance = follower.getHalberdStance();
            if (stance == STANCE_GUARD_PARRY) {
                follower.getNavigation().stop();
                return;
            }

            double distSq = follower.distanceToSqr(target);
            if (distSq > 3.1 * 3.1) {
                follower.getNavigation().moveTo(target, 1.24);
            } else {
                follower.getNavigation().stop();
            }

            if (stance == STANCE_IDLE && sweepCooldown <= 0 && distSq <= 3.7 * 3.7) {
                follower.beginHalberdSweep(serverLevel, target);
                sweepCooldown = 26;
            }
        }
    }
}
