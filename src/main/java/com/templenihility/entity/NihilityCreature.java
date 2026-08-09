package com.templenihility.entity;

import com.templenihility.action.CreatureActionLockGoal;
import com.templenihility.action.CreatureActionPolicy;
import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.config.ModConfig;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import com.templenihility.menu.NihilityTradeMenu;
import com.templenihility.world.NihilityVisualEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class NihilityCreature extends PathfinderMob {
    private static final int ALERT_DURATION_TICKS = 30 * 20;
    private static final EntityDataAccessor<Byte> DATA_DISPOSITION = SynchedEntityData.defineId(
        NihilityCreature.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
        NihilityCreature.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_ACTION_PHASE = SynchedEntityData.defineId(
        NihilityCreature.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ACTION_START = SynchedEntityData.defineId(
        NihilityCreature.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTION_TARGET = SynchedEntityData.defineId(
        NihilityCreature.class, EntityDataSerializers.INT);

    private long nextTradeGameTime;
    private long alertUntilGameTime;
    private long retaliationAtGameTime;
    private int alertTargetId = -1;
    private int tradingPlayerId = -1;
    private long nextCreatureActionGameTime;
    private boolean creatureActionApplied;

    protected NihilityCreature(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes(int tier) {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, switch (tier) {
                case 1 -> 24.0;
                case 2 -> 36.0;
                case 3 -> 54.0;
                case 4 -> 82.0;
                default -> 150.0;
            })
            .add(Attributes.ATTACK_DAMAGE, switch (tier) {
                case 1 -> 3.0;
                case 2 -> 5.0;
                case 3 -> 7.5;
                case 4 -> 11.0;
                default -> 18.0;
            })
            .add(Attributes.ARMOR, switch (tier) {
                case 1 -> 0.0;
                case 2 -> 2.0;
                case 3 -> 5.0;
                case 4 -> 8.0;
                default -> 14.0;
            })
            .add(Attributes.ARMOR_TOUGHNESS, tier >= 4 ? tier * 1.5 : 0.0)
            .add(Attributes.ATTACK_KNOCKBACK, tier >= 3 ? tier * 0.12 : 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, tier >= 4 ? 0.15 + tier * 0.05 : 0.0)
            .add(Attributes.MOVEMENT_SPEED, switch (tier) {
                case 1 -> 0.25;
                case 2 -> 0.27;
                case 3 -> 0.29;
                case 4 -> 0.27;
                default -> 0.24;
            })
            .add(Attributes.FOLLOW_RANGE, 24.0 + tier * 8.0)
            .add(Attributes.SCALE, 0.9 + tier * 0.09)
            .add(Attributes.STEP_HEIGHT, tier >= 4 ? 1.0 : 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CreatureActionLockGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        if (getTier() >= 2) {
            this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.9));
        }
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new TradeGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DISPOSITION, (byte) Disposition.CALM.ordinal());
        builder.define(DATA_ACTION, (byte) ActionType.NONE.ordinal());
        builder.define(DATA_ACTION_PHASE, (byte) Phase.IDLE.ordinal());
        builder.define(DATA_ACTION_START, 0L);
        builder.define(DATA_ACTION_TARGET, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive()) {
            updateDispositionState();
            tickCreatureAction();
            if (tickCount % 6 == 0) {
                NihilityVisualEffects.creatureFlow(this);
            }
        }
    }

    public boolean becomeHostileTo(LivingEntity target) {
        if (target == null || !target.isAlive() || target instanceof NihilityCreature || !canAttack(target)) {
            return false;
        }

        long gameTime = level().getGameTime();
        if (alertTargetId == target.getId()) {
            alertUntilGameTime = gameTime + ALERT_DURATION_TICKS;
            if (getTarget() != null) {
                setTarget(target);
                setDisposition(Disposition.HOSTILE);
            }
            return false;
        }

        endActiveTrade();
        alertTargetId = target.getId();
        alertUntilGameTime = gameTime + ALERT_DURATION_TICKS;
        if (getTarget() != null) {
            setTarget(target);
            setDisposition(Disposition.HOSTILE);
            return true;
        }

        retaliationAtGameTime = gameTime + CreatureDispositionPolicy.warningTicks(getTier());
        getNavigation().stop();
        setAggressive(true);
        setDisposition(Disposition.ALERT);
        return true;
    }

    private void updateDispositionState() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Disposition forcedDisposition = getForcedDisposition();
        if (forcedDisposition != null) {
            setAggressive(forcedDisposition.isCombat() && getTarget() != null);
            setDisposition(forcedDisposition);
            return;
        }

        long gameTime = level().getGameTime();
        LivingEntity target = getTarget();
        if (target != null) {
            if (!target.isAlive() || gameTime >= alertUntilGameTime) {
                clearAggression();
            } else {
                setAggressive(true);
                setDisposition(Disposition.HOSTILE);
            }
            return;
        }

        if (alertTargetId >= 0) {
            Entity pending = serverLevel.getEntity(alertTargetId);
            if (!(pending instanceof LivingEntity livingTarget)
                || !livingTarget.isAlive()
                || !canAttack(livingTarget)
                || gameTime >= alertUntilGameTime) {
                clearAggression();
                return;
            }

            if (gameTime >= retaliationAtGameTime) {
                setTarget(livingTarget);
                if (getTarget() == null) {
                    clearAggression();
                } else {
                    setDisposition(Disposition.HOSTILE);
                }
            } else {
                getLookControl().setLookAt(livingTarget, 45.0F, 45.0F);
                getNavigation().stop();
                setDisposition(Disposition.ALERT);
            }
            return;
        }

        boolean trading = isActivelyTrading();
        setAggressive(false);
        setDisposition(CreatureDispositionPolicy.resolve(false, false, trading));
    }

    private void clearAggression() {
        setTarget(null);
        alertTargetId = -1;
        alertUntilGameTime = 0L;
        retaliationAtGameTime = 0L;
        setAggressive(false);
        setDisposition(isActivelyTrading() ? Disposition.TRADING : Disposition.CALM);
    }

    private boolean isActivelyTrading() {
        if (tradingPlayerId < 0 || !(level().getEntity(tradingPlayerId) instanceof ServerPlayer player)) {
            tradingPlayerId = -1;
            return false;
        }
        if (player.containerMenu instanceof NihilityTradeMenu menu && menu.isTrader(this)) {
            return true;
        }
        tradingPlayerId = -1;
        return false;
    }

    private void endActiveTrade() {
        if (tradingPlayerId >= 0 && level().getEntity(tradingPlayerId) instanceof ServerPlayer player
            && player.containerMenu instanceof NihilityTradeMenu menu && menu.isTrader(this)) {
            player.closeContainer();
        }
        tradingPlayerId = -1;
    }

    private void rallyNearby(LivingEntity attacker) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double radius = CreatureDispositionPolicy.rallyRadius(getTier());
        for (NihilityCreature ally : serverLevel.getEntitiesOfClass(
            NihilityCreature.class, getBoundingBox().inflate(radius, 8.0, radius),
            creature -> creature != this && creature.isAlive())) {
            ally.becomeHostileTo(attacker);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            if (hasActiveCreatureAction() && getCreatureActionPhase() == Phase.WINDUP && amount >= 4.0F) {
                clearCreatureAction();
            }
            if (isAlive()) {
                becomeHostileTo(attacker);
            }
            if (shouldRallyAfterHurt(attacker)) {
                rallyNearby(attacker);
            }
        }
        return hurt;
    }

    public abstract int getTier();

    protected ActionType selectCreatureAction(LivingEntity target, long gameTime) {
        return ActionType.NONE;
    }

    protected Disposition getForcedDisposition() {
        return null;
    }

    protected boolean shouldRallyAfterHurt(LivingEntity attacker) {
        return true;
    }

    protected void applyCustomCreatureAction(ServerLevel level, ActionType action,
                                             LivingEntity target, long elapsed) {
    }

    protected void onCreatureActionStarted(ActionType action, LivingEntity target) {
    }

    protected final boolean markCreatureActionApplied() {
        if (creatureActionApplied) {
            return false;
        }
        creatureActionApplied = true;
        return true;
    }

    public ActionType getCreatureAction() {
        return ActionType.byId(entityData.get(DATA_ACTION));
    }

    public Phase getCreatureActionPhase() {
        return Phase.byId(entityData.get(DATA_ACTION_PHASE));
    }

    public long getCreatureActionStartGameTime() {
        return entityData.get(DATA_ACTION_START);
    }

    public boolean hasActiveCreatureAction() {
        return getCreatureAction() != ActionType.NONE;
    }

    public LivingEntity getCreatureActionTarget() {
        int targetId = entityData.get(DATA_ACTION_TARGET);
        return targetId >= 0 && level().getEntity(targetId) instanceof LivingEntity target ? target : null;
    }

    private void tickCreatureAction() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        long gameTime = serverLevel.getGameTime();
        ActionType action = getCreatureAction();
        if (action == ActionType.NONE) {
            LivingEntity target = getTarget();
            if (target == null || !CreatureActionPolicy.canStart(
                gameTime, nextCreatureActionGameTime, getDisposition().isCombat())) {
                return;
            }
            ActionType selected = selectCreatureAction(target, gameTime);
            if (selected != ActionType.NONE) {
                startCreatureAction(selected, target, gameTime);
            }
            return;
        }

        LivingEntity target = getCreatureActionTarget();
        if (target == null || !target.isAlive() || getTarget() != target) {
            clearCreatureAction();
            return;
        }

        long elapsed = gameTime - getCreatureActionStartGameTime();
        Phase phase = CreatureActionPolicy.phase(action, elapsed);
        setCreatureActionPhase(phase);
        if (CreatureActionPolicy.isFinished(action, elapsed)) {
            clearCreatureAction();
            return;
        }

        getLookControl().setLookAt(target, 50.0F, 50.0F);
        getNavigation().stop();
        if (phase == Phase.ACTIVE) {
            applyCreatureAction(serverLevel, action, target, elapsed);
        }
    }

    private void startCreatureAction(ActionType action, LivingEntity target, long gameTime) {
        creatureActionApplied = false;
        nextCreatureActionGameTime = gameTime + action.cooldownTicks();
        entityData.set(DATA_ACTION, (byte) action.ordinal());
        entityData.set(DATA_ACTION_PHASE, (byte) Phase.WINDUP.ordinal());
        entityData.set(DATA_ACTION_START, gameTime);
        entityData.set(DATA_ACTION_TARGET, target.getId());
        getNavigation().stop();
        onCreatureActionStarted(action, target);
        NihilityVisualEffects.creatureActionWindup(this, action, target);
    }

    private void applyCreatureAction(ServerLevel level, ActionType action, LivingEntity target, long elapsed) {
        switch (action) {
            case FOLLOWER_INTERCEPT -> applyFollowerIntercept(level, target, elapsed);
            case ENVOY_BACKSTEP -> applyEnvoyBackstep(target);
            case ENVOY_RIFT_DIRECTION, GRAND_ENVOY_DOMAIN, GRAND_ENVOY_RECALL, GRAND_ENVOY_SHIELD,
                 SAINT_ERASURE_LINE, SAINT_MISSING_ZONE, SAINT_VOID_TERMINUS ->
                applyCustomCreatureAction(level, action, target, elapsed);
            case NONE -> {
            }
        }
    }

    private void applyFollowerIntercept(ServerLevel level, LivingEntity target, long elapsed) {
        if (elapsed == ActionType.FOLLOWER_INTERCEPT.windupTicks()) {
            net.minecraft.world.phys.Vec3 direction = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (direction.lengthSqr() > 1.0E-6) {
                direction = direction.normalize();
                setDeltaMovement(direction.x * 0.72, Math.max(0.08, getDeltaMovement().y), direction.z * 0.72);
                hurtMarked = true;
            }
            NihilityVisualEffects.creatureIntercept(this, target, false);
        }

        if (!creatureActionApplied && distanceToSqr(target) <= 2.8 * 2.8) {
            creatureActionApplied = true;
            if (doHurtTarget(level, target)) {
                net.minecraft.world.phys.Vec3 push = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (push.lengthSqr() > 1.0E-6) {
                    push = push.normalize().scale(0.35);
                    target.push(push.x, 0.08, push.z);
                }
                NihilityVisualEffects.creatureIntercept(this, target, true);
            }
        }
    }

    private void applyEnvoyBackstep(LivingEntity target) {
        if (creatureActionApplied) {
            return;
        }
        creatureActionApplied = true;
        net.minecraft.world.phys.Vec3 away = position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() <= 1.0E-6) {
            away = getLookAngle().multiply(-1.0, 0.0, -1.0);
        }
        if (away.lengthSqr() > 1.0E-6) {
            away = away.normalize();
            setDeltaMovement(away.x * 0.82, Math.max(0.12, getDeltaMovement().y), away.z * 0.82);
            hurtMarked = true;
        }
        NihilityVisualEffects.creatureBackstep(this, target);
    }

    private void setCreatureActionPhase(Phase phase) {
        if (getCreatureActionPhase() != phase) {
            entityData.set(DATA_ACTION_PHASE, (byte) phase.ordinal());
        }
    }

    private void clearCreatureAction() {
        creatureActionApplied = false;
        entityData.set(DATA_ACTION, (byte) ActionType.NONE.ordinal());
        entityData.set(DATA_ACTION_PHASE, (byte) Phase.IDLE.ordinal());
        entityData.set(DATA_ACTION_START, 0L);
        entityData.set(DATA_ACTION_TARGET, -1);
    }

    protected final void cancelCreatureAction() {
        clearCreatureAction();
    }

    public Disposition getDisposition() {
        return Disposition.byId(entityData.get(DATA_DISPOSITION));
    }

    private void setDisposition(Disposition disposition) {
        if (!level().isClientSide() && getDisposition() != disposition) {
            entityData.set(DATA_DISPOSITION, (byte) disposition.ordinal());
        }
    }

    public boolean canTradeWith(Player player) {
        return player != null
            && !getDisposition().blocksTrading()
            && (tradingPlayerId < 0 || tradingPlayerId == player.getId());
    }

    public void stopTrading(Player player) {
        if (player != null && tradingPlayerId == player.getId()) {
            tradingPlayerId = -1;
            if (getTarget() == null && alertTargetId < 0) {
                setDisposition(Disposition.CALM);
            }
        }
    }

    public boolean checkTradeCooldown(Player player) {
        long remaining = nextTradeGameTime - level().getGameTime();
        if (remaining <= 0) {
            return true;
        }
        long seconds = Math.max(1, (remaining + 19) / 20);
        player.sendSystemMessage(Component.translatable("message.templenihility.trade_cooldown", seconds));
        return false;
    }

    public void markTradeCompleted(Player player) {
        int cooldown = ModConfig.TRADE_COOLDOWN.get();
        if (CuriosCompat.hasTraderSeal(player)) {
            cooldown = Math.max(1, cooldown / 2);
            player.sendSystemMessage(Component.translatable("message.templenihility.trade_seal_applied"));
        }
        nextTradeGameTime = level().getGameTime() + cooldown;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) return InteractionResult.CONSUME;

        if (getDisposition().blocksTrading()) {
            player.sendSystemMessage(Component.translatable("message.templenihility.creature_hostile"));
            return InteractionResult.FAIL;
        }

        if (!canTradeWith(player)) {
            player.sendSystemMessage(Component.translatable("message.templenihility.trade_busy"));
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            int tier = getTier();
            tradingPlayerId = player.getId();
            setDisposition(Disposition.TRADING);
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new NihilityTradeMenu(id, inventory, this),
                Component.translatable("container.templenihility.nihility_trade", getDisplayName())),
                buffer -> {
                    buffer.writeVarInt(tier);
                    buffer.writeVarInt(getId());
                });
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("TempleTradeCooldownUntil", nextTradeGameTime);
        output.putLong("TempleCreatureActionCooldownUntil", nextCreatureActionGameTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nextTradeGameTime = input.getLongOr("TempleTradeCooldownUntil", 0L);
        nextCreatureActionGameTime = input.getLongOr("TempleCreatureActionCooldownUntil", 0L);
        alertTargetId = -1;
        tradingPlayerId = -1;
        setDisposition(Disposition.CALM);
        clearCreatureAction();
    }
}
