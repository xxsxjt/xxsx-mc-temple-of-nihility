package com.templenihility.action;

import com.templenihility.TempleNihilityMod;
import com.templenihility.effect.NihilityRelicEffects;
import com.templenihility.effect.RelicBuildPolicy;
import com.templenihility.effect.VoidPhaseEvents;
import com.templenihility.effect.CombatFeedbackPolicy;
import com.templenihility.energy.VoidPower;
import com.templenihility.network.NihilityNetwork;
import com.templenihility.init.ModEffects;
import com.templenihility.init.ModTags;
import com.templenihility.item.AshenAxe;
import com.templenihility.item.AshenSword;
import com.templenihility.item.NihilityAxe;
import com.templenihility.item.RiftAxe;
import com.templenihility.item.RiftBlade;
import com.templenihility.item.NihilityDagger;
import com.templenihility.item.NihilityGreatsword;
import com.templenihility.item.NihilitySword;
import com.templenihility.world.NihilityVisualEffects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class PlayerWeaponActions {
    private static final int MISSING_STEP_COST = 8;
    private static final int MISSING_STEP_COOLDOWN = 30;
    private static final int ZERO_SLASH_COOLDOWN = 40;
    private static final int BOUNDARY_BREAK_COST = 28;
    private static final int BOUNDARY_BREAK_COOLDOWN = 70;
    private static final int MAX_ACTION_TARGETS = 32;
    public static final int MISSING_STEP_DURATION = 12;
    public static final int BOUNDARY_BREAK_DURATION = 39;

    private static final String MARK_UNTIL = TempleNihilityMod.MOD_ID + ".MissingMarkUntil";
    private static final String MARK_OWNER = TempleNihilityMod.MOD_ID + ".MissingMarkOwner";
    private static final String HEAL_LOCK_UNTIL = TempleNihilityMod.MOD_ID + ".HealLockUntil";
    private static final String EXISTENCE_STACKS = TempleNihilityMod.MOD_ID + ".ExistenceReductionStacks";
    private static final String EXISTENCE_UNTIL = TempleNihilityMod.MOD_ID + ".ExistenceReductionUntil";
    private static final String MELEE_ENERGY_NEXT = TempleNihilityMod.MOD_ID + ".MeleeEnergyNextTick";

    @FunctionalInterface
    public interface ClientSkillListener {
        void onSkillStart(Player player, InteractionHand hand, String skillName);
    }

    private static volatile ClientSkillListener clientSkillListener;

    public static void setClientSkillListener(ClientSkillListener listener) {
        clientSkillListener = listener;
    }

    public static void notifyClientSkillStart(Player player, InteractionHand hand, String skillName) {
        ClientSkillListener listener = clientSkillListener;
        if (listener != null) {
            listener.onSkillStart(player, hand, skillName);
        }
    }

    private static final Map<UUID, ActiveAction> ACTIVE = new HashMap<>();

    public static boolean prepareZeroSlash(Player player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel)) {
            return true;
        }
        if (!canPrepare(player, stack, 12)) {
            return false;
        }
        NihilityVisualEffects.weaponWindup(player, NihilityVisualEffects.WeaponAction.ZERO_SLASH);
        return true;
    }

    public static boolean startMissingStep(Player player, InteractionHand hand) {
        return start(player, hand, ActionType.MISSING_STEP, MISSING_STEP_COST, MISSING_STEP_COOLDOWN, 0);
    }

    public static boolean startZeroSlash(Player player, InteractionHand hand, int chargeTicks) {
        int clampedCharge = Math.max(8, Math.min(24, chargeTicks));
        int tier = clampedCharge >= 24 ? 2 : clampedCharge >= 16 ? 1 : 0;
        int cost = 12 + tier * 4;
        return start(player, hand, ActionType.ZERO_SLASH, cost, ZERO_SLASH_COOLDOWN, tier);
    }

    public static boolean startBoundaryBreak(Player player, InteractionHand hand) {
        return start(player, hand, ActionType.BOUNDARY_BREAK,
            BOUNDARY_BREAK_COST, BOUNDARY_BREAK_COOLDOWN, 0);
    }

    public static void applyExistenceReductionStacks(LivingEntity target, long gameTime, int addStacks) {
        var data = target.getPersistentData();
        int current = getExistenceStacks(target, gameTime);
        int next = Math.max(1, Math.min(3, current + Math.max(1, addStacks)));
        data.putInt(EXISTENCE_STACKS, next);
        data.putLong(EXISTENCE_UNTIL, gameTime + 120L);
    }

    public static int getExistenceStacks(LivingEntity target, long gameTime) {
        var data = target.getPersistentData();
        long until = data.getLongOr(EXISTENCE_UNTIL, 0L);
        if (until < gameTime) {
            if (until != 0L) {
                data.remove(EXISTENCE_UNTIL);
                data.remove(EXISTENCE_STACKS);
            }
            return 0;
        }
        return Math.max(0, Math.min(3, data.getIntOr(EXISTENCE_STACKS, 0)));
    }

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ActiveAction action = ACTIVE.get(player.getUUID());
        if (action == null) {
            return;
        }
        if (!action.isStillValid(player)) {
            ACTIVE.remove(player.getUUID());
            stopActionPose(player, action);
            return;
        }

        long elapsed = level.getGameTime() - action.startTick;
        switch (action.type) {
            case MISSING_STEP -> tickMissingStep(player, level, action, elapsed);
            case ZERO_SLASH -> tickZeroSlash(player, level, action, elapsed);
            case BOUNDARY_BREAK -> tickBoundaryBreak(player, level, action, elapsed);
        }

        if (elapsed >= action.type.totalTicks) {
            ACTIVE.remove(player.getUUID());
            stopActionPose(player, action);
        }
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.isCanceled() || target.level().isClientSide() || event.getAmount() <= 0.0F
            || target instanceof Player phasedPlayer && VoidPhaseEvents.hasFullPhase(phasedPlayer)) {
            return;
        }

        long gameTime = target.level().getGameTime();

        if (target instanceof Player player) {
            ActiveAction active = ACTIVE.get(player.getUUID());
            if (active != null && gameTime - active.startTick < active.type.firstActiveTick) {
                float rawIncoming = event.getAmount();
                if (active.type == ActionType.BOUNDARY_BREAK) {
                    // Greatsword Heavy Poise: 65% damage reduction during windup, only >=12 raw damage interrupts
                    event.setAmount(rawIncoming * 0.35F);
                    if (rawIncoming >= 12.0F) {
                        ACTIVE.remove(player.getUUID());
                        stopActionPose(player, active);
                        player.sendOverlayMessage(Component.translatable("message.templenihility.action_interrupted"));
                    }
                } else if (rawIncoming >= 4.0F) {
                    ACTIVE.remove(player.getUUID());
                    stopActionPose(player, active);
                    player.sendOverlayMessage(Component.translatable("message.templenihility.action_interrupted"));
                }
            }
        }

        if (!(event.getSource().getEntity() instanceof Player attacker) || target == attacker) {
            return;
        }
        ItemStack mainHand = attacker.getMainHandItem();
        if (!isNihilityWeapon(mainHand)) {
            return;
        }

        // Core melee-to-energy loop scaled by weapon tier: Tier 1 (+2), Tier 2 (+3), Tier 3 (+4)
        long nextEnergyTick = attacker.getPersistentData().getLongOr(MELEE_ENERGY_NEXT, 0L);
        if (gameTime >= nextEnergyTick) {
            int gain = meleeEnergyGain(mainHand);
            long icd = gain >= 4 ? 4L : (gain == 3 ? 5L : 6L);
            attacker.getPersistentData().putLong(MELEE_ENERGY_NEXT, gameTime + icd);
            VoidPower.add(attacker, gain);
        }

        // Sword line Existence Reduction progression: Ashen (+6% up to 1 stack) -> Rift (+9% up to 2) -> Blade of Zero (+12% up to 3)
        if (mainHand.getItem() instanceof NihilitySword) {
            int existingStacks = getExistenceStacks(target, gameTime);
            if (existingStacks > 0) {
                event.setAmount(event.getAmount() * (1.0F + existingStacks * 0.12F));
            }
            applyExistenceReductionStacks(target, gameTime, 1);
        } else if (mainHand.getItem() instanceof RiftBlade) {
            int existingStacks = Math.min(2, getExistenceStacks(target, gameTime));
            if (existingStacks > 0) {
                event.setAmount(event.getAmount() * (1.0F + existingStacks * 0.09F));
            }
            if (getExistenceStacks(target, gameTime) < 2) {
                applyExistenceReductionStacks(target, gameTime, 1);
            }
        } else if (mainHand.getItem() instanceof AshenSword) {
            int existingStacks = Math.min(1, getExistenceStacks(target, gameTime));
            if (existingStacks > 0) {
                event.setAmount(event.getAmount() * 1.06F);
            }
            if (getExistenceStacks(target, gameTime) < 1) {
                applyExistenceReductionStacks(target, gameTime, 1);
            }
        }

        // Rift Dagger passive: Backstab Crit (120-degree rear cone -> 1.45x damage & +4 extra VoidPower)
        if (mainHand.getItem() instanceof NihilityDagger) {
            Vec3 targetLook = target.getLookAngle();
            if (RelicBuildPolicy.isBackstab(
                    attacker.getX(), attacker.getZ(),
                    target.getX(), target.getZ(),
                    targetLook.x, targetLook.z)) {
                event.setAmount(event.getAmount() * 1.45F);
                VoidPower.add(attacker, 4);
                if (target.level() instanceof ServerLevel serverLevel) {
                    NihilityVisualEffects.weaponHitBurst(serverLevel, target, NihilityVisualEffects.WeaponAction.MISSING_STEP);
                }
                NihilityNetwork.sendCombatFeedback(attacker, CombatFeedbackPolicy.Kind.DAGGER_BACKSTAB, 4, "");
            }
        }

        // Missing Mark detonation: percent-max-HP armor-piercing burst + 4s heal lock + +10 VP + phase cloak + CD halve
        var data = target.getPersistentData();
        long markUntil = data.getLongOr(MARK_UNTIL, 0L);
        String markOwner = data.getStringOr(MARK_OWNER, "");
        if (markUntil < gameTime || !markOwner.equals(attacker.getUUID().toString())) {
            if (markUntil < gameTime) {
                clearMissingMark(target);
            }
            return;
        }

        clearMissingMark(target);
        data.putLong(HEAL_LOCK_UNTIL, gameTime + 80L);
        float executeBonus = RelicBuildPolicy.missingMarkExecuteBonus(target.getMaxHealth());
        event.setAmount(event.getAmount() + executeBonus);
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
        VoidPower.add(attacker, 10);
        attacker.addEffect(new MobEffectInstance(ModEffects.INTO_NIHILITY, 30, 0, true, false, true));
        if (mainHand.getItem() instanceof NihilityDagger) {
            attacker.getCooldowns().addCooldown(mainHand, MISSING_STEP_COOLDOWN / 2);
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            NihilityVisualEffects.weaponHitBurst(serverLevel, target, NihilityVisualEffects.WeaponAction.MISSING_STEP);
        }
        NihilityNetwork.sendCombatFeedback(attacker, CombatFeedbackPolicy.Kind.DAGGER_MARK, 10, "");
    }

    public static void livingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        var data = entity.getPersistentData();
        long gameTime = entity.level().getGameTime();
        long healLockUntil = data.getLongOr(HEAL_LOCK_UNTIL, 0L);
        if (healLockUntil >= gameTime) {
            event.setAmount(0.0F);
            return;
        }
        if (healLockUntil != 0L) {
            data.remove(HEAL_LOCK_UNTIL);
        }

        int stacks = getExistenceStacks(entity, gameTime);
        if (stacks > 0) {
            event.setAmount(event.getAmount() * (1.0F - stacks * 0.2F));
        }
    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    public static void playerCloned(PlayerEvent.Clone event) {
        ACTIVE.remove(event.getOriginal().getUUID());
        ACTIVE.remove(event.getEntity().getUUID());
    }

    private static boolean start(Player player, InteractionHand hand, ActionType type,
                                 int energyCost, int cooldownTicks, int chargeTier) {
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!canPrepare(player, stack, energyCost)) {
            return false;
        }
        int effectiveCost = VoidPower.effectiveCost(player, energyCost);
        if (!VoidPower.tryConsume(player, energyCost)) {
            sendNoPower(player, effectiveCost);
            return false;
        }

        Vec3 direction = (type == ActionType.BOUNDARY_BREAK || type == ActionType.MISSING_STEP)
            ? horizontalLook(player)
            : player.getLookAngle().normalize();
        Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.56, 0.0);
        ActiveAction active = new ActiveAction(
            type, hand, stack.getItem(), level.dimension(), level.getGameTime(), origin, direction, energyCost, chargeTier);
        ACTIVE.put(player.getUUID(), active);
        if (type != ActionType.ZERO_SLASH) {
            player.startUsingItem(hand);
        }
        player.getCooldowns().addCooldown(stack, cooldownTicks);
        if (type != ActionType.ZERO_SLASH) {
            NihilityVisualEffects.weaponWindup(player, type.visual);
        }
        return true;
    }

    private static boolean canPrepare(Player player, ItemStack stack, int energyCost) {
        if (ACTIVE.containsKey(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.templenihility.action_busy"));
            return false;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            player.sendOverlayMessage(Component.translatable("message.templenihility.item_cooldown"));
            return false;
        }
        int effectiveCost = VoidPower.effectiveCost(player, energyCost);
        if (VoidPower.get(player) < effectiveCost) {
            sendNoPower(player, effectiveCost);
            return false;
        }
        return true;
    }

    private static void sendNoPower(Player player, int required) {
        player.sendOverlayMessage(Component.translatable(
            "message.templenihility.not_enough_void_power", required, VoidPower.get(player), VoidPower.getMax(player)));
    }

    private static void tickMissingStep(Player player, ServerLevel level, ActiveAction action, long elapsed) {
        if (!action.activated && elapsed >= action.type.firstActiveTick) {
            action.activated = true;
            executeMissingStep(player, level, action);
        }
    }

    private static void tickZeroSlash(Player player, ServerLevel level, ActiveAction action, long elapsed) {
        int step = (int) Math.min(9L, Math.max(0L, elapsed));
        if (step >= action.zeroSlashStep && action.zeroSlashStep < 10) {
            while (action.zeroSlashStep <= step && action.zeroSlashStep < 10) {
                executeZeroSlashStep(player, level, action, action.zeroSlashStep);
                action.zeroSlashStep++;
            }
        }
    }

    private static void tickBoundaryBreak(Player player, ServerLevel level, ActiveAction action, long elapsed) {
        player.setSprinting(false);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * 0.42, movement.y, movement.z * 0.42);
        player.hurtMarked = true;

        int[] waveTicks = {15, 21, 27};
        while (action.nextWave < waveTicks.length && elapsed >= waveTicks[action.nextWave]) {
            executeBoundaryWave(player, level, action, action.nextWave);
            action.nextWave++;
        }
    }

    private static void executeMissingStep(Player player, ServerLevel level, ActiveAction action) {
        Vec3 origin = player.position();
        Vec3 destination = findSafeDashDestination(player, level, action.direction, 5.5);
        if (destination.distanceToSqr(origin) < 0.25) {
            VoidPower.add(player, action.energyCost);
            player.sendOverlayMessage(Component.translatable("message.templenihility.missing_step_blocked"));
            return;
        }

        LivingEntity target = nearestLivingAlongLine(level, player,
            origin.add(0.0, player.getBoundingBox().getYsize() * 0.5, 0.0),
            destination.add(0.0, player.getBoundingBox().getYsize() * 0.5, 0.0), 0.9);
        if (target != null) {
            double distToTarget = origin.distanceTo(target.position());
            double desiredDist = Math.min(5.5, Math.max(1.8, distToTarget + 1.9));
            Vec3 tuned = findSafeDashDestination(player, level, action.direction, desiredDist);
            if (tuned.distanceToSqr(origin) >= 0.25) {
                destination = tuned;
            }
        }
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.swing(action.hand, true);

        if (target != null) {
            float damage = (float) Math.max(6.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.95);
            if (target.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
                applyMissingMark(target, player, level.getGameTime());
                NihilityVisualEffects.weaponHitBurst(level, target, action.type.visual);
                Vec3 push = action.direction.scale(0.28).add(0.0, 0.12, 0.0);
                target.setDeltaMovement(target.getDeltaMovement().add(push));
                target.hurtMarked = true;
                player.sendOverlayMessage(Component.translatable("message.templenihility.missing_mark_applied"));
            }
        }
        damageActionItem(player, action);
        NihilityVisualEffects.missingStep(level, origin, destination);
    }

    /**
     * Traveling flying crescent sword-wave for Blade of Zero:
     * sweeps from -0.65 blocks (point-blank touching player) out to 10/13/16 blocks over 10 ticks,
     * slicing projectiles & enemies along its 3D crescent swath and delivering true armor-piercing
     * Execute + clean lethal finisher on low-HP non-boss targets.
     */
    private static void executeZeroSlashStep(Player player, ServerLevel level, ActiveAction action, int step) {
        double reach = 10.0 + action.chargeTier * 3.0;
        double totalSteps = 10.0;
        double prevDist = step == 0 ? -0.65 : (reach * (step / totalSteps)) - 0.75;
        double currDist = (reach * ((step + 1) / totalSteps)) + 0.65;
        double halfWidth = 1.65 + action.chargeTier * 0.35;
        double halfHeight = 1.45;

        if (step == 0) {
            player.swing(action.hand, true);
            damageActionItem(player, action);
        }

        Vec3 fwd = action.direction;
        Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
        if (right.lengthSqr() < 1.0E-5) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(fwd).normalize();

        Vec3 segStart = action.origin.add(fwd.scale(Math.max(-0.5, prevDist)));
        Vec3 segEnd = action.origin.add(fwd.scale(currDist));
        AABB sweepBox = new AABB(segStart, segEnd).inflate(halfWidth + 0.85, halfHeight + 0.65, halfWidth + 0.85);

        List<Entity> candidates = level.getEntities(player, sweepBox,
            candidate -> candidate.isAlive() && !candidate.isSpectator());
        candidates.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(action.origin)));

        long now = level.getGameTime();
        for (Entity entity : candidates) {
            if (action.zeroSlashProcessed >= MAX_ACTION_TARGETS) {
                break;
            }
            if (action.hitEntityIds.contains(entity.getId())) {
                continue;
            }

            Vec3 center = entity.getBoundingBox().getCenter();
            Vec3 rel = center.subtract(action.origin);
            double forward = rel.dot(fwd);
            double entityHalfDepth = Math.max(entity.getBbWidth(), 0.6) * 0.55;
            if (forward + entityHalfDepth < prevDist || forward - entityHalfDepth > currDist) {
                continue;
            }
            double lateral = Math.abs(rel.dot(right));
            double vertical = Math.abs(rel.dot(up));
            if (lateral > halfWidth + entity.getBbWidth() * 0.55
                || vertical > halfHeight + entity.getBbHeight() * 0.55) {
                continue;
            }

            if (entity instanceof Projectile projectile) {
                if (projectile.isPickable() && projectile.getOwner() != player) {
                    action.hitEntityIds.add(projectile.getId());
                    projectile.discard();
                    VoidPower.add(player, 4);
                    action.zeroSlashProcessed++;
                }
                continue;
            }

            if (entity instanceof LivingEntity living && canHit(player, living)) {
                action.hitEntityIds.add(living.getId());
                double atk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float slashDamage = (float) (atk * (1.20 + action.chargeTier * 0.28)
                    + 5.0 + action.chargeTier * 3.0);
                int stacksBefore = getExistenceStacks(living, now);
                float executeBonus = RelicBuildPolicy.zeroSlashExecuteBonus(
                    living.getHealth(), living.getMaxHealth(), stacksBefore);

                living.invulnerableTime = 0;
                boolean hit = living.hurtServer(level, player.damageSources().playerAttack(player), slashDamage);
                if (hit) {
                    applyExistenceReductionStacks(living, now, 1 + action.chargeTier);
                    boolean interruptImmune = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE)
                        .wrapAsHolder(living.getType()).is(ModTags.EntityTypes.ACTION_INTERRUPT_IMMUNE);
                    int stacksAfter = getExistenceStacks(living, now);
                    boolean executeTriggered = executeBonus > 0.0F
                        || RelicBuildPolicy.shouldLethalZeroExecute(
                            living.getHealth(), living.getMaxHealth(), Math.max(stacksBefore, stacksAfter), interruptImmune);

                    if (executeTriggered && living.isAlive()) {
                        float trueExecuteDmg = Math.max(6.0F, executeBonus);
                        if (RelicBuildPolicy.shouldLethalZeroExecute(
                                living.getHealth(), living.getMaxHealth(), Math.max(stacksBefore, stacksAfter), interruptImmune)) {
                            trueExecuteDmg = Math.max(trueExecuteDmg * 1.6F, living.getHealth() + 8.0F);
                        }
                        living.invulnerableTime = 0;
                        living.hurtServer(level, player.damageSources().magic(), trueExecuteDmg);
                    }

                    if (executeTriggered) {
                        stripBeneficialEffects(living);
                        VoidPower.add(player, 12);
                        NihilityVisualEffects.zeroExecuteBurst(level, living);
                        if (!action.executeNotified) {
                            NihilityNetwork.sendCombatFeedback(player, CombatFeedbackPolicy.Kind.ZERO_EXECUTE, 12, "");
                            action.executeNotified = true;
                        }
                    } else {
                        NihilityVisualEffects.weaponHitBurst(level, living, action.type.visual);
                    }

                    if (living.isAlive()) {
                        if (living instanceof Mob mob && !interruptImmune) {
                            mob.getNavigation().stop();
                        }
                        Vec3 flatPush = new Vec3(fwd.x, 0.0, fwd.z);
                        if (flatPush.lengthSqr() > 1.0E-4) {
                            flatPush = flatPush.normalize().scale(0.54 + action.chargeTier * 0.14).add(0.0, 0.18, 0.0);
                            living.setDeltaMovement(living.getDeltaMovement().add(flatPush));
                            living.hurtMarked = true;
                        }
                    }
                    action.zeroSlashProcessed++;
                }
            }
        }

        double waveFront = reach * ((step + 1) / totalSteps);
        NihilityVisualEffects.zeroSlashWaveStep(level, action.origin, fwd, waveFront, reach, action.chargeTier, step);
    }

    /**
     * Boundary Greatsword 3-wave combo:
     * uses per-wave hit tracking and resets invulnerableTime before each wave so ALL 3 waves
     * reliably connect from point-blank (-0.65 blocks) through mid-range with escalating damage!
     */
    private static void executeBoundaryWave(Player player, ServerLevel level, ActiveAction action, int wave) {
        double minForward = -0.65;
        double maxForward = 4.2 + wave * 2.6;
        double maxLateral = 2.4 + wave * 0.75;
        AABB search = player.getBoundingBox().inflate(maxForward + 1.2, 2.8, maxForward + 1.2);
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity entity : level.getEntities(player, search,
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (entity instanceof LivingEntity living && canHit(player, living)) {
                candidates.add(living);
            }
        }
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));

        long now = level.getGameTime();
        Set<Integer> waveHitIds = new HashSet<>();
        int hits = 0;
        float mult = RelicBuildPolicy.boundaryWaveDamageMultiplier(wave);
        float flatBonus = RelicBuildPolicy.boundaryWaveFlatBonus(wave);
        for (LivingEntity living : candidates) {
            if (waveHitIds.contains(living.getId()) || hits >= MAX_ACTION_TARGETS) {
                continue;
            }
            Vec3 relative = living.getBoundingBox().getCenter().subtract(player.position());
            double forward = relative.dot(action.direction);
            Vec3 lateralVector = new Vec3(relative.x, 0.0, relative.z)
                .subtract(action.direction.scale(forward));
            if (forward < minForward || forward > maxForward || lateralVector.length() > maxLateral) {
                continue;
            }

            float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * mult + flatBonus);
            if (NihilityRelicEffects.isArmorShattered(living, now) || NihilityRelicEffects.isEchoed(living, now)) {
                damage *= 1.35F;
            }
            living.invulnerableTime = 0;
            if (living.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
                waveHitIds.add(living.getId());
                NihilityRelicEffects.applyArmorShatter(living, 100);
                NihilityVisualEffects.weaponHitBurst(level, living, action.type.visual);
                boolean interruptImmune = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE)
                    .wrapAsHolder(living.getType()).is(ModTags.EntityTypes.ACTION_INTERRUPT_IMMUNE);
                if (living instanceof Mob mob && !interruptImmune) {
                    mob.getNavigation().stop();
                }
                double pushScale = wave == 2 ? 0.84 : (wave == 1 ? 0.28 : 0.22);
                double lift = wave == 2 ? 0.34 : 0.12;
                Vec3 push = action.direction.scale(pushScale).add(0.0, lift, 0.0);
                living.setDeltaMovement(living.getDeltaMovement().add(push));
                living.hurtMarked = true;
                hits++;
            }
        }

        player.swing(action.hand, true);
        if (wave == 0) {
            damageActionItem(player, action);
        }
        NihilityVisualEffects.boundaryBreakWave(level, player.position(), action.direction, wave);
    }

    private static void stripBeneficialEffects(LivingEntity target) {
        List<MobEffectInstance> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                toRemove.add(effect);
            }
        }
        for (MobEffectInstance effect : toRemove) {
            target.removeEffect(effect.getEffect());
        }
    }

    private static Vec3 findSafeDashDestination(Player player, ServerLevel level, Vec3 direction, double maxDistance) {
        Vec3 origin = player.position();
        Vec3 flatDir = new Vec3(direction.x, 0.0, direction.z);
        if (flatDir.lengthSqr() <= 1.0E-6) {
            return origin;
        }
        flatDir = flatDir.normalize();
        AABB baseBox = player.getBoundingBox();
        double[] stepOffsets = { 0.0, 0.5, 1.0 };
        for (double distance = maxDistance; distance >= 0.75; distance -= 0.25) {
            for (double dy : stepOffsets) {
                Vec3 candidate = new Vec3(
                    origin.x + flatDir.x * distance,
                    Math.max(origin.y, Math.floor(origin.y)) + dy,
                    origin.z + flatDir.z * distance
                );
                BlockPos candidatePos = BlockPos.containing(candidate);
                if (!level.hasChunk(SectionPos.blockToSectionCoord(candidatePos.getX()),
                        SectionPos.blockToSectionCoord(candidatePos.getZ()))) {
                    continue;
                }
                AABB moved = baseBox.move(candidate.subtract(origin));
                if (level.noBlockCollision(player, moved) && level.noBorderCollision(player, moved)) {
                    return candidate;
                }
            }
        }
        return origin;
    }

    private static LivingEntity nearestLivingAlongLine(ServerLevel level, Player player,
                                                        Vec3 start, Vec3 end, double inflation) {
        for (Entity entity : entitiesAlongLine(level, player, start, end, inflation)) {
            if (entity instanceof LivingEntity living && canHit(player, living)) {
                return living;
            }
        }
        return null;
    }

    private static List<Entity> entitiesAlongLine(ServerLevel level, Player player,
                                                   Vec3 start, Vec3 end, double inflation) {
        AABB bounds = new AABB(start, end).inflate(inflation);
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : level.getEntities(player, bounds,
                candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            if (entity.getBoundingBox().inflate(inflation).clip(start, end).isPresent()) {
                entities.add(entity);
            }
        }
        entities.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(start)));
        return entities;
    }

    private static boolean canHit(Player player, LivingEntity target) {
        return target != player && target.isAlive() && !player.isAlliedTo(target);
    }

    private static void applyMissingMark(LivingEntity target, Player owner, long gameTime) {
        var data = target.getPersistentData();
        data.putLong(MARK_UNTIL, gameTime + 80L);
        data.putString(MARK_OWNER, owner.getUUID().toString());
    }

    private static void clearMissingMark(LivingEntity target) {
        target.getPersistentData().remove(MARK_UNTIL);
        target.getPersistentData().remove(MARK_OWNER);
    }

    private static void damageActionItem(Player player, ActiveAction action) {
        ItemStack stack = player.getItemInHand(action.hand);
        if (stack.is(action.expectedItem)) {
            stack.hurtAndBreak(1, player, action.hand);
        }
    }

    private static void stopActionPose(Player player, ActiveAction action) {
        if (player.isUsingItem()
            && player.getUsedItemHand() == action.hand
            && player.getUseItem().is(action.expectedItem)) {
            player.stopUsingItem();
        }
    }

    private static boolean isNihilityWeapon(ItemStack stack) {
        return stack.getItem() instanceof NihilityDagger
            || stack.getItem() instanceof NihilitySword
            || stack.getItem() instanceof NihilityGreatsword
            || stack.getItem() instanceof NihilityAxe
            || stack.getItem() instanceof RiftBlade
            || stack.getItem() instanceof RiftAxe
            || stack.getItem() instanceof AshenSword
            || stack.getItem() instanceof AshenAxe;
    }

    private static int meleeEnergyGain(ItemStack stack) {
        if (stack.getItem() instanceof AshenSword || stack.getItem() instanceof AshenAxe) {
            return 2;
        }
        if (stack.getItem() instanceof RiftBlade || stack.getItem() instanceof RiftAxe) {
            return 3;
        }
        return 4;
    }

    private static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() > 1.0E-6) {
            return horizontal.normalize();
        }
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    private enum ActionType {
        MISSING_STEP(4, MISSING_STEP_DURATION, NihilityVisualEffects.WeaponAction.MISSING_STEP),
        ZERO_SLASH(0, 10, NihilityVisualEffects.WeaponAction.ZERO_SLASH),
        BOUNDARY_BREAK(15, BOUNDARY_BREAK_DURATION, NihilityVisualEffects.WeaponAction.BOUNDARY_BREAK);

        private final int firstActiveTick;
        private final int totalTicks;
        private final NihilityVisualEffects.WeaponAction visual;

        ActionType(int firstActiveTick, int totalTicks, NihilityVisualEffects.WeaponAction visual) {
            this.firstActiveTick = firstActiveTick;
            this.totalTicks = totalTicks;
            this.visual = visual;
        }
    }

    private static final class ActiveAction {
        private final ActionType type;
        private final InteractionHand hand;
        private final Item expectedItem;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final long startTick;
        private final Vec3 origin;
        private final Vec3 direction;
        private final int energyCost;
        private final int chargeTier;
        private final Set<Integer> hitEntityIds = new HashSet<>();
        private boolean activated;
        private boolean executeNotified;
        private int zeroSlashStep;
        private int zeroSlashProcessed;
        private int nextWave;

        private ActiveAction(ActionType type, InteractionHand hand, Item expectedItem,
                             net.minecraft.resources.ResourceKey<Level> dimension, long startTick,
                             Vec3 origin, Vec3 direction, int energyCost, int chargeTier) {
            this.type = type;
            this.hand = hand;
            this.expectedItem = expectedItem;
            this.dimension = dimension;
            this.startTick = startTick;
            this.origin = origin;
            this.direction = direction;
            this.energyCost = energyCost;
            this.chargeTier = chargeTier;
        }

        private boolean isStillValid(Player player) {
            return player.isAlive()
                && !player.isSpectator()
                && player.level().dimension().equals(dimension)
                && player.getItemInHand(hand).is(expectedItem);
        }
    }

    private PlayerWeaponActions() {
    }
}
