package com.templenihility.entity;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.action.SaintActionPolicy;
import com.templenihility.challenge.SaintChallengePolicy;
import com.templenihility.challenge.SaintChallengePolicy.EndReason;
import com.templenihility.challenge.SaintChallengeService;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import com.templenihility.init.ModItems;
import com.templenihility.init.ModParticles;
import com.templenihility.world.NihilityVisualEffects;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 虚无圣者 (Nihility Saint) — Sovereign Floating Spell-Blade Boss.
 *
 * <p>Every floating armament on the Saint has a distinct, visible, interlocking combat function:
 * <ul>
 *   <li><b>Twin Hovering Void Greatswords</b>: Detach physically as flying 3D scissor-blades during
 *       Erasure Line / Cross-Slash and snap forward into an X-Guard whirl to parry close melee rushers.</li>
 *   <li><b>6 Seraphic Obsidian Blade-Wings</b>: Fan open to fire staggered homing Wing-Feather salvos,
 *       and physically fold shut into a Phase Refraction Cocoon under burst pressure before snapping
 *       wide open to fire all absorbed damage back as counter-blades (no cliché half-HP shockwave).</li>
 *   <li><b>Golden Astrolabe Halo</b>: Charges 0..6 as spells connect; at full 6/6 resonance, channels
 *       3 sequential sky-falling Astrolabe Judgment Lances onto predicted target positions.</li>
 * </ul>
 */
public class NihilitySaint extends NihilityCreature {
    private static final EntityDataAccessor<Byte> DATA_CHALLENGE_PHASE = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_SWORDS_DEPLOYED = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWORDS_GUARD_TICKS = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WING_SHIELD_TICKS = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WING_SALVO_TICKS = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ASTROLABE_CHARGE = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SWORD_LIBERATION = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.BOOLEAN);

    public enum ChallengePhase {
        NONE,
        DECLARATION,
        ACTIVE;

        public static ChallengePhase byId(int id) {
            ChallengePhase[] values = values();
            return id >= 0 && id < values.length ? values[id] : NONE;
        }
    }

    private UUID challengePlayerId;
    private BlockPos challengeOrigin = BlockPos.ZERO;
    private long challengeStartGameTime;
    private long challengeExpiresGameTime;
    private long nextChallengeGameTime;
    private Vec3 lockedActionOrigin = Vec3.ZERO;
    private Vec3 lockedActionDirection = Vec3.ZERO;
    private Vec3[] missingZoneCenters = new Vec3[0];
    private long missingZonesUntilGameTime;
    private int saintActionSequence;
    private Vec3 terminusCenter = Vec3.ZERO;
    private long terminusStartedGameTime = -1L;

    // Interlocking Floating-Armament Runtime State
    private long swordsDeployedUntilGameTime;
    private long nextSwordGuardGameTime;
    private long nextPhaseBlinkGameTime;
    private long nextWingSalvoGameTime;
    private int pendingWingFeathers;
    private int wingSalvoTotal;
    private float recentBurstDamage;
    private long burstDamageWindowStart;
    private long nextWingShieldGameTime;
    private float wingShieldAbsorbed;
    private int pendingAstrolabeLances;
    private long nextAstrolabeLanceGameTime;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
        UUID.randomUUID(),
        Component.translatable("entity.templenihility.nihility_saint"),
        BossEvent.BossBarColor.WHITE,
        BossEvent.BossBarOverlay.NOTCHED_6
    );

    public NihilitySaint(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.bossEvent.setVisible(false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new SaintSovereignCombatGoal(this));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    private void updateBossBar() {
        boolean combatActive = isAlive() && (isRitualChallengeActive() || getDisposition().isCombat() || getTarget() != null);
        this.bossEvent.setVisible(combatActive);
        if (combatActive) {
            this.bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / Math.max(1.0F, getMaxHealth()))));
            this.bossEvent.setName(getDisplayName());
            this.bossEvent.setColor(isWingShieldActive()
                ? BossEvent.BossBarColor.YELLOW
                : (isSwordLiberation() ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.WHITE));
        }
    }

    @Override
    public int getTier() {
        return 5;
    }

    @Override
    protected double actionCooldownScale() {
        return isSwordLiberation() ? 0.72 : 1.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHALLENGE_PHASE, (byte) ChallengePhase.NONE.ordinal());
        builder.define(DATA_SWORDS_DEPLOYED, false);
        builder.define(DATA_SWORDS_GUARD_TICKS, 0);
        builder.define(DATA_WING_SHIELD_TICKS, 0);
        builder.define(DATA_WING_SALVO_TICKS, 0);
        builder.define(DATA_ASTROLABE_CHARGE, 0);
        builder.define(DATA_SWORD_LIBERATION, false);
    }

    public boolean areSwordsDeployed() {
        return entityData.get(DATA_SWORDS_DEPLOYED);
    }

    public int getSwordsGuardTicks() {
        return Math.max(0, entityData.get(DATA_SWORDS_GUARD_TICKS));
    }

    public int getWingShieldTicks() {
        return Math.max(0, entityData.get(DATA_WING_SHIELD_TICKS));
    }

    public boolean isWingShieldActive() {
        return getWingShieldTicks() > 0;
    }

    public int getWingSalvoTicks() {
        return Math.max(0, entityData.get(DATA_WING_SALVO_TICKS));
    }

    public int getAstrolabeCharge() {
        return Math.max(0, Math.min(6, entityData.get(DATA_ASTROLABE_CHARGE)));
    }

    public boolean isSwordLiberation() {
        return entityData.get(DATA_SWORD_LIBERATION);
    }

    public void onSpellHitTarget(LivingEntity target, int chargeGain) {
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        int mult = isSwordLiberation() ? 2 : 1;
        int next = Math.min(6, getAstrolabeCharge() + chargeGain * mult);
        entityData.set(DATA_ASTROLABE_CHARGE, next);
        if (next >= 6 && pendingAstrolabeLances <= 0) {
            triggerAstrolabeJudgment();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive() && level() instanceof ServerLevel serverLevel) {
            tickRitualChallenge(serverLevel);
            tickFloatingArmaments(serverLevel);
            tickSaintActionTelegraph();
            tickMissingZones(serverLevel);
            tickVoidTerminus(serverLevel);
            updateBossBar();
            com.templenihility.world.TempleCommand.tickShowcaseDirector(serverLevel, this);
        } else if (!level().isClientSide() && !isAlive()) {
            this.bossEvent.setVisible(false);
        }
    }

    private void tickFloatingArmaments(ServerLevel level) {
        long gameTime = level.getGameTime();
        boolean combatActive = isRitualChallengeActive() || (getDisposition().isCombat() && getTarget() != null);

        // Quiet, dignified Sword Liberation escalation below 45% HP (NO cliché shockwave!)
        boolean shouldLiberate = combatActive && getMaxHealth() > 0.0F && getHealth() <= getMaxHealth() * 0.45F;
        if (shouldLiberate && !isSwordLiberation()) {
            entityData.set(DATA_SWORD_LIBERATION, true);
            level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.1F, 1.45F);
        } else if (!combatActive && isSwordLiberation() && getHealth() >= getMaxHealth() * 0.92F) {
            entityData.set(DATA_SWORD_LIBERATION, false);
            entityData.set(DATA_ASTROLABE_CHARGE, 0);
        }

        // Sync Twin Swords physical recall timer
        if (areSwordsDeployed() && gameTime >= swordsDeployedUntilGameTime) {
            entityData.set(DATA_SWORDS_DEPLOYED, false);
        }

        // Decrement close-quarters sword X-guard ticks
        int guard = getSwordsGuardTicks();
        if (guard > 0) {
            entityData.set(DATA_SWORDS_GUARD_TICKS, guard - 1);
        }

        // 6-Wing Phase Refraction Cocoon: absorbs 80% burst dmg, then snaps wide open firing counter-feathers!
        int shield = getWingShieldTicks();
        if (shield > 0) {
            int nextShield = shield - 1;
            entityData.set(DATA_WING_SHIELD_TICKS, nextShield);
            if (tickCount % 3 == 0) {
                level.sendParticles(ModParticles.NIHILITY_RIPPLE.get(),
                    getX(), getY() + 1.3, getZ(), 2, 0.55, 0.55, 0.55, 0.02);
            }
            if (nextShield == 0) {
                releaseRefractedWingCounter(level);
            }
        }

        // Staggered 6-Wing Homing Feather Salvo (1 feather per wing every 2 ticks!)
        int salvo = getWingSalvoTicks();
        if (salvo > 0) {
            entityData.set(DATA_WING_SALVO_TICKS, salvo - 1);
        }
        if (pendingWingFeathers > 0 && tickCount % 2 == 0) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                int index = wingSalvoTotal - pendingWingFeathers;
                fireSingleWingFeather(level, target, index, wingSalvoTotal, 18.0F);
                pendingWingFeathers--;
            } else {
                pendingWingFeathers = 0;
            }
        }

        // Astrolabe Halo 6/6 Overdrive: drop 3 sequential sky-falling Judgment Lances!
        if (pendingAstrolabeLances > 0 && gameTime >= nextAstrolabeLanceGameTime) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                Vec3 predicted = target.position().add(target.getDeltaMovement().multiply(7.0, 0.0, 7.0));
                VoidSpellEntity lance = VoidSpellEntity.createJudgmentLance(
                    level, this, predicted, isSwordLiberation() ? 58.0F : 50.0F);
                level.addFreshEntity(lance);
                pendingAstrolabeLances--;
                nextAstrolabeLanceGameTime = gameTime + 11L;
                if (pendingAstrolabeLances <= 0) {
                    entityData.set(DATA_ASTROLABE_CHARGE, 0);
                }
            } else {
                pendingAstrolabeLances = 0;
                entityData.set(DATA_ASTROLABE_CHARGE, 0);
            }
        }

        // Autonomous mid-range Wing-Feather Salvo when fighting actively
        if (combatActive && !isWingShieldActive() && !hasActiveCreatureAction()
            && pendingWingFeathers <= 0 && gameTime >= nextWingSalvoGameTime) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                double distSq = distanceToSqr(target);
                if (distSq >= 3.2 * 3.2 && distSq <= 24.0 * 24.0) {
                    startWingFeatherSalvo(level, gameTime);
                }
            }
        }
    }

    private void startWingFeatherSalvo(ServerLevel level, long gameTime) {
        wingSalvoTotal = isSwordLiberation() ? 8 : 6;
        pendingWingFeathers = wingSalvoTotal;
        entityData.set(DATA_WING_SALVO_TICKS, wingSalvoTotal * 2 + 8);
        nextWingSalvoGameTime = gameTime + (isSwordLiberation() ? 52L : 68L);
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 0.85F, 1.65F);
    }

    private void fireSingleWingFeather(ServerLevel level, LivingEntity target, int index, int total, float baseDmg) {
        Vec3 forward = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-5) {
            forward = getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        boolean leftWing = (index % 2) == 0;
        int tierSlot = (index / 2) % 3; // upper / mid / lower wing!
        double sideSign = leftWing ? -1.0 : 1.0;
        double lateral = 1.05 + tierSlot * 0.25;
        double height = 2.15 - tierSlot * 0.42;

        Vec3 spawnPos = position()
            .add(right.scale(sideSign * lateral))
            .add(0.0, height, 0.0)
            .subtract(forward.scale(0.35));

        Vec3 toTarget = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(spawnPos).normalize();
        Vec3 fanVelocity = toTarget.scale(1.05)
            .add(right.scale(sideSign * (0.28 + tierSlot * 0.08)))
            .add(0.0, 0.14 - tierSlot * 0.05, 0.0);

        VoidSpellEntity feather = VoidSpellEntity.createWingFeather(
            level, this, target, spawnPos, fanVelocity, baseDmg);
        level.addFreshEntity(feather);
        level.sendParticles(ModParticles.NIHILITY_MOTE.get(),
            spawnPos.x, spawnPos.y, spawnPos.z, 4, 0.12, 0.12, 0.12, 0.03);
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
            SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.45F, 1.55F + tierSlot * 0.12F);
    }

    private void activateWingRefractionShield(ServerLevel level, long gameTime) {
        entityData.set(DATA_WING_SHIELD_TICKS, 38);
        wingShieldAbsorbed = 0.0F;
        recentBurstDamage = 0.0F;
        nextWingShieldGameTime = gameTime + 180L;
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.1F, 0.72F);
        level.sendParticles(ModParticles.NIHILITY_RIPPLE.get(),
            getX(), getY() + 1.3, getZ(), 14, 0.65, 0.65, 0.65, 0.04);
    }

    private void releaseRefractedWingCounter(ServerLevel level) {
        LivingEntity primary = getTarget();
        List<LivingEntity> targets = collectCombatTargets(level, position(), 18.0);
        int count = Math.min(8, Math.max(6, 6 + (int) (wingShieldAbsorbed / 40.0F)));
        float counterDmg = Math.min(34.0F, 18.0F + wingShieldAbsorbed * 0.12F);
        for (int i = 0; i < count; i++) {
            LivingEntity chosen = !targets.isEmpty() ? targets.get(i % targets.size()) : primary;
            double angle = i * (Math.PI * 2.0 / count);
            Vec3 spawnPos = position().add(Math.cos(angle) * 1.1, 1.5, Math.sin(angle) * 1.1);
            Vec3 vel = new Vec3(Math.cos(angle) * 1.15, 0.12, Math.sin(angle) * 1.15);
            VoidSpellEntity feather = VoidSpellEntity.createWingFeather(
                level, this, chosen, spawnPos, vel, counterDmg);
            level.addFreshEntity(feather);
        }
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.1F, 1.35F);
        wingShieldAbsorbed = 0.0F;
    }

    private void triggerAstrolabeJudgment() {
        pendingAstrolabeLances = isSwordLiberation() ? 4 : 3;
        nextAstrolabeLanceGameTime = level().getGameTime() + 4L;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2F, 1.5F);
        }
    }

    private void deployTwinFloatingSwords(ServerLevel level, LivingEntity target, float swordDmg) {
        Vec3 forward = target != null
            ? target.position().subtract(position()).multiply(1.0, 0.0, 1.0)
            : getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-5) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        Vec3 leftOrigin = position().subtract(right.scale(1.15)).add(0.0, 1.55, 0.0);
        Vec3 rightOrigin = position().add(right.scale(1.15)).add(0.0, 1.55, 0.0);

        level.addFreshEntity(VoidSpellEntity.createFloatingSword(level, this, target, leftOrigin, -1.0, swordDmg));
        level.addFreshEntity(VoidSpellEntity.createFloatingSword(level, this, target, rightOrigin, 1.0, swordDmg));
        entityData.set(DATA_SWORDS_DEPLOYED, true);
        swordsDeployedUntilGameTime = level.getGameTime() + 30L;
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.72F);
    }

    private List<LivingEntity> collectCombatTargets(ServerLevel level, Vec3 center, double radius) {
        if (isRitualChallengeActive()) {
            ServerPlayer player = challengePlayerId == null
                ? null
                : level.getServer().getPlayerList().getPlayer(challengePlayerId);
            if (player != null && player.isAlive() && player.level() == level
                && player.distanceToSqr(center) <= radius * radius) {
                return List.of(player);
            }
            return List.of();
        }
        double radiusSq = radius * radius;
        AABB area = new AABB(center, center).inflate(radius, 4.5, radius);
        List<LivingEntity> result = new ArrayList<>();
        LivingEntity primary = getTarget();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e.isAlive() && e != this && !(e instanceof NihilityCreature) && e.distanceToSqr(center) <= radiusSq)) {
            if (candidate instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) {
                continue;
            }
            if (candidate == primary
                || canAttack(candidate)
                || (candidate instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() instanceof NihilityCreature)) {
                result.add(candidate);
            }
        }
        return result;
    }

    public ChallengePhase getChallengePhase() {
        return ChallengePhase.byId(entityData.get(DATA_CHALLENGE_PHASE));
    }

    public boolean isRitualChallengeActive() {
        return getChallengePhase() != ChallengePhase.NONE && challengePlayerId != null;
    }

    public boolean isChallengeParticipant(ServerPlayer player) {
        return player != null && challengePlayerId != null && challengePlayerId.equals(player.getUUID());
    }

    public boolean canBeginRitualChallenge(ServerPlayer player) {
        return player != null
            && level() == player.level()
            && SaintChallengeService.isEligible(player)
            && isAlive()
            && !isRitualChallengeActive()
            && getDisposition() == Disposition.CALM
            && getTarget() == null
            && level().getGameTime() >= nextChallengeGameTime;
    }

    public boolean beginRitualChallenge(ServerPlayer player) {
        if (!canBeginRitualChallenge(player) || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        challengePlayerId = player.getUUID();
        challengeOrigin = hasHome() ? getHomePosition() : blockPosition();
        challengeStartGameTime = serverLevel.getGameTime();
        challengeExpiresGameTime = challengeStartGameTime + SaintChallengePolicy.DURATION_TICKS;
        saintActionSequence = 0;
        entityData.set(DATA_SWORD_LIBERATION, false);
        entityData.set(DATA_ASTROLABE_CHARGE, 0);
        entityData.set(DATA_WING_SHIELD_TICKS, 0);
        entityData.set(DATA_SWORDS_DEPLOYED, false);
        clearMissingZones();
        setChallengePhase(ChallengePhase.DECLARATION);
        setTarget(null);
        setAggressive(false);
        getNavigation().stop();
        setHealth(getMaxHealth());
        NihilityVisualEffects.saintChallengeStart(this, challengeOrigin);
        player.sendSystemMessage(Component.translatable(
            "message.templenihility.saint_challenge_declared",
            SaintChallengePolicy.DECLARATION_TICKS / 20,
            (int) SaintChallengePolicy.ARENA_RADIUS,
            SaintChallengePolicy.DURATION_TICKS / (20 * 60)));
        return true;
    }

    public void cancelRitualChallenge(ServerPlayer player, EndReason reason) {
        if (isChallengeParticipant(player)) {
            finishRitualChallenge(reason, true);
        }
    }

    private void tickRitualChallenge(ServerLevel level) {
        if (!isRitualChallengeActive()) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(challengePlayerId);
        EndReason endReason = currentChallengeEndReason(level, player);
        if (endReason != EndReason.NONE) {
            finishRitualChallenge(endReason, true);
            return;
        }

        getLookControl().setLookAt(player, 50.0F, 50.0F);
        if (getChallengePhase() == ChallengePhase.DECLARATION) {
            getNavigation().stop();
            if (tickCount % 10 == 0) {
                NihilityVisualEffects.saintChallengeDeclarationPulse(this);
            }
            if (level.getGameTime() - challengeStartGameTime >= SaintChallengePolicy.DECLARATION_TICKS) {
                setChallengePhase(ChallengePhase.ACTIVE);
                setTarget(player);
                player.sendSystemMessage(Component.translatable("message.templenihility.saint_challenge_active"));
                NihilityVisualEffects.saintChallengeBoundary(this, challengeOrigin);
            }
            return;
        }

        if (getTarget() != player) {
            setTarget(player);
        }
        if (tickCount % 40 == 0) {
            NihilityVisualEffects.saintChallengeBoundary(this, challengeOrigin);
        }
    }

    @Override
    protected ActionType selectCreatureAction(LivingEntity target, long gameTime) {
        if (isWingShieldActive()) {
            return ActionType.NONE;
        }
        boolean challengeActive = getChallengePhase() == ChallengePhase.ACTIVE && isRitualChallengeActive();
        boolean hostileCombat = getDisposition() == Disposition.HOSTILE && target != null && target.isAlive();
        if (!challengeActive && !hostileCombat) {
            return ActionType.NONE;
        }
        double healthFraction = getMaxHealth() > 0.0
            ? (double) Math.max(0.0F, getHealth()) / getMaxHealth()
            : 1.0;
        return SaintActionPolicy.select(saintActionSequence, distanceToSqr(target), healthFraction);
    }

    @Override
    protected void onCreatureActionStarted(ActionType action, LivingEntity target) {
        if (action == ActionType.SAINT_ERASURE_LINE) {
            updateErasureAim(target);
        } else if (action == ActionType.SAINT_MISSING_ZONE) {
            prepareMissingZones(target);
        } else if (action == ActionType.SAINT_VOID_TERMINUS) {
            terminusCenter = position();
        }
    }

    private void updateErasureAim(LivingEntity target) {
        lockedActionOrigin = position();
        lockedActionDirection = target.position().subtract(lockedActionOrigin).multiply(1.0, 0.0, 1.0);
        if (lockedActionDirection.lengthSqr() <= 1.0E-6) {
            lockedActionDirection = getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        if (lockedActionDirection.lengthSqr() > 1.0E-6) {
            lockedActionDirection = lockedActionDirection.normalize();
        }
    }

    @Override
    protected void applyCustomCreatureAction(ServerLevel level, ActionType action,
                                             LivingEntity target, long elapsed) {
        if (isRitualChallengeActive()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(challengePlayerId);
            EndReason endReason = currentChallengeEndReason(level, player);
            if (endReason != EndReason.NONE) {
                finishRitualChallenge(endReason, true);
                return;
            }
        } else if (target == null || !target.isAlive()) {
            return;
        }
        if (!markCreatureActionApplied()) {
            return;
        }
        if (action == ActionType.SAINT_ERASURE_LINE) {
            applyErasureLine(level, target);
            saintActionSequence++;
        } else if (action == ActionType.SAINT_MISSING_ZONE) {
            activateMissingZones(level);
            saintActionSequence++;
        } else if (action == ActionType.SAINT_VOID_TERMINUS) {
            startVoidTerminus(level, target);
            saintActionSequence++;
        }
    }

    private void tickSaintActionTelegraph() {
        if (getCreatureActionPhase() != Phase.WINDUP) {
            return;
        }
        long elapsed = level().getGameTime() - getCreatureActionStartGameTime();
        if (getCreatureAction() == ActionType.SAINT_ERASURE_LINE) {
            LivingEntity actionTarget = getCreatureActionTarget();
            if (actionTarget != null && actionTarget.isAlive()
                && elapsed <= (ActionType.SAINT_ERASURE_LINE.windupTicks() * 3L) / 5L) {
                updateErasureAim(actionTarget);
            }
            if (tickCount % 3 == 0) {
                NihilityVisualEffects.saintErasureLineTelegraph(this, lockedActionOrigin, lockedActionDirection);
            }
        } else if (getCreatureAction() == ActionType.SAINT_MISSING_ZONE && tickCount % 4 == 0) {
            NihilityVisualEffects.saintMissingZonesTelegraph(this, missingZoneCenters);
        } else if (getCreatureAction() == ActionType.SAINT_VOID_TERMINUS && tickCount % 4 == 0) {
            NihilityVisualEffects.saintVoidTerminusTelegraph(this, terminusCenter);
        }
    }

    private void startVoidTerminus(ServerLevel level, LivingEntity target) {
        terminusCenter = position();
        terminusStartedGameTime = level.getGameTime();
        heal(getMaxHealth() * 0.025F);
        // Channel the Astrolabe Halo into an immediate Judgment Lance alongside Void Terminus!
        if (target != null && target.isAlive()) {
            level.addFreshEntity(VoidSpellEntity.createJudgmentLance(level, this, target.position(), 52.0F));
        }
        NihilityVisualEffects.saintVoidTerminusActivate(this, terminusCenter);
        if (target instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.templenihility.saint_void_terminus_start"));
        }
    }

    private void tickVoidTerminus(ServerLevel level) {
        if (terminusStartedGameTime < 0L) {
            return;
        }
        long elapsed = level.getGameTime() - terminusStartedGameTime;
        if (elapsed >= SaintActionPolicy.TERMINUS_DURATION_TICKS) {
            terminusStartedGameTime = -1L;
            return;
        }
        double progress = (double) elapsed / SaintActionPolicy.TERMINUS_DURATION_TICKS;
        double radius = SaintActionPolicy.terminusSafeRadius(progress);
        if (tickCount % 10 == 0) {
            NihilityVisualEffects.saintVoidTerminusPulse(this, terminusCenter, radius);
        }
        if (!getDisposition().isCombat() || tickCount % 10 != 0) {
            return;
        }
        float dmg = SaintActionPolicy.terminusDamage(progress);
        List<LivingEntity> enemies = collectCombatTargets(level, terminusCenter, 22.0);
        for (LivingEntity victim : enemies) {
            double distanceSquared = victim.distanceToSqr(terminusCenter);
            boolean safe = SaintActionPolicy.isInTerminusSafeZone(distanceSquared, progress);
            if (!safe) {
                dealVoidStrike(level, victim, dmg, 1.15F, true);
            } else if (!(victim instanceof Player) && tickCount % 20 == 0 && distanceSquared <= 3.5 * 3.5) {
                dealVoidStrike(level, victim, dmg * 0.70F, 0.85F, false);
            }
        }
    }

    private void applyErasureLine(ServerLevel level, LivingEntity primaryTarget) {
        // Physically detach & launch the Twin Hovering Greatswords in a scissor-cross alongside the Erasure beam!
        deployTwinFloatingSwords(level, primaryTarget, isSwordLiberation() ? 42.0F : 36.0F);

        boolean anyHit = false;
        List<LivingEntity> candidates = collectCombatTargets(level, lockedActionOrigin,
            SaintActionPolicy.ERASURE_LINE_LENGTH + 2.5);
        for (LivingEntity victim : candidates) {
            boolean hit = Math.abs(victim.getY() - lockedActionOrigin.y) <= 4.0
                && SaintActionPolicy.lineHits(
                    victim.getX(), victim.getZ(),
                    lockedActionOrigin.x, lockedActionOrigin.z,
                    lockedActionDirection.x, lockedActionDirection.z);
            if (hit && dealVoidStrike(level, victim, SaintActionPolicy.ERASURE_LINE_DAMAGE, 1.45F, true)) {
                anyHit = true;
                onSpellHitTarget(victim, 2);
                Vec3 side = new Vec3(-lockedActionDirection.z, 0.0, lockedActionDirection.x);
                double sign = victim.position().subtract(lockedActionOrigin).dot(side) >= 0.0 ? 1.0 : -1.0;
                victim.push(side.x * sign * 0.65, 0.18, side.z * sign * 0.65);
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, false, false));
            }
        }
        NihilityVisualEffects.saintErasureLineResolve(
            this, lockedActionOrigin, lockedActionDirection, anyHit);
    }

    private void prepareMissingZones(LivingEntity target) {
        Vec3 targetPos = target.position();
        Vec3 forward = targetPos.subtract(position()).multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() <= 1.0E-6) {
            forward = getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        if (forward.lengthSqr() > 1.0E-6) {
            forward = forward.normalize();
        }
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        missingZoneCenters = new Vec3[] {
            targetPos,
            targetPos.add(side.scale(3.4)).add(forward.scale(0.8)),
            targetPos.subtract(side.scale(3.4)).add(forward.scale(0.8))
        };
    }

    private void activateMissingZones(ServerLevel level) {
        missingZonesUntilGameTime = level.getGameTime() + SaintActionPolicy.MISSING_ZONE_DURATION_TICKS;
        NihilityVisualEffects.saintMissingZonesActivate(this, missingZoneCenters);
        // Drop an Astrolabe Judgment Lance on the center Missing Zone for dramatic synergy!
        if (missingZoneCenters.length > 0) {
            level.addFreshEntity(VoidSpellEntity.createJudgmentLance(level, this, missingZoneCenters[0], 44.0F));
        }
        for (Vec3 center : missingZoneCenters) {
            for (LivingEntity victim : collectCombatTargets(level, center, SaintActionPolicy.MISSING_ZONE_RADIUS + 0.5)) {
                if (Math.abs(victim.getY() - center.y) <= 3.5) {
                    if (dealVoidStrike(level, victim, SaintActionPolicy.MISSING_ZONE_BURST_DAMAGE, 1.15F, true)) {
                        onSpellHitTarget(victim, 1);
                    }
                    victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, false, false));
                }
            }
        }
    }

    private void tickMissingZones(ServerLevel level) {
        boolean combatActive = isRitualChallengeActive() || (getDisposition().isCombat() && getTarget() != null);
        if (missingZonesUntilGameTime <= level.getGameTime() || !combatActive) {
            if (missingZonesUntilGameTime != 0L) {
                clearMissingZones();
            }
            return;
        }
        if (tickCount % 2 == 0) {
            boolean damageTick = tickCount % 12 == 0;
            for (Vec3 center : missingZoneCenters) {
                for (LivingEntity victim : collectCombatTargets(level, center, SaintActionPolicy.MISSING_ZONE_RADIUS + 0.4)) {
                    if (Math.abs(victim.getY() - center.y) > 3.5
                        || !SaintActionPolicy.zoneContains(victim.getX(), victim.getZ(), center.x, center.z)) {
                        continue;
                    }
                    Vec3 outward = victim.position().subtract(center).multiply(1.0, 0.0, 1.0);
                    if (outward.lengthSqr() <= 1.0E-6) {
                        outward = victim.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                    }
                    if (outward.lengthSqr() > 1.0E-6) {
                        outward = outward.normalize().scale(0.18);
                        Vec3 movement = victim.getDeltaMovement();
                        victim.setDeltaMovement(movement.x + outward.x,
                            Math.max(0.14, movement.y), movement.z + outward.z);
                        victim.hurtMarked = true;
                    }
                    if (damageTick) {
                        dealVoidStrike(level, victim, SaintActionPolicy.MISSING_ZONE_DAMAGE, 0.90F, true);
                    }
                }
            }
        }
        if (tickCount % 10 == 0) {
            NihilityVisualEffects.saintMissingZonesPulse(this, missingZoneCenters);
        }
    }

    /** Twin Hovering Greatswords snap forward into a spinning X-guard slash when rushers get close! */
    private void performTwinSwordGuardSlash(ServerLevel level, LivingEntity primary) {
        this.swing(InteractionHand.MAIN_HAND);
        entityData.set(DATA_SWORDS_GUARD_TICKS, 16);
        float baseAtk = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        float slashDmg = baseAtk * (isSwordLiberation() ? 1.15F : 0.95F);
        List<LivingEntity> nearby = collectCombatTargets(level, position(), 4.8);
        for (LivingEntity enemy : nearby) {
            if (dealVoidStrike(level, enemy, slashDmg, 1.05F, true)) {
                onSpellHitTarget(enemy, 1);
            }
            Vec3 away = enemy.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-5) {
                away = away.normalize();
                enemy.setDeltaMovement(away.x * 0.85, Math.max(0.24, enemy.getDeltaMovement().y + 0.14), away.z * 0.85);
                enemy.hurtMarked = true;
            }
        }
        level.playSound(null, getX(), getY(), getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.15F);
        if (primary != null) {
            NihilityVisualEffects.zeroExecuteBurst(level, primary);
        }
    }

    /** Phase Blink glide to keep commanding spellcaster spacing instead of walking like a villager! */
    private boolean tryPhaseBlinkAround(ServerLevel level, LivingEntity target, double desiredRange) {
        Vec3 from = position();
        Vec3 toTarget = target.position().subtract(from).multiply(1.0, 0.0, 1.0);
        double baseAngle = toTarget.lengthSqr() > 1.0E-5
            ? Math.atan2(toTarget.z, toTarget.x)
            : Math.toRadians(getYRot());
        AABB box = getBoundingBox();
        double[] angleOffsets = {Math.PI, Math.PI * 0.72, -Math.PI * 0.72, Math.PI * 0.45, -Math.PI * 0.45};
        for (double offset : angleOffsets) {
            double ang = baseAngle + offset;
            Vec3 candidate = target.position().add(Math.cos(ang) * desiredRange, 0.0, Math.sin(ang) * desiredRange);
            for (double dy : new double[] {0.0, 1.0, -1.0}) {
                Vec3 test = candidate.add(0.0, dy, 0.0);
                AABB moved = box.move(test.subtract(from)).deflate(0.04);
                if (level.noCollision(this, moved) && level.noBorderCollision(this, moved)) {
                    teleportTo(test.x, test.y, test.z);
                    setDeltaMovement(Vec3.ZERO);
                    hurtMarked = true;
                    getNavigation().stop();
                    NihilityVisualEffects.missingStep(level, from, test);
                    return true;
                }
            }
        }
        return false;
    }

    private void clearMissingZones() {
        missingZonesUntilGameTime = 0L;
        missingZoneCenters = new Vec3[0];
    }

    private EndReason currentChallengeEndReason(ServerLevel level, ServerPlayer player) {
        boolean present = player != null;
        boolean alive = present && player.isAlive();
        boolean eligible = present && SaintChallengeService.isEligible(player);
        boolean sameLevel = present && player.level() == level;
        double distanceSquared = sameLevel
            ? player.distanceToSqr(Vec3.atCenterOf(challengeOrigin))
            : Double.POSITIVE_INFINITY;
        return SaintChallengePolicy.evaluateEnd(
            present, alive, eligible, sameLevel, distanceSquared,
            level.getGameTime(), challengeExpiresGameTime);
    }

    private void finishRitualChallenge(EndReason reason, boolean restoreHealth) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            clearChallengeState();
            return;
        }
        ServerPlayer player = challengePlayerId == null
            ? null
            : serverLevel.getServer().getPlayerList().getPlayer(challengePlayerId);
        if (restoreHealth && isAlive()) {
            setHealth(getMaxHealth());
        }
        setTarget(null);
        setAggressive(false);
        getNavigation().stop();
        cancelCreatureAction();
        clearMissingZones();
        entityData.set(DATA_SWORD_LIBERATION, false);
        entityData.set(DATA_ASTROLABE_CHARGE, 0);
        entityData.set(DATA_WING_SHIELD_TICKS, 0);
        entityData.set(DATA_SWORDS_DEPLOYED, false);
        nextChallengeGameTime = serverLevel.getGameTime() + SaintChallengePolicy.RETRY_COOLDOWN_TICKS;
        NihilityVisualEffects.saintChallengeEnd(this, false);
        clearChallengeState();
        if (player != null) {
            player.sendSystemMessage(Component.translatable(endMessageKey(reason)));
        }
    }

    private void clearChallengeState() {
        challengePlayerId = null;
        challengeStartGameTime = 0L;
        challengeExpiresGameTime = 0L;
        lockedActionOrigin = Vec3.ZERO;
        lockedActionDirection = Vec3.ZERO;
        clearMissingZones();
        terminusCenter = Vec3.ZERO;
        terminusStartedGameTime = -1L;
        setChallengePhase(ChallengePhase.NONE);
    }

    private void setChallengePhase(ChallengePhase phase) {
        entityData.set(DATA_CHALLENGE_PHASE, (byte) phase.ordinal());
    }

    private String endMessageKey(EndReason reason) {
        return switch (reason) {
            case PLAYER_GONE -> "message.templenihility.saint_challenge_player_gone";
            case PLAYER_DEFEATED -> "message.templenihility.saint_challenge_defeated";
            case INVALID_MODE -> "message.templenihility.saint_challenge_invalid_mode";
            case LEFT_ARENA -> "message.templenihility.saint_challenge_left_arena";
            case TIMEOUT -> "message.templenihility.saint_challenge_timeout";
            case VOLUNTARY -> "message.templenihility.saint_challenge_withdrawn";
            case NONE -> "message.templenihility.saint_challenge_withdrawn";
        };
    }

    @Override
    public boolean becomeHostileTo(LivingEntity target) {
        return isRitualChallengeActive() ? false : super.becomeHostileTo(target);
    }

    @Override
    public boolean canTradeWith(Player player) {
        return !isRitualChallengeActive() && super.canTradeWith(player);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRitualChallengeActive()) {
            Entity attacker = source.getEntity();
            if (getChallengePhase() != ChallengePhase.ACTIVE
                || !(attacker instanceof ServerPlayer player)
                || !isChallengeParticipant(player)) {
                return false;
            }
        }
        // If 6-Wing Phase Refraction Cocoon is folded shut, absorb 80% into counter-blade energy!
        boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypass && isWingShieldActive() && amount > 0.0F) {
            wingShieldAbsorbed = Math.min(260.0F, wingShieldAbsorbed + amount * 0.80F);
            level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9F, 1.35F);
            amount *= 0.20F;
        }
        float beforeHp = getHealth();
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && !bypass && isAlive() && !isWingShieldActive()) {
            float lost = Math.max(0.0F, beforeHp - getHealth());
            long gameTime = level.getGameTime();
            if (gameTime - burstDamageWindowStart > 80L) {
                burstDamageWindowStart = gameTime;
                recentBurstDamage = lost;
            } else {
                recentBurstDamage += lost;
            }
            // Organic pressure response: heavy burst triggers 6-Wing Phase Refraction Cocoon!
            if (recentBurstDamage >= 130.0F && gameTime >= nextWingShieldGameTime && !hasActiveCreatureAction()) {
                activateWingRefractionShield(level, gameTime);
            }
        }
        return hurt;
    }

    @Override
    protected boolean shouldRallyAfterHurt(LivingEntity attacker) {
        return !isRitualChallengeActive();
    }

    @Override
    protected Disposition getForcedDisposition() {
        return isRitualChallengeActive() ? Disposition.CHALLENGE : null;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        boolean challengeVictory = getChallengePhase() == ChallengePhase.ACTIVE
            && source.getEntity() instanceof ServerPlayer player
            && isChallengeParticipant(player);
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (challengeVictory) {
            drop(new ItemStack(ModItems.NIHILITY_CORE.get()), false, false);
            drop(new ItemStack(ModItems.NIHILITY_RELIC_FRAGMENT.get(), 2), false, false);
            if (source.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("message.templenihility.saint_challenge_victory"));
            }
            NihilityVisualEffects.saintChallengeEnd(this, true);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("TempleSaintChallengeCooldownUntil", nextChallengeGameTime);
        if (isRitualChallengeActive()) {
            output.putString("TempleSaintChallengePlayer", challengePlayerId.toString());
            output.putInt("TempleSaintChallengePhase", getChallengePhase().ordinal());
            output.putLong("TempleSaintChallengeStart", challengeStartGameTime);
            output.putLong("TempleSaintChallengeExpires", challengeExpiresGameTime);
            output.putInt("TempleSaintChallengeOriginX", challengeOrigin.getX());
            output.putInt("TempleSaintChallengeOriginY", challengeOrigin.getY());
            output.putInt("TempleSaintChallengeOriginZ", challengeOrigin.getZ());
            output.putInt("TempleSaintActionSequence", saintActionSequence);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nextChallengeGameTime = input.getLongOr("TempleSaintChallengeCooldownUntil", 0L);
        String playerText = input.getStringOr("TempleSaintChallengePlayer", "");
        try {
            challengePlayerId = playerText.isEmpty() ? null : UUID.fromString(playerText);
        } catch (IllegalArgumentException ignored) {
            challengePlayerId = null;
        }
        if (challengePlayerId == null) {
            clearChallengeState();
            return;
        }
        challengeStartGameTime = input.getLongOr("TempleSaintChallengeStart", 0L);
        challengeExpiresGameTime = input.getLongOr("TempleSaintChallengeExpires", 0L);
        challengeOrigin = new BlockPos(
            input.getIntOr("TempleSaintChallengeOriginX", blockPosition().getX()),
            input.getIntOr("TempleSaintChallengeOriginY", blockPosition().getY()),
            input.getIntOr("TempleSaintChallengeOriginZ", blockPosition().getZ()));
        saintActionSequence = Math.max(0, input.getIntOr("TempleSaintActionSequence", 0));
        clearMissingZones();
        setChallengePhase(ChallengePhase.byId(input.getIntOr(
            "TempleSaintChallengePhase", ChallengePhase.NONE.ordinal())));
        if (getChallengePhase() == ChallengePhase.NONE || challengeExpiresGameTime <= 0L) {
            clearChallengeState();
        }
    }

    /**
     * Sovereign Spell-Blade positioning & defense controller:
     * Maintains regal mid-range orbit while channeling Wing-Feathers & Astrolabe Lances,
     * snaps Twin Hovering Swords into a front X-Guard slash when melee enemies rush in,
     * and Phase-Blinks effortlessly to avoid ever being corner-locked.
     */
    private static final class SaintSovereignCombatGoal extends Goal {
        private static final double CLOSE_GUARD_RANGE = 4.6;
        private static final double MAX_STANDOFF = 9.5;
        private static final double BLINK_ENGAGE_RANGE = 13.5;
        private final NihilitySaint saint;

        SaintSovereignCombatGoal(NihilitySaint saint) {
            this.saint = saint;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (saint.hasActiveCreatureAction()) {
                return false;
            }
            LivingEntity target = saint.getTarget();
            return target != null && target.isAlive()
                && (saint.isRitualChallengeActive() || saint.getDisposition().isCombat());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = saint.getTarget();
            if (target == null || !(saint.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            saint.getLookControl().setLookAt(target, 60.0F, 60.0F);
            if (saint.isWingShieldActive()) {
                // Hold majestic levitating guard posture while the 6 wings absorb incoming burst
                saint.getNavigation().stop();
                return;
            }
            long gameTime = serverLevel.getGameTime();
            double dist = saint.distanceTo(target);
            if (dist <= CLOSE_GUARD_RANGE) {
                // Snap Twin Hovering Greatswords into spinning X-Guard slash!
                if (!saint.areSwordsDeployed() && gameTime >= saint.nextSwordGuardGameTime) {
                    saint.nextSwordGuardGameTime = gameTime + (saint.isSwordLiberation() ? 18L : 22L);
                    saint.performTwinSwordGuardSlash(serverLevel, target);
                }
                if (gameTime >= saint.nextPhaseBlinkGameTime) {
                    saint.nextPhaseBlinkGameTime = gameTime + (saint.isSwordLiberation() ? 38L : 48L);
                    if (saint.tryPhaseBlinkAround(serverLevel, target, 6.8)) {
                        return;
                    }
                }
                Vec3 away = saint.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
                if (away.lengthSqr() > 1.0E-4) {
                    away = away.normalize();
                    Vec3 side = new Vec3(-away.z, 0.0, away.x);
                    Vec3 glide = saint.position().add(away.scale(2.2)).add(side.scale(1.8));
                    saint.getNavigation().moveTo(glide.x, glide.y, glide.z, 1.22);
                }
            } else if (dist > BLINK_ENGAGE_RANGE && gameTime >= saint.nextPhaseBlinkGameTime) {
                saint.nextPhaseBlinkGameTime = gameTime + 45L;
                saint.tryPhaseBlinkAround(serverLevel, target, 7.2);
            } else if (dist > MAX_STANDOFF) {
                saint.getNavigation().moveTo(target, 1.24);
            } else {
                // Smooth sovereign orbit glide at optimal spell-blade range (5..9 blocks)
                Vec3 toTarget = target.position().subtract(saint.position()).multiply(1.0, 0.0, 1.0);
                if (toTarget.lengthSqr() > 1.0E-4) {
                    toTarget = toTarget.normalize();
                    double dir = ((saint.tickCount / 45) & 1) == 0 ? 1.0 : -1.0;
                    Vec3 orbit = saint.position().add(-toTarget.z * dir * 2.6, 0.0, toTarget.x * dir * 2.6);
                    saint.getNavigation().moveTo(orbit.x, orbit.y, orbit.z, 1.06);
                }
            }
        }
    }
}
