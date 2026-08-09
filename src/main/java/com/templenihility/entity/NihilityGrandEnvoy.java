package com.templenihility.entity;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.GrandEnvoyActionPolicy;
import com.templenihility.action.GrandEnvoyDomainService;
import com.templenihility.world.NihilityVisualEffects;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NihilityGrandEnvoy extends NihilityCreature {
    private static final EntityDataAccessor<Integer> DATA_SHIELD_CHARGES = SynchedEntityData.defineId(
        NihilityGrandEnvoy.class, EntityDataSerializers.INT);

    private BlockPos domainOrigin = BlockPos.ZERO;
    private long domainUntilGameTime;

    public NihilityGrandEnvoy(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int getTier() {
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHIELD_CHARGES, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive() && level() instanceof ServerLevel serverLevel) {
            if (isDomainActive(serverLevel.getGameTime())) {
                GrandEnvoyDomainService.tickDomain(serverLevel, this);
            } else if (domainUntilGameTime != 0L) {
                domainUntilGameTime = 0L;
            }
            if (getRitualShieldCharges() > 0 && tickCount % 12 == 0) {
                NihilityVisualEffects.grandEnvoyShieldPulse(this, getRitualShieldCharges());
            }
        }
    }

    @Override
    protected ActionType selectCreatureAction(LivingEntity target, long gameTime) {
        double healthRatio = getMaxHealth() <= 0.0F ? 1.0 : getHealth() / getMaxHealth();
        return GrandEnvoyActionPolicy.select(
            healthRatio,
            getRitualShieldCharges(),
            hasRecallCandidate(target),
            distanceToSqr(target));
    }

    @Override
    protected void applyCustomCreatureAction(ServerLevel level, ActionType action,
                                             LivingEntity target, long elapsed) {
        if (!markCreatureActionApplied()) {
            return;
        }
        switch (action) {
            case GRAND_ENVOY_DOMAIN -> activateDomain(level);
            case GRAND_ENVOY_RECALL -> recallAllies(level, target);
            case GRAND_ENVOY_SHIELD -> activateRitualShield();
            default -> {
            }
        }
    }

    public int getRitualShieldCharges() {
        return Math.max(0, entityData.get(DATA_SHIELD_CHARGES));
    }

    public BlockPos getDomainOrigin() {
        return domainOrigin;
    }

    public boolean isDomainActive(long gameTime) {
        return domainUntilGameTime > gameTime;
    }

    private void activateDomain(ServerLevel level) {
        domainOrigin = blockPosition();
        domainUntilGameTime = level.getGameTime() + GrandEnvoyActionPolicy.DOMAIN_DURATION_TICKS;
        NihilityVisualEffects.grandEnvoyDomainStart(this, domainOrigin);
    }

    private void activateRitualShield() {
        setRitualShieldCharges(GrandEnvoyActionPolicy.SHIELD_CHARGES);
        NihilityVisualEffects.grandEnvoyShieldRaised(this, GrandEnvoyActionPolicy.SHIELD_CHARGES);
    }

    private boolean hasRecallCandidate(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return !recallCandidates(serverLevel, target).isEmpty();
    }

    private void recallAllies(ServerLevel level, LivingEntity target) {
        List<NihilityCreature> candidates = recallCandidates(level, target);
        int recalled = 0;
        for (NihilityCreature ally : candidates) {
            if (recalled >= GrandEnvoyActionPolicy.MAX_RECALLED_ALLIES) {
                break;
            }
            Vec3 destination = findRecallDestination(level, ally, recalled);
            if (destination == null) {
                ally.getNavigation().moveTo(this, 1.15);
                continue;
            }
            Vec3 from = ally.position();
            ally.teleportTo(destination.x, destination.y, destination.z);
            ally.setDeltaMovement(Vec3.ZERO);
            ally.hurtMarked = true;
            ally.getNavigation().stop();
            ally.setTarget(target);
            NihilityVisualEffects.grandEnvoyRecall(this, ally, from, destination);
            recalled++;
        }
    }

    private List<NihilityCreature> recallCandidates(ServerLevel level, LivingEntity target) {
        return level.getEntitiesOfClass(NihilityCreature.class, getBoundingBox().inflate(24.0, 10.0, 24.0),
                ally -> ally != this
                    && ally.isAlive()
                    && ally.getTier() < getTier()
                    && ally.getTarget() == target
                    && ally.distanceToSqr(this) >= 7.0 * 7.0)
            .stream()
            .sorted(Comparator.comparingDouble((NihilityCreature ally) -> ally.distanceToSqr(this)).reversed())
            .toList();
    }

    private Vec3 findRecallDestination(ServerLevel level, NihilityCreature ally, int index) {
        Vec3 origin = position();
        AABB baseBox = ally.getBoundingBox();
        for (double radius : new double[] {2.4, 3.2}) {
            for (int offset = 0; offset < 8; offset++) {
                double angle = (index * 2 + offset) * Math.PI / 4.0;
                Vec3 candidate = origin.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                AABB moved = baseBox.move(candidate.subtract(ally.position())).deflate(0.03);
                if (level.noCollision(ally, moved) && level.noBorderCollision(ally, moved)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void setRitualShieldCharges(int charges) {
        entityData.set(DATA_SHIELD_CHARGES,
            Math.max(0, Math.min(GrandEnvoyActionPolicy.SHIELD_CHARGES, charges)));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        int charges = getRitualShieldCharges();
        boolean shielded = charges > 0
            && amount > 0.0F
            && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        boolean hurt = super.hurtServer(level, source,
            shielded ? GrandEnvoyActionPolicy.shieldedDamage(amount) : amount);
        if (hurt && shielded) {
            setRitualShieldCharges(charges - 1);
            NihilityVisualEffects.grandEnvoyShieldImpact(this, charges - 1);
        }
        return hurt;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TempleGrandEnvoyShieldCharges", getRitualShieldCharges());
        if (domainUntilGameTime > 0L) {
            output.putLong("TempleGrandEnvoyDomainUntil", domainUntilGameTime);
            output.putInt("TempleGrandEnvoyDomainX", domainOrigin.getX());
            output.putInt("TempleGrandEnvoyDomainY", domainOrigin.getY());
            output.putInt("TempleGrandEnvoyDomainZ", domainOrigin.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setRitualShieldCharges(input.getIntOr("TempleGrandEnvoyShieldCharges", 0));
        domainUntilGameTime = input.getLongOr("TempleGrandEnvoyDomainUntil", 0L);
        domainOrigin = new BlockPos(
            input.getIntOr("TempleGrandEnvoyDomainX", blockPosition().getX()),
            input.getIntOr("TempleGrandEnvoyDomainY", blockPosition().getY()),
            input.getIntOr("TempleGrandEnvoyDomainZ", blockPosition().getZ()));
    }
}
