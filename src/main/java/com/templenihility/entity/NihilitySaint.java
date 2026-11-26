package com.templenihility.entity;

import com.templenihility.challenge.SaintChallengePolicy;
import com.templenihility.challenge.SaintChallengePolicy.EndReason;
import com.templenihility.challenge.SaintChallengeService;
import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.action.SaintActionPolicy;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import com.templenihility.init.ModItems;
import com.templenihility.world.NihilityVisualEffects;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class NihilitySaint extends NihilityCreature {
    private static final EntityDataAccessor<Byte> DATA_CHALLENGE_PHASE = SynchedEntityData.defineId(
        NihilitySaint.class, EntityDataSerializers.BYTE);

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
        this.goalSelector.addGoal(1, new SaintTacticalSpacingGoal(this));
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
        }
    }

    @Override
    public int getTier() {
        return 5;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHALLENGE_PHASE, (byte) ChallengePhase.NONE.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive() && level() instanceof ServerLevel serverLevel) {
            tickRitualChallenge(serverLevel);
            tickSaintActionTelegraph();
            tickMissingZones(serverLevel);
            tickVoidTerminus(serverLevel);
            updateBossBar();
            com.templenihility.world.TempleCommand.tickShowcaseDirector(serverLevel, this);
        } else if (!level().isClientSide() && !isAlive()) {
            this.bossEvent.setVisible(false);
        }
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
            lockedActionOrigin = position();
            lockedActionDirection = target.position().subtract(lockedActionOrigin).multiply(1.0, 0.0, 1.0);
            if (lockedActionDirection.lengthSqr() <= 1.0E-6) {
                lockedActionDirection = getLookAngle().multiply(1.0, 0.0, 1.0);
            }
            if (lockedActionDirection.lengthSqr() > 1.0E-6) {
                lockedActionDirection = lockedActionDirection.normalize();
            }
        } else if (action == ActionType.SAINT_MISSING_ZONE) {
            prepareMissingZones(target);
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
            missingZonesUntilGameTime = level.getGameTime() + SaintActionPolicy.MISSING_ZONE_DURATION_TICKS;
            NihilityVisualEffects.saintMissingZonesActivate(this, missingZoneCenters);
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
        if (getCreatureAction() == ActionType.SAINT_ERASURE_LINE && tickCount % 4 == 0) {
            NihilityVisualEffects.saintErasureLineTelegraph(this, lockedActionOrigin, lockedActionDirection);
        } else if (getCreatureAction() == ActionType.SAINT_MISSING_ZONE && tickCount % 4 == 0) {
            NihilityVisualEffects.saintMissingZonesTelegraph(this, missingZoneCenters);
        } else if (getCreatureAction() == ActionType.SAINT_VOID_TERMINUS && tickCount % 4 == 0) {
            NihilityVisualEffects.saintVoidTerminusTelegraph(this, terminusCenter);
        }
    }

    private void startVoidTerminus(ServerLevel level, LivingEntity target) {
        terminusCenter = position();
        terminusStartedGameTime = level.getGameTime();
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
        LivingEntity victim = isRitualChallengeActive() && challengePlayerId != null
            ? level.getServer().getPlayerList().getPlayer(challengePlayerId)
            : getTarget();
        double radius = SaintActionPolicy.terminusSafeRadius(progress);
        if (tickCount % 10 == 0) {
            NihilityVisualEffects.saintVoidTerminusPulse(this, terminusCenter, radius);
        }
        if (victim != null && victim.isAlive() && victim.level() == level && getDisposition().isCombat()) {
            double distanceSquared = victim.distanceToSqr(terminusCenter);
            boolean safe = SaintActionPolicy.isInTerminusSafeZone(distanceSquared, progress);
            if (!safe && tickCount % 20 == 0) {
                victim.hurtServer(level, damageSources().mobAttack(this),
                    SaintActionPolicy.terminusDamage(progress));
            }
        }
    }

    private void applyErasureLine(ServerLevel level, LivingEntity target) {
        boolean validTarget = target != null && target.isAlive()
            && (!isRitualChallengeActive() || (target instanceof ServerPlayer p && isChallengeParticipant(p)));
        boolean hit = validTarget
            && Math.abs(target.getY() - lockedActionOrigin.y) <= 3.5
            && SaintActionPolicy.lineHits(
                target.getX(), target.getZ(),
                lockedActionOrigin.x, lockedActionOrigin.z,
                lockedActionDirection.x, lockedActionDirection.z);
        if (hit && target.hurtServer(level, damageSources().mobAttack(this),
            SaintActionPolicy.ERASURE_LINE_DAMAGE)) {
            Vec3 side = new Vec3(-lockedActionDirection.z, 0.0, lockedActionDirection.x);
            double sign = target.position().subtract(lockedActionOrigin).dot(side) >= 0.0 ? 1.0 : -1.0;
            target.push(side.x * sign * 0.48, 0.12, side.z * sign * 0.48);
        }
        NihilityVisualEffects.saintErasureLineResolve(
            this, lockedActionOrigin, lockedActionDirection, hit);
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
            targetPos.add(side.scale(3.6)).add(forward.scale(1.0)),
            targetPos.subtract(side.scale(3.6)).add(forward.scale(1.0))
        };
    }

    private void tickMissingZones(ServerLevel level) {
        boolean combatActive = isRitualChallengeActive() || (getDisposition().isCombat() && getTarget() != null);
        if (missingZonesUntilGameTime <= level.getGameTime() || !combatActive) {
            if (missingZonesUntilGameTime != 0L) {
                clearMissingZones();
            }
            return;
        }
        LivingEntity victim = isRitualChallengeActive() && challengePlayerId != null
            ? level.getServer().getPlayerList().getPlayer(challengePlayerId)
            : getTarget();
        if (victim == null || !victim.isAlive() || victim.level() != level) {
            clearMissingZones();
            return;
        }

        Vec3 containingCenter = null;
        for (Vec3 center : missingZoneCenters) {
            if (Math.abs(victim.getY() - center.y) <= 3.0
                && SaintActionPolicy.zoneContains(victim.getX(), victim.getZ(), center.x, center.z)) {
                containingCenter = center;
                break;
            }
        }
        if (containingCenter != null && tickCount % 2 == 0) {
            Vec3 outward = victim.position().subtract(containingCenter).multiply(1.0, 0.0, 1.0);
            if (outward.lengthSqr() <= 1.0E-6) {
                outward = victim.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            }
            if (outward.lengthSqr() > 1.0E-6) {
                outward = outward.normalize().scale(0.14);
                Vec3 movement = victim.getDeltaMovement();
                victim.setDeltaMovement(movement.x + outward.x,
                    Math.max(0.12, movement.y), movement.z + outward.z);
                victim.hurtMarked = true;
            }
            if (tickCount % 20 == 0) {
                victim.hurtServer(level, damageSources().mobAttack(this), SaintActionPolicy.MISSING_ZONE_DAMAGE);
            }
        }
        if (tickCount % 10 == 0) {
            NihilityVisualEffects.saintMissingZonesPulse(this, missingZoneCenters);
        }
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
        return super.hurtServer(level, source, amount);
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

    private static final class SaintTacticalSpacingGoal extends Goal {
        private static final double MIN_STANDOFF = 4.4;
        private static final double MAX_STANDOFF = 6.8;
        private final NihilitySaint saint;

        SaintTacticalSpacingGoal(NihilitySaint saint) {
            this.saint = saint;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
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
            if (target == null) return;
            saint.getLookControl().setLookAt(target, 45.0F, 45.0F);
            if (saint.getCreatureActionPhase() != Phase.IDLE) {
                saint.getNavigation().stop();
                return;
            }
            double dist = saint.distanceTo(target);
            if (dist < MIN_STANDOFF) {
                Vec3 away = saint.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
                if (away.lengthSqr() > 1.0E-4) {
                    away = away.normalize();
                    Vec3 retreat = saint.position().add(away.scale(2.6));
                    saint.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.05);
                }
            } else if (dist > MAX_STANDOFF) {
                saint.getNavigation().moveTo(target, 1.0);
            } else {
                saint.getNavigation().stop();
            }
        }
    }
}