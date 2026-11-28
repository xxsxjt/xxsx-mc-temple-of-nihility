package com.templenihility.entity;

import com.templenihility.init.ModEntities;
import com.templenihility.init.ModParticles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Physical 3D spell & detached-armament entity shared across all Temple of Nihility guardians:
 *  - FLOATING_SWORD  (Saint):       Detached shoulder greatsword curving out & returning to dock.
 *  - WING_FEATHER    (Saint):       Homing obsidian feather-blade fired from the 6 Seraphic Wings.
 *  - JUDGMENT_LANCE  (Saint):       Sky-falling Astrolabe spear with 14-tick rune-circle telegraph.
 *  - CRYSTAL_BOLT    (Believer):    Curved obsidian-cyan crystal bolt cast from the Focus Orb.
 *  - MONOLITH_PILLAR (Envoy):       Detached runic tablet that flanks target, locks on, then slams inward in a pincer clamp!
 *  - AEGIS_SHIELD    (Grand Envoy): Detached orbiting Aegis Shield Pillar hurled with shockwave impact & boomerang return!
 */
public class VoidSpellEntity extends Entity {
    public enum SpellKind {
        FLOATING_SWORD,
        WING_FEATHER,
        JUDGMENT_LANCE,
        CRYSTAL_BOLT,
        MONOLITH_PILLAR,
        AEGIS_SHIELD;

        public static SpellKind byId(int id) {
            SpellKind[] values = values();
            return id >= 0 && id < values.length ? values[id] : WING_FEATHER;
        }
    }

    private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(
        VoidSpellEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(
        VoidSpellEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DROPPING = SynchedEntityData.defineId(
        VoidSpellEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerUuid;
    private int targetEntityId = -1;
    private float baseDamage = 18.0F;
    private float erosionMultiplier = 1.0F;
    private int maxLifeTicks = 44;
    private double curveSign = 1.0;
    private Vec3 groundTarget = Vec3.ZERO;
    private boolean lanceImpacted;
    private boolean returnNotified;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public VoidSpellEntity(EntityType<? extends VoidSpellEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static VoidSpellEntity createFloatingSword(
        ServerLevel level,
        NihilityCreature owner,
        LivingEntity target,
        Vec3 spawnPos,
        double curveSign,
        float baseDamage
    ) {
        VoidSpellEntity entity = new VoidSpellEntity(ModEntities.VOID_SPELL.get(), level);
        entity.setSpellKind(SpellKind.FLOATING_SWORD);
        entity.ownerUuid = owner.getUUID();
        entity.targetEntityId = target != null ? target.getId() : -1;
        entity.baseDamage = baseDamage;
        entity.erosionMultiplier = 1.35F;
        entity.maxLifeTicks = 34;
        entity.curveSign = curveSign;
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        Vec3 aim = target != null
            ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(spawnPos)
            : owner.getLookAngle();
        if (aim.lengthSqr() < 1.0E-5) {
            aim = new Vec3(0.0, 0.0, 1.0);
        }
        Vec3 fwd = aim.normalize();
        Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x);
        if (side.lengthSqr() > 1.0E-5) {
            side = side.normalize();
        }
        Vec3 velocity = fwd.scale(1.18).add(side.scale(curveSign * 0.42));
        entity.setDeltaMovement(velocity);
        entity.updateRotationFromMotion(velocity);
        return entity;
    }

    public static VoidSpellEntity createWingFeather(
        ServerLevel level,
        NihilityCreature owner,
        LivingEntity target,
        Vec3 spawnPos,
        Vec3 initialVelocity,
        float baseDamage
    ) {
        VoidSpellEntity entity = new VoidSpellEntity(ModEntities.VOID_SPELL.get(), level);
        entity.setSpellKind(SpellKind.WING_FEATHER);
        entity.ownerUuid = owner.getUUID();
        entity.targetEntityId = target != null ? target.getId() : -1;
        entity.baseDamage = baseDamage;
        entity.erosionMultiplier = 0.90F;
        entity.maxLifeTicks = 50;
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.setDeltaMovement(initialVelocity);
        entity.updateRotationFromMotion(initialVelocity);
        return entity;
    }

    public static VoidSpellEntity createJudgmentLance(
        ServerLevel level,
        NihilityCreature owner,
        Vec3 impactGroundPos,
        float baseDamage
    ) {
        VoidSpellEntity entity = new VoidSpellEntity(ModEntities.VOID_SPELL.get(), level);
        entity.setSpellKind(SpellKind.JUDGMENT_LANCE);
        entity.ownerUuid = owner.getUUID();
        entity.baseDamage = baseDamage;
        entity.erosionMultiplier = 1.45F;
        entity.maxLifeTicks = 30;
        entity.groundTarget = impactGroundPos;
        entity.setPos(impactGroundPos.x, impactGroundPos.y + 13.5, impactGroundPos.z);
        entity.setDeltaMovement(Vec3.ZERO);
        return entity;
    }

    public static VoidSpellEntity createCrystalBolt(
        ServerLevel level,
        NihilityCreature owner,
        LivingEntity target,
        Vec3 spawnPos,
        Vec3 initialVelocity,
        float baseDamage
    ) {
        VoidSpellEntity entity = new VoidSpellEntity(ModEntities.VOID_SPELL.get(), level);
        entity.setSpellKind(SpellKind.CRYSTAL_BOLT);
        entity.ownerUuid = owner.getUUID();
        entity.targetEntityId = target != null ? target.getId() : -1;
        entity.baseDamage = baseDamage;
        entity.erosionMultiplier = 0.85F;
        entity.maxLifeTicks = 44;
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.setDeltaMovement(initialVelocity);
        entity.updateRotationFromMotion(initialVelocity);
        return entity;
    }

    public static VoidSpellEntity createMonolithPillar(
        ServerLevel level,
        NihilityCreature owner,
        LivingEntity target,
        Vec3 flankSpawnPos,
        Vec3 clampCenter,
        float baseDamage
    ) {
        VoidSpellEntity entity = new VoidSpellEntity(ModEntities.VOID_SPELL.get(), level);
        entity.setSpellKind(SpellKind.MONOLITH_PILLAR);
        entity.ownerUuid = owner.getUUID();
        entity.targetEntityId = target != null ? target.getId() : -1;
        entity.baseDamage = baseDamage;
        entity.erosionMultiplier = 1.25F;
        entity.maxLifeTicks = 36;
        entity.groundTarget = clampCenter;
        entity.setPos(flankSpawnPos.x, flankSpawnPos.y, flankSpawnPos.z);
        Vec3 face = clampCenter.subtract(flankSpawnPos);
        if (face.lengthSqr() > 1.0E-5) {
            entity.updateRotationFromMotion(face);
        }
        entity.setDeltaMovement(Vec3.ZERO);
        return entity;
    }

    public static VoidSpellEntity createAegisShield(
        ServerLevel level,
        NihilityCreature owner,
        LivingEntity target,
        Vec3 spawnPos,
        double curveSign,
        float baseDamage
    ) {
        VoidSpellEntity entity = new VoidSpellEntity(ModEntities.VOID_SPELL.get(), level);
        entity.setSpellKind(SpellKind.AEGIS_SHIELD);
        entity.ownerUuid = owner.getUUID();
        entity.targetEntityId = target != null ? target.getId() : -1;
        entity.baseDamage = baseDamage;
        entity.erosionMultiplier = 1.20F;
        entity.maxLifeTicks = 42;
        entity.curveSign = curveSign;
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        Vec3 aim = target != null
            ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(spawnPos)
            : owner.getLookAngle();
        if (aim.lengthSqr() < 1.0E-5) {
            aim = new Vec3(0.0, 0.0, 1.0);
        }
        Vec3 fwd = aim.normalize();
        Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x);
        if (side.lengthSqr() > 1.0E-5) {
            side = side.normalize();
        }
        Vec3 velocity = fwd.scale(1.08).add(side.scale(curveSign * 0.28));
        entity.setDeltaMovement(velocity);
        entity.updateRotationFromMotion(velocity);
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, (byte) SpellKind.WING_FEATHER.ordinal());
        builder.define(DATA_RETURNING, false);
        builder.define(DATA_DROPPING, false);
    }

    public SpellKind getSpellKind() {
        return SpellKind.byId(entityData.get(DATA_KIND));
    }

    public void setSpellKind(SpellKind kind) {
        entityData.set(DATA_KIND, (byte) kind.ordinal());
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    public boolean isDropping() {
        return entityData.get(DATA_DROPPING);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            Vec3 vel = getDeltaMovement();
            if (vel.lengthSqr() > 1.0E-5) {
                setPos(getX() + vel.x, getY() + vel.y, getZ() + vel.z);
            }
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        NihilityCreature owner = resolveOwner(serverLevel);
        if (owner == null || !owner.isAlive() || tickCount > maxLifeTicks) {
            notifyOwnerReturned(owner);
            discard();
            return;
        }

        switch (getSpellKind()) {
            case FLOATING_SWORD -> tickFloatingSword(serverLevel, owner);
            case WING_FEATHER -> tickWingFeather(serverLevel, owner);
            case JUDGMENT_LANCE -> tickJudgmentLance(serverLevel, owner);
            case CRYSTAL_BOLT -> tickCrystalBolt(serverLevel, owner);
            case MONOLITH_PILLAR -> tickMonolithPillar(serverLevel, owner);
            case AEGIS_SHIELD -> tickAegisShield(serverLevel, owner);
        }
    }

    private void notifyOwnerReturned(NihilityCreature owner) {
        if (returnNotified || owner == null) {
            return;
        }
        returnNotified = true;
        if (getSpellKind() == SpellKind.AEGIS_SHIELD && owner instanceof NihilityGrandEnvoy grand) {
            grand.onAegisShieldReturned();
        } else if (getSpellKind() == SpellKind.MONOLITH_PILLAR && owner instanceof NihilityEnvoy envoy) {
            envoy.onMonolithReturned();
        }
    }

    private void tickFloatingSword(ServerLevel level, NihilityCreature owner) {
        LivingEntity target = resolveTarget(level, owner);
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        if (tickCount < 16) {
            if (target != null && target.isAlive()) {
                Vec3 toTarget = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(pos);
                if (toTarget.lengthSqr() > 0.25) {
                    Vec3 dir = toTarget.normalize();
                    Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
                    if (side.lengthSqr() > 1.0E-5) {
                        side = side.normalize();
                    }
                    double curveFade = Math.max(0.0, 1.0 - tickCount / 11.0);
                    Vec3 steer = dir.scale(1.22).add(side.scale(curveSign * 0.32 * curveFade));
                    vel = vel.scale(0.45).add(steer.scale(0.55));
                }
            }
        } else {
            if (!isReturning()) {
                entityData.set(DATA_RETURNING, true);
                hitEntityIds.clear();
            }
            Vec3 home = owner.position().add(0.0, owner.getBbHeight() * 0.55, 0.0);
            Vec3 toOwner = home.subtract(pos);
            if (toOwner.lengthSqr() <= 2.2 * 2.2) {
                discard();
                return;
            }
            vel = toOwner.normalize().scale(1.42);
        }
        Vec3 next = pos.add(vel);
        setPos(next.x, next.y, next.z);
        setDeltaMovement(vel);
        updateRotationFromMotion(vel);

        AABB sweep = new AABB(pos, next).inflate(1.25, 1.0, 1.25);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, sweep,
            e -> isValidVictim(owner, e) && !hitEntityIds.contains(e.getId()));
        float hitDmg = isReturning() ? baseDamage * 0.72F : baseDamage;
        for (LivingEntity victim : victims) {
            hitEntityIds.add(victim.getId());
            if (owner.dealVoidStrike(level, victim, hitDmg, erosionMultiplier, true)) {
                if (owner instanceof NihilitySaint saint) {
                    saint.onSpellHitTarget(victim, 1);
                }
                level.sendParticles(ModParticles.NIHILITY_SHARD.get(),
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                    10, 0.35, 0.35, 0.35, 0.09);
            }
        }
        if (tickCount % 2 == 0) {
            level.sendParticles(ModParticles.NIHILITY_MOTE.get(), next.x, next.y, next.z,
                3, 0.14, 0.14, 0.14, 0.02);
        }
    }

    private void tickWingFeather(ServerLevel level, NihilityCreature owner) {
        LivingEntity target = resolveTarget(level, owner);
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        if (target != null && target.isAlive() && tickCount >= 3) {
            Vec3 desired = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(pos);
            if (desired.lengthSqr() > 0.04) {
                Vec3 dir = desired.normalize();
                double speed = Math.min(1.55, Math.max(0.95, vel.length() + 0.06));
                vel = vel.normalize().scale(0.72).add(dir.scale(0.28)).normalize().scale(speed);
            }
        }
        Vec3 next = pos.add(vel);
        setPos(next.x, next.y, next.z);
        setDeltaMovement(vel);
        updateRotationFromMotion(vel);

        AABB hitBox = new AABB(pos, next).inflate(0.72, 0.72, 0.72);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, hitBox,
            e -> isValidVictim(owner, e));
        if (!victims.isEmpty()) {
            LivingEntity victim = victims.get(0);
            if (owner.dealVoidStrike(level, victim, baseDamage, erosionMultiplier, true)) {
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 0, false, false));
                if (owner instanceof NihilitySaint saint) {
                    saint.onSpellHitTarget(victim, 1);
                }
            }
            level.sendParticles(ModParticles.NIHILITY_SHARD.get(),
                next.x, next.y, next.z, 8, 0.22, 0.22, 0.22, 0.08);
            level.playSound(null, next.x, next.y, next.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 0.65F, 1.45F);
            discard();
            return;
        }
        if (tickCount % 2 == 0) {
            level.sendParticles(ModParticles.NIHILITY_MOTE.get(), next.x, next.y, next.z,
                2, 0.06, 0.06, 0.06, 0.01);
        }
    }

    private void tickCrystalBolt(ServerLevel level, NihilityCreature owner) {
        LivingEntity target = resolveTarget(level, owner);
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        if (target != null && target.isAlive() && tickCount >= 2 && tickCount <= 18) {
            Vec3 desired = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(pos);
            if (desired.lengthSqr() > 0.04) {
                Vec3 dir = desired.normalize();
                double speed = Math.min(1.18, Math.max(0.78, vel.length() + 0.03));
                vel = vel.normalize().scale(0.84).add(dir.scale(0.16)).normalize().scale(speed);
            }
        }
        Vec3 next = pos.add(vel);
        setPos(next.x, next.y, next.z);
        setDeltaMovement(vel);
        updateRotationFromMotion(vel);

        AABB hitBox = new AABB(pos, next).inflate(0.65, 0.65, 0.65);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, hitBox,
            e -> isValidVictim(owner, e));
        if (!victims.isEmpty()) {
            LivingEntity victim = victims.get(0);
            if (owner.dealVoidStrike(level, victim, baseDamage, erosionMultiplier, true)) {
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0, false, false));
            }
            level.sendParticles(ModParticles.NIHILITY_SHARD.get(),
                next.x, next.y, next.z, 10, 0.25, 0.25, 0.25, 0.09);
            level.playSound(null, next.x, next.y, next.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 0.7F, 1.25F);
            discard();
            return;
        }
        if (tickCount % 2 == 0) {
            level.sendParticles(ModParticles.NIHILITY_MOTE.get(), next.x, next.y, next.z,
                2, 0.08, 0.08, 0.08, 0.02);
        }
    }

    private void tickMonolithPillar(ServerLevel level, NihilityCreature owner) {
        Vec3 pos = position();
        if (tickCount < 8) {
            // Telegraph phase: hovering runic monolith hums & draws cyan lock-on beam to clamp center!
            if (tickCount % 2 == 0) {
                Vec3 beam = groundTarget.subtract(pos);
                for (int i = 1; i <= 4; i++) {
                    Vec3 p = pos.add(beam.scale(i / 4.0));
                    level.sendParticles(ModParticles.NIHILITY_MOTE.get(), p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
                }
            }
            return;
        }
        if (tickCount == 8) {
            entityData.set(DATA_DROPPING, true);
            Vec3 rush = groundTarget.subtract(pos);
            if (rush.lengthSqr() > 1.0E-5) {
                Vec3 vel = rush.normalize().scale(1.52);
                setDeltaMovement(vel);
                updateRotationFromMotion(vel);
            }
            level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.85F, 1.45F);
        }
        if (tickCount < 16) {
            // High-speed inward pincer clamp!
            Vec3 vel = getDeltaMovement();
            Vec3 next = pos.add(vel);
            setPos(next.x, next.y, next.z);
            AABB sweep = new AABB(pos, next).inflate(1.15, 1.35, 1.15);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> isValidVictim(owner, e) && !hitEntityIds.contains(e.getId()))) {
                hitEntityIds.add(victim.getId());
                if (owner.dealVoidStrike(level, victim, baseDamage, erosionMultiplier, true)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, false, false));
                    level.sendParticles(ModParticles.NIHILITY_SHARD.get(),
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                        12, 0.35, 0.45, 0.35, 0.12);
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.55F, 1.55F);
                }
            }
            level.sendParticles(ModParticles.NIHILITY_MOTE.get(), next.x, next.y, next.z,
                4, 0.15, 0.28, 0.15, 0.03);
        } else {
            // Return smoothly back to Envoy's flanks
            if (!isReturning()) {
                entityData.set(DATA_RETURNING, true);
            }
            Vec3 home = owner.position().add(0.0, owner.getBbHeight() * 0.55, 0.0);
            Vec3 toOwner = home.subtract(pos);
            if (toOwner.lengthSqr() <= 2.2 * 2.2) {
                notifyOwnerReturned(owner);
                discard();
                return;
            }
            Vec3 vel = toOwner.normalize().scale(1.45);
            setDeltaMovement(vel);
            setPos(pos.x + vel.x, pos.y + vel.y, pos.z + vel.z);
            updateRotationFromMotion(vel);
        }
    }

    private void tickAegisShield(ServerLevel level, NihilityCreature owner) {
        LivingEntity target = resolveTarget(level, owner);
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        if (!isReturning() && tickCount < 18) {
            if (target != null && target.isAlive()) {
                Vec3 toTarget = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(pos);
                if (toTarget.lengthSqr() > 0.25) {
                    Vec3 dir = toTarget.normalize();
                    Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
                    if (side.lengthSqr() > 1.0E-5) {
                        side = side.normalize();
                    }
                    double curveFade = Math.max(0.0, 1.0 - tickCount / 14.0);
                    Vec3 steer = dir.scale(1.12).add(side.scale(curveSign * 0.26 * curveFade));
                    vel = vel.scale(0.55).add(steer.scale(0.45));
                }
            }
            Vec3 next = pos.add(vel);
            setPos(next.x, next.y, next.z);
            setDeltaMovement(vel);
            updateRotationFromMotion(vel);

            AABB hitBox = new AABB(pos, next).inflate(1.15, 1.25, 1.15);
            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> isValidVictim(owner, e) && !hitEntityIds.contains(e.getId()));
            if (!victims.isEmpty()) {
                // Shockwave smash on impact, then immediately boomerang back to Grand Envoy!
                AABB blast = new AABB(next, next).inflate(2.8, 2.2, 2.8);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, blast,
                    e -> isValidVictim(owner, e) && !hitEntityIds.contains(e.getId()))) {
                    hitEntityIds.add(victim.getId());
                    if (owner.dealVoidStrike(level, victim, baseDamage, erosionMultiplier, true)) {
                        Vec3 push = victim.position().subtract(next).multiply(1.0, 0.0, 1.0);
                        if (push.lengthSqr() > 1.0E-5) {
                            push = push.normalize();
                            victim.setDeltaMovement(push.x * 0.85, Math.max(0.32, victim.getDeltaMovement().y + 0.18), push.z * 0.85);
                            victim.hurtMarked = true;
                        }
                        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, false, false));
                    }
                }
                level.sendParticles(ModParticles.CHAOS_GLINT.get(), next.x, next.y, next.z, 14, 0.65, 0.45, 0.65, 0.08);
                level.sendParticles(ModParticles.NIHILITY_SHARD.get(), next.x, next.y, next.z, 12, 0.55, 0.45, 0.55, 0.1);
                level.playSound(null, next.x, next.y, next.z,
                    SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.1F, 0.72F);
                entityData.set(DATA_RETURNING, true);
            }
            if (tickCount % 2 == 0) {
                level.sendParticles(ModParticles.CHAOS_GLINT.get(), next.x, next.y, next.z, 2, 0.14, 0.22, 0.14, 0.02);
            }
        } else {
            if (!isReturning()) {
                entityData.set(DATA_RETURNING, true);
            }
            Vec3 home = owner.position().add(0.0, owner.getBbHeight() * 0.55, 0.0);
            Vec3 toOwner = home.subtract(pos);
            if (toOwner.lengthSqr() <= 2.3 * 2.3) {
                notifyOwnerReturned(owner);
                discard();
                return;
            }
            Vec3 retVel = toOwner.normalize().scale(1.38);
            setPos(pos.x + retVel.x, pos.y + retVel.y, pos.z + retVel.z);
            setDeltaMovement(retVel);
            updateRotationFromMotion(retVel);
            if (tickCount % 3 == 0) {
                level.sendParticles(ModParticles.NIHILITY_MOTE.get(), getX(), getY(), getZ(), 2, 0.1, 0.15, 0.1, 0.01);
            }
        }
    }

    private void tickJudgmentLance(ServerLevel level, NihilityCreature owner) {
        if (tickCount < 14) {
            double radius = 3.1;
            int pts = 14;
            double rot = tickCount * 0.22;
            for (int i = 0; i < pts; i++) {
                double a = rot + (i * Math.PI * 2.0 / pts);
                double rx = groundTarget.x + Math.cos(a) * radius;
                double rz = groundTarget.z + Math.sin(a) * radius;
                level.sendParticles(i % 2 == 0 ? ModParticles.CHAOS_GLINT.get() : ModParticles.NIHILITY_MOTE.get(),
                    rx, groundTarget.y + 0.14, rz, 1, 0.0, 0.02, 0.0, 0.0);
            }
            for (double y = 0.5; y <= 13.0; y += 2.2) {
                level.sendParticles(ModParticles.NIHILITY_MOTE.get(),
                    groundTarget.x, groundTarget.y + y, groundTarget.z, 1, 0.05, 0.0, 0.05, 0.0);
            }
            return;
        }
        if (!isDropping()) {
            entityData.set(DATA_DROPPING, true);
            level.playSound(null, groundTarget.x, groundTarget.y, groundTarget.z,
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 1.1F, 1.35F);
        }
        if (!lanceImpacted) {
            Vec3 pos = position();
            double nextY = pos.y - 3.6;
            if (nextY <= groundTarget.y + 0.3) {
                setPos(groundTarget.x, groundTarget.y + 0.3, groundTarget.z);
                lanceImpacted = true;
                detonateJudgmentLance(level, owner);
            } else {
                setPos(groundTarget.x, nextY, groundTarget.z);
                level.sendParticles(ModParticles.CHAOS_GLINT.get(),
                    groundTarget.x, nextY, groundTarget.z, 5, 0.15, 0.4, 0.15, 0.04);
            }
        }
    }

    private void detonateJudgmentLance(ServerLevel level, NihilityCreature owner) {
        double radius = 3.4;
        AABB area = new AABB(groundTarget, groundTarget).inflate(radius, 4.0, radius);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area,
            e -> isValidVictim(owner, e) && e.distanceToSqr(groundTarget) <= radius * radius);
        for (LivingEntity victim : victims) {
            owner.dealVoidStrike(level, victim, baseDamage, erosionMultiplier, true);
            Vec3 push = victim.position().subtract(groundTarget).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-5) {
                push = push.normalize();
                victim.setDeltaMovement(push.x * 0.72, Math.max(0.32, victim.getDeltaMovement().y + 0.2), push.z * 0.72);
                victim.hurtMarked = true;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 55, 1, false, false));
        }
        for (double y = 0.2; y <= 11.0; y += 0.65) {
            level.sendParticles(ModParticles.CHAOS_GLINT.get(),
                groundTarget.x, groundTarget.y + y, groundTarget.z, 3, 0.28, 0.12, 0.28, 0.03);
            level.sendParticles(ModParticles.NIHILITY_SHARD.get(),
                groundTarget.x, groundTarget.y + y, groundTarget.z, 2, 0.35, 0.12, 0.35, 0.05);
        }
        level.playSound(null, groundTarget.x, groundTarget.y, groundTarget.z,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.95F, 1.25F);
    }

    private boolean isValidVictim(NihilityCreature owner, LivingEntity candidate) {
        if (!candidate.isAlive() || candidate == owner || candidate instanceof NihilityCreature) {
            return false;
        }
        if (candidate instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return owner.canAttack(candidate);
    }

    private NihilityCreature resolveOwner(ServerLevel level) {
        if (ownerUuid == null) {
            return null;
        }
        Entity e = level.getEntity(ownerUuid);
        return e instanceof NihilityCreature c ? c : null;
    }

    private LivingEntity resolveTarget(ServerLevel level, NihilityCreature owner) {
        if (targetEntityId >= 0) {
            Entity e = level.getEntity(targetEntityId);
            if (e instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return owner.getTarget();
    }

    private void updateRotationFromMotion(Vec3 vel) {
        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        float yaw = (float) (Math.atan2(vel.x, vel.z) * (180.0 / Math.PI));
        float pitch = (float) (Math.atan2(vel.y, horiz) * (180.0 / Math.PI));
        setYRot(yaw);
        setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setSpellKind(SpellKind.byId(input.getIntOr("SpellKind", SpellKind.WING_FEATHER.ordinal())));
        maxLifeTicks = input.getIntOr("MaxLifeTicks", 40);
        baseDamage = input.getFloatOr("BaseDamage", 18.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("SpellKind", getSpellKind().ordinal());
        output.putInt("MaxLifeTicks", maxLifeTicks);
        output.putFloat("BaseDamage", baseDamage);
    }
}
