package com.templenihility.entity;

import com.templenihility.action.CreatureActionLockGoal;
import com.templenihility.action.CreatureActionPolicy;
import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.action.CreatureCombatPolicy;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.config.ModConfig;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import com.templenihility.init.ModItems;
import com.templenihility.menu.NihilityTradeMenu;
import com.templenihility.world.NihilityVisualEffects;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    private long lastPressureHitGameTime;
    private int rapidPressureHits;
    private long nextVoidRepulseGameTime;

    protected NihilityCreature(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes(int tier) {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, switch (tier) {
                case 1 -> 80.0;
                case 2 -> 160.0;
                case 3 -> 280.0;
                case 4 -> 520.0;
                default -> 960.0;
            })
            .add(Attributes.ATTACK_DAMAGE, switch (tier) {
                case 1 -> 10.0;
                case 2 -> 16.0;
                case 3 -> 24.0;
                case 4 -> 34.0;
                default -> 48.0;
            })
            .add(Attributes.ARMOR, switch (tier) {
                case 1 -> 8.0;
                case 2 -> 12.0;
                case 3 -> 16.0;
                case 4 -> 20.0;
                default -> 24.0;
            })
            .add(Attributes.ARMOR_TOUGHNESS, switch (tier) {
                case 1 -> 2.0;
                case 2 -> 4.0;
                case 3 -> 8.0;
                case 4 -> 12.0;
                default -> 16.0;
            })
            .add(Attributes.ATTACK_KNOCKBACK, tier >= 2 ? tier * 0.22 : 0.1)
            .add(Attributes.KNOCKBACK_RESISTANCE, switch (tier) {
                case 1 -> 0.30;
                case 2 -> 0.45;
                case 3 -> 0.65;
                case 4 -> 0.85;
                default -> 1.00;
            })
            .add(Attributes.MOVEMENT_SPEED, switch (tier) {
                case 1 -> 0.30;
                case 2 -> 0.33;
                case 3 -> 0.36;
                case 4 -> 0.35;
                default -> 0.38;
            })
            .add(Attributes.FOLLOW_RANGE, 32.0 + tier * 8.0)
            .add(Attributes.SCALE, 0.9 + tier * 0.09)
            .add(Attributes.STEP_HEIGHT, tier >= 3 ? 1.25 : 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CreatureActionLockGoal(this));
        if (usesDefaultMeleeGoal()) {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.24, true));
        }
        if (getTier() >= 2) {
            this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.95));
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
            tickVoidResilience();
            if (tickCount % 6 == 0) {
                NihilityVisualEffects.creatureFlow(this);
            }
        }
    }

    private void tickVoidResilience() {
        int tier = getTier();
        if (tier >= 4 && tickCount % 40 == 0) {
            // High-tier guardians cleanse crippling status spam (Wither / Poison / Slowness / Weakness / Levitation)
            removeEffect(MobEffects.WITHER);
            removeEffect(MobEffects.POISON);
            removeEffect(MobEffects.WEAKNESS);
            removeEffect(MobEffects.LEVITATION);
            if (tier >= 5) {
                removeEffect(MobEffects.SLOWNESS);
                removeEffect(MobEffects.MINING_FATIGUE);
            }
            if (getHealth() < getMaxHealth()) {
                float regen = tier >= 5 ? getMaxHealth() * 0.012F : getMaxHealth() * 0.008F;
                heal(regen);
            }
        }
    }

    public boolean becomeHostileTo(LivingEntity target) {
        if (target == null || !target.isAlive() || target instanceof NihilityCreature || !canAttack(target)) {
            return false;
        }

        long gameTime = level().getGameTime();
        boolean nonPlayerThreat = !(target instanceof Player);
        if (alertTargetId == target.getId()) {
            alertUntilGameTime = gameTime + ALERT_DURATION_TICKS;
            if (getTarget() != null || nonPlayerThreat) {
                setTarget(target);
                setAggressive(true);
                setDisposition(Disposition.HOSTILE);
            }
            return false;
        }

        endActiveTrade();
        alertTargetId = target.getId();
        alertUntilGameTime = gameTime + ALERT_DURATION_TICKS;
        if (getTarget() != null || nonPlayerThreat) {
            // Against hostile modded bosses/mobs, retaliate IMMEDIATELY with zero freeze delay!
            setTarget(target);
            setAggressive(true);
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

    protected boolean isEnvironmentalDamageImmune(DamageSource source) {
        if (getTier() < 3) {
            return false;
        }
        return source.is(DamageTypeTags.IS_FALL)
            || source.is(DamageTypeTags.IS_FIRE)
            || source.is(DamageTypeTags.IS_DROWNING)
            || source.is(DamageTypeTags.IS_LIGHTNING)
            || source.is(DamageTypeTags.IS_FREEZING);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (isEnvironmentalDamageImmune(source)) {
                return false;
            }
            if (source.getEntity() instanceof NihilityCreature) {
                return false;
            }
            double healthFraction = getMaxHealth() > 0.0F ? (double) getHealth() / getMaxHealth() : 1.0;
            amount = CreatureCombatPolicy.mitigatedDamage(getTier(), amount, getMaxHealth(), healthFraction);
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            if (hasActiveCreatureAction()
                && getCreatureActionPhase() == Phase.WINDUP
                && CreatureCombatPolicy.canInterruptWindup(getTier(), amount, getMaxHealth())) {
                clearCreatureAction();
            }
            if (isAlive()) {
                becomeHostileTo(attacker);
                tryTriggerVoidRepulse(level, attacker);
            }
            if (shouldRallyAfterHurt(attacker)) {
                rallyNearby(attacker);
            }
        }
        return hurt;
    }

    private void tryTriggerVoidRepulse(ServerLevel level, LivingEntity attacker) {
        if (getTier() < 3 || distanceToSqr(attacker) > 5.0 * 5.0) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime - lastPressureHitGameTime <= 35L) {
            rapidPressureHits++;
        } else {
            rapidPressureHits = 1;
        }
        lastPressureHitGameTime = gameTime;

        if (rapidPressureHits < 2 || gameTime < nextVoidRepulseGameTime) {
            return;
        }
        rapidPressureHits = 0;
        nextVoidRepulseGameTime = gameTime + (getTier() >= 5 ? 55L : 75L);

        double radius = getTier() >= 5 ? 5.5 : 4.5;
        float baseAtk = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        float burstDmg = CreatureCombatPolicy.retaliationBurstDamage(getTier(), baseAtk);
        AABB box = getBoundingBox().inflate(radius, 3.0, radius);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != this && !(e instanceof NihilityCreature) && canAttack(e));
        for (LivingEntity enemy : enemies) {
            if (enemy instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) {
                continue;
            }
            dealVoidStrike(level, enemy, burstDmg, 1.0F, true);
            Vec3 away = enemy.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-5) {
                away = away.normalize();
                double knock = getTier() >= 5 ? 1.15 : 0.85;
                enemy.setDeltaMovement(away.x * knock, Math.max(0.28, enemy.getDeltaMovement().y + 0.18), away.z * knock);
                enemy.hurtMarked = true;
            }
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 1, false, false));
        }
        NihilityVisualEffects.zeroExecuteBurst(level, attacker);
        NihilityVisualEffects.creatureMeleeStrike(this, attacker);
    }

    /**
     * Unified strike helper combining base physical/skill damage with armor-piercing Void Erosion
     * true damage so Temple guardians deal meaningful damage to tanky modded bosses without
     * unfairly one-shotting players.
     */
    public boolean dealVoidStrike(ServerLevel level, LivingEntity target, float baseDamage,
                                  float erosionMultiplier, boolean resetInvuln) {
        if (target == null || !target.isAlive() || target instanceof NihilityCreature) {
            return false;
        }
        if (resetInvuln) {
            target.invulnerableTime = 0;
        }
        boolean hit = target.hurtServer(level, damageSources().mobAttack(this), Math.max(1.0F, baseDamage));
        float erosion = CreatureCombatPolicy.voidErosionBonus(
            getTier(), target.getMaxHealth(), target instanceof Player, erosionMultiplier);
        if (erosion > 0.0F && target.isAlive()) {
            target.invulnerableTime = 0;
            boolean magicHit = target.hurtServer(level, damageSources().indirectMagic(this, this), erosion);
            hit = hit || magicHit;
        }
        if (hit) {
            NihilityVisualEffects.creatureMeleeStrike(this, target);
        }
        return hit;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.swing(InteractionHand.MAIN_HAND);
        if (target instanceof LivingEntity living && !(living instanceof NihilityCreature)) {
            float atk = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            return dealVoidStrike(level, living, atk, 0.85F, false);
        }
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target != null) {
            NihilityVisualEffects.creatureMeleeStrike(this, target);
        }
        return hit;
    }

    public abstract int getTier();

    protected boolean usesDefaultMeleeGoal() {
        return true;
    }

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

    protected double actionCooldownScale() {
        return 1.0;
    }

    protected final void resetCreatureActionCooldown(long gameTime) {
        nextCreatureActionGameTime = gameTime;
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
        long scaledCooldown = Math.max(action.totalTicks() + 6L,
            Math.round(action.cooldownTicks() * actionCooldownScale()));
        nextCreatureActionGameTime = gameTime + scaledCooldown;
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
            case ENVOY_BACKSTEP -> applyEnvoyBackstep(level, target);
            case ENVOY_RIFT_DIRECTION, GRAND_ENVOY_DOMAIN, GRAND_ENVOY_RECALL, GRAND_ENVOY_SHIELD,
                 SAINT_ERASURE_LINE, SAINT_MISSING_ZONE, SAINT_VOID_TERMINUS ->
                applyCustomCreatureAction(level, action, target, elapsed);
            case NONE -> {
            }
        }
    }

    private void applyFollowerIntercept(ServerLevel level, LivingEntity target, long elapsed) {
        if (elapsed == ActionType.FOLLOWER_INTERCEPT.windupTicks()) {
            Vec3 direction = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (direction.lengthSqr() > 1.0E-6) {
                direction = direction.normalize();
                setDeltaMovement(direction.x * 0.96, Math.max(0.14, getDeltaMovement().y), direction.z * 0.96);
                hurtMarked = true;
            }
            NihilityVisualEffects.creatureIntercept(this, target, false);
        }
        if (!creatureActionApplied && distanceToSqr(target) <= 3.4 * 3.4) {
            creatureActionApplied = true;
            float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35F;
            if (dealVoidStrike(level, target, dmg, 1.15F, true)) {
                Vec3 push = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (push.lengthSqr() > 1.0E-6) {
                    push = push.normalize().scale(0.55);
                    target.push(push.x, 0.14, push.z);
                }
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
                NihilityVisualEffects.creatureIntercept(this, target, true);
            }
        }
    }

    private void applyEnvoyBackstep(ServerLevel level, LivingEntity target) {
        if (creatureActionApplied) {
            return;
        }
        creatureActionApplied = true;
        // Leave a Void Afterimage Burst at close quarters before leaping back!
        float burstDmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35F;
        AABB burstArea = getBoundingBox().inflate(3.8, 2.5, 3.8);
        for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, burstArea,
            e -> e.isAlive() && e != this && !(e instanceof NihilityCreature) && canAttack(e))) {
            if (enemy instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) {
                continue;
            }
            dealVoidStrike(level, enemy, burstDmg, 1.1F, true);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 1, false, false));
            Vec3 push = enemy.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-5) {
                push = push.normalize().scale(0.65);
                enemy.push(push.x, 0.16, push.z);
            }
        }
        Vec3 away = position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() <= 1.0E-6) {
            away = getLookAngle().multiply(-1.0, 0.0, -1.0);
        }
        if (away.lengthSqr() > 1.0E-6) {
            away = away.normalize();
            setDeltaMovement(away.x * 1.05, Math.max(0.22, getDeltaMovement().y), away.z * 1.05);
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

    public void triggerShowcaseAction(ActionType action, LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || target == null) return;
        setTarget(target);
        setAggressive(true);
        startCreatureAction(action, target, serverLevel.getGameTime());
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
        ItemStack held = player.getItemInHand(hand);
        boolean holdingCombatGear = held.is(ModItems.NIHILITY_SWORD.get())
            || held.is(ModItems.NIHILITY_GREATSWORD.get())
            || held.is(ModItems.NIHILITY_DAGGER.get())
            || held.is(ModItems.NIHILITY_ABYSSAL_DRUM.get())
            || held.is(ModItems.NIHILITY_BARRIER_CORE.get());
        if (getDisposition().blocksTrading() || holdingCombatGear) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) return InteractionResult.CONSUME;
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
