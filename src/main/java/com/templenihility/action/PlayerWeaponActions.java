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
import com.templenihility.item.NihilityAxe;
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

        // Core melee-to-energy loop: every Nihility weapon hit restores +4 VoidPower (short ICD prevents multi-hit spam)
        long nextEnergyTick = attacker.getPersistentData().getLongOr(MELEE_ENERGY_NEXT, 0L);
        if (gameTime >= nextEnergyTick) {
            attacker.getPersistentData().putLong(MELEE_ENERGY_NEXT, gameTime + 4L);
            VoidPower.add(attacker, 4);
        }

        // Blade of Zero melee passive: stacks Existence Reduction (1..3) and deals +12% per stack
        if (mainHand.getItem() instanceof NihilitySword) {
            int existingStacks = getExistenceStacks(target, gameTime);
            if (existingStacks > 0) {
                event.setAmount(event.getAmount() * (1.0F + existingStacks * 0.12F));
            }
            applyExistenceReductionStacks(target, gameTime, 1);
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
                attacker.sendOverlayMessage(Component.translatable("message.templenihility.dagger_backstab"));
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
        if (!VoidPower.tryConsume(player, energyCost)) {
            sendNoPower(player, energyCost);
            return false;
        }

        Vec3 direction = (type == ActionType.BOUNDARY_BREAK || type == ActionType.MISSING_STEP)
            ? horizontalLook(player)
            : player.getLookAngle().normalize();
        ACTIVE.put(player.getUUID(), new ActiveAction(
            type, hand, stack.getItem(), level.dimension(), level.getGameTime(), direction, energyCost, chargeTier));
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
        if (VoidPower.get(player) < energyCost) {
            sendNoPower(player, energyCost);
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
        if (!action.activated && elapsed >= action.type.firstActiveTick) {
            action.activated = true;
            executeZeroSlash(player, level, action);
        }
    }

    private static void tickBoundaryBreak(Player player, ServerLevel level, ActiveAction action, long elapsed) {
        player.setSprinting(false);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * 0.42, movement.y, movement.z * 0.42);
        player.hurtMarked = true;

        int[] waveTicks = {16, 20, 24};
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

    private static void executeZeroSlash(Player player, ServerLevel level, ActiveAction action) {
        double reach = 7.5 + action.chargeTier * 2.25;
        double corridor = 1.2 + action.chargeTier * 0.25;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(action.direction.scale(reach));
        List<Entity> targets = entitiesAlongLine(level, player, start, end, corridor);
        long now = level.getGameTime();
        int processed = 0;
        boolean executeNotified = false;
        for (Entity entity : targets) {
            if (processed >= MAX_ACTION_TARGETS) {
                break;
            }
            if (entity instanceof Projectile projectile) {
                if (projectile.isPickable() && projectile.getOwner() != player) {
                    projectile.discard();
                    VoidPower.add(player, 4);
                    processed++;
                }
                continue;
            }
            if (entity instanceof LivingEntity living && canHit(player, living)) {
                double atk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float damage = (float) (atk * (1.15 + action.chargeTier * 0.25)
                    + 4.0 + action.chargeTier * 2.5);
                int stacks = getExistenceStacks(living, now);
                float executeBonus = RelicBuildPolicy.zeroSlashExecuteBonus(
                    living.getHealth(), living.getMaxHealth(), stacks);
                if (executeBonus > 0.0F) {
                    damage += executeBonus;
                }
                if (living.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
                    applyExistenceReductionStacks(living, now, 1 + action.chargeTier);
                    if (executeBonus > 0.0F) {
                        stripBeneficialEffects(living);
                        VoidPower.add(player, 12);
                        if (!executeNotified) {
                            NihilityNetwork.sendCombatFeedback(player, CombatFeedbackPolicy.Kind.ZERO_EXECUTE, 12, "");
                            executeNotified = true;
                        }
                    }
                    NihilityVisualEffects.weaponHitBurst(level, living, action.type.visual);
                    if (living instanceof Mob mob) {
                        mob.getNavigation().stop();
                    }
                    Vec3 flatPush = new Vec3(action.direction.x, 0.0, action.direction.z);
                    if (flatPush.lengthSqr() > 1.0E-4) {
                        flatPush = flatPush.normalize().scale(0.58 + action.chargeTier * 0.14).add(0.0, 0.20, 0.0);
                        living.setDeltaMovement(living.getDeltaMovement().add(flatPush));
                        living.hurtMarked = true;
                    }
                    processed++;
                }
            }
        }
        player.swing(action.hand, true);
        damageActionItem(player, action);
        NihilityVisualEffects.zeroSlash(level, start, action.direction, reach, action.chargeTier);
    }

    private static void executeBoundaryWave(Player player, ServerLevel level, ActiveAction action, int wave) {
        double minForward = Math.max(-0.5, wave * 1.35 - 0.8);
        double maxForward = 3.6 + wave * 2.85;
        double maxLateral = 2.15 + wave * 0.75;
        AABB search = player.getBoundingBox().inflate(maxForward + 1.0, 2.5, maxForward + 1.0);
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity entity : level.getEntities(player, search,
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (entity instanceof LivingEntity living && canHit(player, living)) {
                candidates.add(living);
            }
        }
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));

        long now = level.getGameTime();
        int hits = 0;
        for (LivingEntity living : candidates) {
            if (action.hitEntityIds.contains(living.getId()) || hits >= MAX_ACTION_TARGETS) {
                continue;
            }
            Vec3 relative = living.position().subtract(player.position());
            double forward = relative.dot(action.direction);
            Vec3 lateralVector = relative.subtract(action.direction.scale(forward));
            if (forward < minForward || forward > maxForward || lateralVector.length() > maxLateral) {
                continue;
            }

            float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.45 + wave * 3.0);
            if (NihilityRelicEffects.isArmorShattered(living, now) || NihilityRelicEffects.isEchoed(living, now)) {
                damage *= 1.35F;
            }
            if (living.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
                action.hitEntityIds.add(living.getId());
                NihilityRelicEffects.applyArmorShatter(living, 80);
                NihilityVisualEffects.weaponHitBurst(level, living, action.type.visual);
                boolean interruptImmune = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE)
                    .wrapAsHolder(living.getType()).is(ModTags.EntityTypes.ACTION_INTERRUPT_IMMUNE);
                if (living instanceof Mob mob && !interruptImmune) {
                    mob.getNavigation().stop();
                }
                Vec3 push = action.direction.scale(0.68 + wave * 0.16).add(0.0, 0.24, 0.0);
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
            || stack.getItem() instanceof NihilityAxe;
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
        BOUNDARY_BREAK(16, BOUNDARY_BREAK_DURATION, NihilityVisualEffects.WeaponAction.BOUNDARY_BREAK);

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
        private final Vec3 direction;
        private final int energyCost;
        private final int chargeTier;
        private final Set<Integer> hitEntityIds = new HashSet<>();
        private boolean activated;
        private int nextWave;

        private ActiveAction(ActionType type, InteractionHand hand, Item expectedItem,
                             net.minecraft.resources.ResourceKey<Level> dimension, long startTick,
                             Vec3 direction, int energyCost, int chargeTier) {
            this.type = type;
            this.hand = hand;
            this.expectedItem = expectedItem;
            this.dimension = dimension;
            this.startTick = startTick;
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
