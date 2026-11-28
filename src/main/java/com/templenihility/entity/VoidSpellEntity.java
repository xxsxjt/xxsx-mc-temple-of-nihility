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
 * Physical 3D spell armament summoned by Nihility Saint (and future caster guardians):
 *  - FLOATING_SWORD: Detached shoulder greatsword that curves out to slash and recalls back to Saint.
 *  - WING_FEATHER: Homing obsidian blade launched directly from Saint's 6 Seraphic Wings.
 *  - JUDGMENT_LANCE: Sky-falling golden-cyan obsidian spear channeled by the Astrolabe Halo.
 */
public class VoidSpellEntity extends Entity {

    public enum SpellKind {
        FLOATING_SWORD,
        WING_FEATHER,
        JUDGMENT_LANCE;

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
        Vec3 groundImpactPos,
        float baseDamage
    ) {
        VoidSpellEntity entity = new VoidSpellEntity(ModEntities.VOID_SPELL.get(), level);
        entity.setSpellKind(SpellKind.JUDGMENT_LANCE);
        entity.ownerUuid = owner.getUUID();
        entity.baseDamage = baseDamage;
        entity.erosionMultiplier = 1.65F;
        entity.maxLifeTicks = 30;
        entity.groundTarget = groundImpactPos;
        entity.setPos(groundImpactPos.x, groundImpactPos.y + 13.5, groundImpactPos.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setXRot(90.0F);
        entity.setYRot(0.0F);
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

    private void setSpellKind(SpellKind kind) {
        entityData.set(DATA_KIND, (byte) kind.ordinal());
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    public boolean isDropping() {
        return entityData.get(DATA_DROPPING);
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
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnClientTrail();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        NihilityCreature owner = resolveOwner(serverLevel);
        if (owner == null || !owner.isAlive() || tickCount > maxLifeTicks) {
            discard();
            return;
        }
        switch (getSpellKind()) {
            case FLOATING_SWORD -> tickFloatingSword(serverLevel, owner);
            case WING_FEATHER -> tickWingFeather(serverLevel, owner);
            case JUDGMENT_LANCE -> tickJudgmentLance(serverLevel, owner);
        }
    }

    private void tickFloatingSword(ServerLevel level, NihilityCreature owner) {
        LivingEntity target = resolveTarget(level, owner);
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        if (tickCount < 16) {
            // Outbound scissor-curve toward target
            if (target != null && target.isAlive()) {
                Vec3 desired = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(pos);
                if (desired.lengthSqr() > 0.25) {
                    Vec3 dir = desired.normalize();
                    Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
                    double curveFade = Math.max(0.0, 1.0 - tickCount / 11.0);
                    Vec3 steer = dir.scale(1.22).add(side.scale(curveSign * 0.32 * curveFade));
                    vel = vel.scale(0.45).add(steer.scale(0.55));
                }
            }
        } else {
            if (!isReturning()) {
                entityData.set(DATA_RETURNING, true);
                hitEntityIds.clear(); // Allow slashing again on the return sweep!
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

        // Wide sword-arc hit detection
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
                Vec3 blended = vel.normalize().scale(0.72).add(dir.scale(0.28)).normalize().scale(speed);
                vel = blended;
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

    private void tickJudgmentLance(ServerLevel level, NihilityCreature owner) {
        if (tickCount < 14) {
            // Telegraph phase: glowing rotating rune circle on ground + vertical targeting beam!
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
        // Towering gold-cyan judgment impact pillar!
        for (double y = 0.2; y <= 11.0; y += 0.65) {
            level.sendParticles(ModParticles.CHAOS_GLINT.get(),
                groundTarget.x, groundTarget.y + y, groundTarget.z, 3, 0.28, 0.12, 0.28, 0.03);
            level.sendParticles(ModParticles.NIHILITY_SHARD.get(),
                groundTarget.x, groundTarget.y + y, groundTarget.z, 2, 0.35, 0.12, 0.35, 0.05);
        }
        for (int i = 0; i < 22; i++) {
            double a = i * Math.PI * 2.0 / 22.0;
            level.sendParticles(ModParticles.NIHILITY_RIPPLE.get(),
                groundTarget.x + Math.cos(a) * 2.6, groundTarget.y + 0.25, groundTarget.z + Math.sin(a) * 2.6,
                1, Math.cos(a) * 0.14, 0.04, Math.sin(a) * 0.14, 0.02);
        }
        level.playSound(null, groundTarget.x, groundTarget.y, groundTarget.z,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9F, 1.35F);
    }

    private void spawnClientTrail() {
        if (tickCount % 2 != 0) {
            return;
        }
        SpellKind kind = getSpellKind();
        var particle = kind == SpellKind.JUDGMENT_LANCE
            ? ModParticles.CHAOS_GLINT.get()
            : ModParticles.NIHILITY_MOTE.get();
        level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.01, 0.0);
    }

    private void updateRotationFromMotion(Vec3 v) {
        double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
        if (horiz > 1.0E-5 || Math.abs(v.y) > 1.0E-5) {
            setYRot((float) (Math.atan2(v.x, v.z) * (180.0 / Math.PI)));
            setXRot((float) (Math.atan2(v.y, horiz) * (180.0 / Math.PI)));
        }
    }

    private NihilityCreature resolveOwner(ServerLevel level) {
        if (ownerUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(ownerUuid);
        return entity instanceof NihilityCreature creature ? creature : null;
    }

    private LivingEntity resolveTarget(ServerLevel level, NihilityCreature owner) {
        if (targetEntityId >= 0 && level.getEntity(targetEntityId) instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return owner.getTarget();
    }

    private boolean isValidVictim(NihilityCreature owner, LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate == owner || candidate instanceof NihilityCreature) {
            return false;
        }
        if (candidate instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) {
            return false;
        }
        if (owner instanceof NihilitySaint saint && saint.isRitualChallengeActive()) {
            return candidate instanceof ServerPlayer sp && saint.isChallengeParticipant(sp);
        }
        return owner.canAttack(candidate) || candidate == owner.getTarget();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String id = input.getStringOr("OwnerUuid", "");
        try {
            ownerUuid = id.isEmpty() ? null : UUID.fromString(id);
        } catch (IllegalArgumentException ignored) {
            ownerUuid = null;
        }
        setSpellKind(SpellKind.byId(input.getIntOr("SpellKind", SpellKind.WING_FEATHER.ordinal())));
        baseDamage = input.getFloatOr("BaseDamage", 18.0F);
        erosionMultiplier = input.getFloatOr("ErosionMult", 1.0F);
        maxLifeTicks = input.getIntOr("MaxLife", 40);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("OwnerUuid", ownerUuid.toString());
        }
        output.putInt("SpellKind", getSpellKind().ordinal());
        output.putFloat("BaseDamage", baseDamage);
        output.putFloat("ErosionMult", erosionMultiplier);
        output.putInt("MaxLife", maxLifeTicks);
    }
}
