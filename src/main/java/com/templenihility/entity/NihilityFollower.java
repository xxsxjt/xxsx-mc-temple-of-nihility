package com.templenihility.entity;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureProjectileDefensePolicy;
import com.templenihility.world.NihilityVisualEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NihilityFollower extends NihilityCreature {
    private static final EntityDataAccessor<Long> DATA_GUARD_UNTIL = SynchedEntityData.defineId(
        NihilityFollower.class, EntityDataSerializers.LONG);

    private long nextProjectileGuardGameTime;

    public NihilityFollower(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GUARD_UNTIL, 0L);
    }

    @Override
    protected ActionType selectCreatureAction(LivingEntity target, long gameTime) {
        double distanceSqr = distanceToSqr(target);
        return distanceSqr >= 2.25 * 2.25 && distanceSqr <= 5.0 * 5.0
            ? ActionType.FOLLOWER_INTERCEPT
            : ActionType.NONE;
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
    }
}
