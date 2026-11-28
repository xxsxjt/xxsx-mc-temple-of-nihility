package com.templenihility.entity;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.GrandEnvoyActionPolicy;
import com.templenihility.action.GrandEnvoyDomainService;
import com.templenihility.init.ModParticles;
import com.templenihility.world.NihilityVisualEffects;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Archetype: Quad-Aegis Ritual Commander (虚无大使其 · 四盾法阵统御指挥官).
 * Floats regally at mid-long range, physically hurls 1~2 of its 4 orbiting Aegis Shield Pillars
 * (visually removing them from its orbit until they smash & boomerang back!), expands all 4
 * pillars into a cardinal ritual array during Void Domain, channels reflective Ritual Shields,
 * and recalls outer Temple allies to form a defensive phalanx!
 */
public class NihilityGrandEnvoy extends NihilityCreature {
    private static final EntityDataAccessor<Integer> DATA_SHIELD_CHARGES = SynchedEntityData.defineId(
        NihilityGrandEnvoy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEPLOYED_SHIELDS = SynchedEntityData.defineId(
        NihilityGrandEnvoy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COMMAND_TICKS = SynchedEntityData.defineId(
        NihilityGrandEnvoy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DOMAIN_ACTIVE = SynchedEntityData.defineId(
        NihilityGrandEnvoy.class, EntityDataSerializers.BOOLEAN);

    private BlockPos domainOrigin = BlockPos.ZERO;
    private long domainUntilGameTime;
    private int shieldSafetyTicks;

    public NihilityGrandEnvoy(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected boolean usesDefaultMeleeGoal() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new GrandEnvoyTacticianGoal(this));
    }

    @Override
    public int getTier() {
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHIELD_CHARGES, 0);
        builder.define(DATA_DEPLOYED_SHIELDS, 0);
        builder.define(DATA_COMMAND_TICKS, 0);
        builder.define(DATA_DOMAIN_ACTIVE, false);
    }

    public int getDeployedShields() {
        return entityData.get(DATA_DEPLOYED_SHIELDS);
    }

    public int getCommandTicks() {
        return entityData.get(DATA_COMMAND_TICKS);
    }

    public boolean isDomainActiveClient() {
        return entityData.get(DATA_DOMAIN_ACTIVE);
    }

    public void onAegisShieldReturned() {
        int current = getDeployedShields();
        if (current > 0) {
            entityData.set(DATA_DEPLOYED_SHIELDS, current - 1);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive() && level() instanceof ServerLevel serverLevel) {
            int cmd = getCommandTicks();
            if (cmd > 0) {
                entityData.set(DATA_COMMAND_TICKS, cmd - 1);
            }
            if (shieldSafetyTicks > 0) {
                shieldSafetyTicks--;
                if (shieldSafetyTicks == 0) {
                    entityData.set(DATA_DEPLOYED_SHIELDS, 0);
                }
            }
            boolean domainOn = isDomainActive(serverLevel.getGameTime());
            if (domainOn != isDomainActiveClient()) {
                entityData.set(DATA_DOMAIN_ACTIVE, domainOn);
            }
            if (domainOn) {
                GrandEnvoyDomainService.tickDomain(serverLevel, this);
                // While Domain is active, periodically fire perimeter pursuit bolts from cardinal pillars!
                if (tickCount % 32 == 0) {
                    LivingEntity target = getTarget();
                    if (target != null && target.isAlive() && distanceToSqr(target) <= 20.0 * 20.0) {
                        fireDomainPursuitBolt(serverLevel, target);
                    }
                }
            } else if (domainUntilGameTime != 0L) {
                domainUntilGameTime = 0L;
            }
            if (getRitualShieldCharges() > 0 && tickCount % 12 == 0) {
                NihilityVisualEffects.grandEnvoyShieldPulse(this, getRitualShieldCharges());
            }
        }
    }

    private void hurlAegisShields(ServerLevel level, LivingEntity target, int count) {
        int available = Math.max(0, 4 - getDeployedShields());
        int toLaunch = Math.min(count, available);
        if (toLaunch <= 0) {
            return;
        }
        entityData.set(DATA_DEPLOYED_SHIELDS, getDeployedShields() + toLaunch);
        entityData.set(DATA_COMMAND_TICKS, 16);
        shieldSafetyTicks = 46;

        Vec3 look = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(getEyePosition());
        if (look.lengthSqr() < 1.0E-5) {
            look = getLookAngle();
        }
        Vec3 fwd = look.normalize();
        Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x).normalize();
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15F;

        for (int i = 0; i < toLaunch; i++) {
            double sign = (i % 2 == 0) ? 1.0 : -1.0;
            Vec3 spawn = getEyePosition().add(side.scale(sign * 1.35)).add(fwd.scale(0.6));
            VoidSpellEntity shield = VoidSpellEntity.createAegisShield(level, this, target, spawn, sign, dmg);
            level.addFreshEntity(shield);
            level.sendParticles(ModParticles.CHAOS_GLINT.get(), spawn.x, spawn.y, spawn.z, 8, 0.25, 0.35, 0.25, 0.06);
        }
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.05F, 0.72F);
    }

    private void fireDomainPursuitBolt(ServerLevel level, LivingEntity target) {
        double angle = (tickCount * 0.45) % (Math.PI * 2.0);
        Vec3 spawn = Vec3.atCenterOf(domainOrigin).add(Math.cos(angle) * 4.5, 2.2, Math.sin(angle) * 4.5);
        Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(spawn);
        if (aim.lengthSqr() < 1.0E-5) {
            return;
        }
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.78F;
        VoidSpellEntity bolt = VoidSpellEntity.createCrystalBolt(level, this, target, spawn, aim.normalize().scale(1.05), dmg);
        level.addFreshEntity(bolt);
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
        entityData.set(DATA_DOMAIN_ACTIVE, true);
        NihilityVisualEffects.grandEnvoyDomainStart(this, domainOrigin);
    }

    private void activateRitualShield() {
        setRitualShieldCharges(GrandEnvoyActionPolicy.SHIELD_CHARGES);
        heal(getMaxHealth() * 0.06F);
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
                ally.getNavigation().moveTo(this, 1.2);
                continue;
            }
            Vec3 from = ally.position();
            ally.teleportTo(destination.x, destination.y, destination.z);
            ally.setDeltaMovement(Vec3.ZERO);
            ally.hurtMarked = true;
            ally.getNavigation().stop();
            ally.setTarget(target);
            ally.heal(ally.getMaxHealth() * 0.20F);
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
        float incoming = amount;
        boolean hurt = super.hurtServer(level, source,
            shielded ? GrandEnvoyActionPolicy.shieldedDamage(amount) : amount);
        if (hurt && shielded) {
            setRitualShieldCharges(charges - 1);
            NihilityVisualEffects.grandEnvoyShieldImpact(this, charges - 1);
            if (source.getEntity() instanceof LivingEntity attacker
                && attacker != this
                && !(attacker instanceof NihilityCreature)
                && attacker.isAlive()
                && distanceToSqr(attacker) <= 7.0 * 7.0) {
                float reflectBase = Math.max(
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F,
                    Math.min(60.0F, incoming * GrandEnvoyActionPolicy.SHIELD_REFLECT_MULTIPLIER));
                dealVoidStrike(level, attacker, reflectBase, 1.0F, true);
            }
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
        entityData.set(DATA_DEPLOYED_SHIELDS, 0);
        entityData.set(DATA_COMMAND_TICKS, 0);
    }

    private static final class GrandEnvoyTacticianGoal extends Goal {
        private final NihilityGrandEnvoy grand;
        private int shieldHurlCooldown = 22;
        private int strafeDir = 1;
        private int strafeSwitch = 35;

        GrandEnvoyTacticianGoal(NihilityGrandEnvoy grand) {
            this.grand = grand;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = grand.getTarget();
            return target != null && target.isAlive() && grand.getDisposition().isCombat();
        }

        @Override
        public void tick() {
            LivingEntity target = grand.getTarget();
            if (target == null || !target.isAlive() || !(grand.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            grand.getLookControl().setLookAt(target, 35.0F, 35.0F);
            if (shieldHurlCooldown > 0) shieldHurlCooldown--;
            if (--strafeSwitch <= 0) {
                strafeDir = -strafeDir;
                strafeSwitch = 32 + grand.getRandom().nextInt(26);
            }

            double distSq = grand.distanceToSqr(target);
            // Regal mid-long range levitation control (6.5..13 blocks)
            if (distSq > 13.5 * 13.5) {
                grand.getNavigation().moveTo(target, 1.15);
            } else if (distSq < 6.0 * 6.0) {
                grand.getNavigation().stop();
                grand.getMoveControl().strafe(-0.75F, strafeDir * 0.55F);
            } else {
                grand.getNavigation().stop();
                grand.getMoveControl().strafe(0.0F, strafeDir * 0.60F);
            }

            // Physically hurl 1~2 orbiting Aegis Shield Pillars!
            if (shieldHurlCooldown <= 0 && distSq <= 19.0 * 19.0 && grand.getDeployedShields() <= 2) {
                int launchCount = grand.isDomainActive(serverLevel.getGameTime()) ? 2 : 1;
                grand.hurlAegisShields(serverLevel, target, launchCount);
                shieldHurlCooldown = grand.isDomainActive(serverLevel.getGameTime()) ? 44 : 52;
            }
        }
    }
}
