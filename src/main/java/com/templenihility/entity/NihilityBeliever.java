package com.templenihility.entity;

import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import com.templenihility.init.ModParticles;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Archetype: Pure Ranged Focus-Crystal Caster (虚无信徒 · 远程浮游法晶术士).
 * Never walks in like a villager to punch — kites at 7..13 blocks, channels 3-round curved
 * 3D Crystal Bolts from its orbiting Focus Orb, plants delayed Ground Zero-Sigils, and
 * emits a Void Repulsion Pulse + back-hop whenever enemies close within 5 blocks.
 */
public class NihilityBeliever extends NihilityCreature {
    private static final EntityDataAccessor<Integer> DATA_CASTING_TICKS = SynchedEntityData.defineId(
        NihilityBeliever.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PULSE_TICKS = SynchedEntityData.defineId(
        NihilityBeliever.class, EntityDataSerializers.INT);

    private int pendingBolts;
    private int boltDelayTicks;
    private int castCount;
    private int sigilDelayTicks;
    private Vec3 sigilPos = Vec3.ZERO;

    public NihilityBeliever(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected boolean usesDefaultMeleeGoal() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING_TICKS, 0);
        builder.define(DATA_PULSE_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new BelieverCasterGoal(this));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(
            this,
            Player.class,
            target -> getDisposition() == Disposition.CALM && target.isSprinting(),
            5.0F,
            0.85,
            1.05,
            EntitySelector.NO_CREATIVE_OR_SPECTATOR));
    }

    @Override
    public int getTier() {
        return 1;
    }

    public int getCastingTicks() {
        return entityData.get(DATA_CASTING_TICKS);
    }

    public int getPulseTicks() {
        return entityData.get(DATA_PULSE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int cast = getCastingTicks();
        if (cast > 0) {
            entityData.set(DATA_CASTING_TICKS, cast - 1);
        }
        int pulse = getPulseTicks();
        if (pulse > 0) {
            entityData.set(DATA_PULSE_TICKS, pulse - 1);
        }

        // Staggered 3-round Focus-Crystal Bolt salvo!
        if (pendingBolts > 0) {
            if (boltDelayTicks > 0) {
                boltDelayTicks--;
            } else {
                LivingEntity target = getTarget();
                if (target != null && target.isAlive()) {
                    fireCrystalBolt(serverLevel, target, 3 - pendingBolts);
                }
                pendingBolts--;
                boltDelayTicks = 4;
            }
        }

        // Delayed Ground Zero-Sigil detonation!
        if (sigilDelayTicks > 0) {
            sigilDelayTicks--;
            double radius = 3.2;
            int pts = 12;
            double rot = tickCount * 0.25;
            for (int i = 0; i < pts; i++) {
                double a = rot + i * (Math.PI * 2.0 / pts);
                serverLevel.sendParticles(ModParticles.NIHILITY_MOTE.get(),
                    sigilPos.x + Math.cos(a) * radius, sigilPos.y + 0.12, sigilPos.z + Math.sin(a) * radius,
                    1, 0.0, 0.02, 0.0, 0.0);
            }
            if (sigilDelayTicks == 0) {
                detonateZeroSigil(serverLevel);
            }
        }
    }

    private void startCrystalSalvo(ServerLevel level, LivingEntity target) {
        castCount++;
        if (castCount % 3 == 0) {
            // Plant delayed Ground Zero-Sigil beneath target's predicted position!
            entityData.set(DATA_CASTING_TICKS, 18);
            sigilPos = target.position().add(target.getDeltaMovement().multiply(5.0, 0.0, 5.0));
            sigilDelayTicks = 12;
            level.playSound(null, sigilPos.x, sigilPos.y, sigilPos.z,
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 0.9F, 1.35F);
        } else {
            // Launch 3-bolt curved Focus-Crystal salvo!
            entityData.set(DATA_CASTING_TICKS, 16);
            pendingBolts = 3;
            boltDelayTicks = 2;
            level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9F, 1.2F);
        }
    }

    private void fireCrystalBolt(ServerLevel level, LivingEntity target, int index) {
        Vec3 look = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(getEyePosition());
        if (look.lengthSqr() < 1.0E-5) {
            look = getLookAngle();
        }
        Vec3 fwd = look.normalize();
        Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x);
        if (side.lengthSqr() > 1.0E-5) {
            side = side.normalize();
        }
        double fan = (index - 1) * 0.18;
        Vec3 spawn = getEyePosition().add(fwd.scale(0.8)).add(side.scale(-0.55));
        Vec3 vel = fwd.scale(0.92).add(side.scale(fan)).add(0.0, 0.04, 0.0);
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.72F;
        VoidSpellEntity bolt = VoidSpellEntity.createCrystalBolt(level, this, target, spawn, vel, dmg);
        level.addFreshEntity(bolt);
        level.sendParticles(ModParticles.NIHILITY_MOTE.get(), spawn.x, spawn.y, spawn.z, 5, 0.15, 0.15, 0.15, 0.03);
    }

    private void detonateZeroSigil(ServerLevel level) {
        double radius = 3.3;
        AABB area = new AABB(sigilPos, sigilPos).inflate(radius, 3.0, radius);
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25F;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e.isAlive() && e != this && !(e instanceof NihilityCreature) && canAttack(e)
                && e.distanceToSqr(sigilPos) <= radius * radius)) {
            if (dealVoidStrike(level, victim, dmg, 1.0F, true)) {
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 1, false, false));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
            }
        }
        for (double y = 0.2; y <= 3.6; y += 0.5) {
            level.sendParticles(ModParticles.NIHILITY_SHARD.get(),
                sigilPos.x, sigilPos.y + y, sigilPos.z, 6, 0.9, 0.1, 0.9, 0.08);
        }
        level.playSound(null, sigilPos.x, sigilPos.y, sigilPos.z,
            SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.0F, 0.85F);
    }

    private void triggerRepulsionPulse(ServerLevel level, LivingEntity threat) {
        entityData.set(DATA_PULSE_TICKS, 14);
        Vec3 away = position().subtract(threat.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() > 1.0E-5) {
            away = away.normalize();
            setDeltaMovement(away.x * 0.68, 0.26, away.z * 0.68);
            hurtMarked = true;
        }
        AABB box = getBoundingBox().inflate(4.4, 2.2, 4.4);
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != this && !(e instanceof NihilityCreature) && canAttack(e))) {
            dealVoidStrike(level, victim, dmg, 0.6F, false);
            Vec3 push = victim.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-5) {
                push = push.normalize();
                victim.setDeltaMovement(push.x * 0.85, 0.28, push.z * 0.85);
                victim.hurtMarked = true;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
        }
        for (int i = 0; i < 16; i++) {
            double a = i * (Math.PI * 2.0 / 16.0);
            level.sendParticles(ModParticles.NIHILITY_MOTE.get(),
                getX() + Math.cos(a) * 2.4, getY() + 1.0, getZ() + Math.sin(a) * 2.4,
                2, 0.1, 0.15, 0.1, 0.05);
        }
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 0.85F, 1.35F);
    }

    private static final class BelieverCasterGoal extends Goal {
        private final NihilityBeliever believer;
        private int castCooldown;
        private int pulseCooldown;
        private int strafeDir = 1;
        private int strafeSwitchTicks = 30;

        BelieverCasterGoal(NihilityBeliever believer) {
            this.believer = believer;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = believer.getTarget();
            return target != null && target.isAlive() && believer.getDisposition().isCombat();
        }

        @Override
        public void tick() {
            LivingEntity target = believer.getTarget();
            if (target == null || !target.isAlive() || !(believer.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            believer.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (castCooldown > 0) castCooldown--;
            if (pulseCooldown > 0) pulseCooldown--;
            if (--strafeSwitchTicks <= 0) {
                strafeDir = -strafeDir;
                strafeSwitchTicks = 28 + believer.getRandom().nextInt(26);
            }

            double distSq = believer.distanceToSqr(target);
            if (distSq < 4.8 * 4.8 && pulseCooldown <= 0) {
                believer.triggerRepulsionPulse(serverLevel, target);
                pulseCooldown = 70;
                return;
            }

            // Maintain crisp caster kiting band (7..13 blocks) with agile strafing
            if (distSq > 13.5 * 13.5) {
                believer.getNavigation().moveTo(target, 1.15);
            } else if (distSq < 6.8 * 6.8) {
                believer.getNavigation().stop();
                believer.getMoveControl().strafe(-0.85F, strafeDir * 0.65F);
            } else {
                believer.getNavigation().stop();
                believer.getMoveControl().strafe(0.0F, strafeDir * 0.72F);
            }

            if (castCooldown <= 0 && distSq <= 19.0 * 19.0 && believer.getSensing().hasLineOfSight(target)) {
                believer.startCrystalSalvo(serverLevel, target);
                castCooldown = 46;
            }
        }
    }
}
