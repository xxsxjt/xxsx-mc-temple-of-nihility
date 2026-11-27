package com.templenihility.item;

import com.templenihility.TempleNihilityMod;
import com.templenihility.action.PlayerWeaponActions;
import com.templenihility.effect.VoidPhaseEvents;
import com.templenihility.effect.CombatFeedbackPolicy;
import com.templenihility.energy.VoidPower;
import com.templenihility.network.NihilityNetwork;
import com.templenihility.init.ModItems;
import com.templenihility.world.NihilityVisualEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class NihilityArmorEvents {
    public static final String STACK_VOID_POWER_KEY = VoidPower.STACK_POWER_KEY;
    public static final String STACK_VOID_POWER_MAX_KEY = VoidPower.STACK_MAX_POWER_KEY;
    private static final String ZERO_COUNTER_UNTIL = TempleNihilityMod.MOD_ID + ".ZeroCounterUntil";
    private static final String LAST_DODGE_AUDIO_TICK = TempleNihilityMod.MOD_ID + ".LastDodgeAudioTick";
    private static final double DODGE_CHANCE = 0.25;

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 10 == 0) {
            NihilityNetwork.syncHud(player);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }

        int pieces = countEquippedPieces(player);
        if (pieces >= 2) {
            VoidPower.add(player, 2);
            accelerateHarmfulEffectDecay(player);
        }
        if (pieces >= 4) {
            if (VoidPower.get(player) > 0 && repairFirstDamagedNihilityItem(player)) {
                VoidPower.tryConsume(player, 1);
            }
        }
        VoidPower.syncArmorTooltipPower(player);
        NihilityNetwork.syncHud(player);
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F || event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity target = event.getEntity();
        long now = target.level().getGameTime();

        // 4pc Offensive Payoff: Zero Counter Strike primed by a phase dodge
        if (event.getSource().getEntity() instanceof Player attacker && target != attacker) {
            long counterUntil = attacker.getPersistentData().getLongOr(ZERO_COUNTER_UNTIL, 0L);
            if (counterUntil >= now) {
                attacker.getPersistentData().remove(ZERO_COUNTER_UNTIL);
                event.setAmount(event.getAmount() * 1.55F);
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
                PlayerWeaponActions.applyExistenceReductionStacks(target, now, 3);
                if (target.level() instanceof ServerLevel level) {
                    NihilityVisualEffects.weaponHitBurst(level, target, NihilityVisualEffects.WeaponAction.ZERO_SLASH);
                }
                NihilityNetwork.sendCombatFeedback(attacker, CombatFeedbackPolicy.Kind.ZERO_COUNTER, 0, "");
            } else if (counterUntil > 0L) {
                attacker.getPersistentData().remove(ZERO_COUNTER_UNTIL);
            }
        }

        if (!(target instanceof Player player)
            || VoidPhaseEvents.hasFullPhase(player)
            || !hasFullSet(player)
            || !isAttack(event.getSource())) {
            return;
        }

        if (!VoidPower.tryConsume(player, 1)) {
            return;
        }

        boolean dodged = player.getRandom().nextDouble() < DODGE_CHANCE;
        if (dodged) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(12);
            VoidPower.add(player, 12);
            player.getPersistentData().putLong(ZERO_COUNTER_UNTIL, now + 80L);
            emitPhaseShockwave(player);
            long lastAudio = player.getPersistentData().getLongOr(LAST_DODGE_AUDIO_TICK, -100L);
            boolean playAudio = CombatFeedbackPolicy.shouldPlayDodgeAudio(now, lastAudio);
            if (playAudio) {
                player.getPersistentData().putLong(LAST_DODGE_AUDIO_TICK, now);
            }
            NihilityVisualEffects.phaseDodgeBurst(player, playAudio);
            NihilityNetwork.sendCombatFeedback(player, CombatFeedbackPolicy.Kind.PHASE_DODGE, 11, "");
        }
        VoidPower.syncArmorTooltipPower(player);
    }

    public static int getZeroCounterRemainingTicks(Player player, long now) {
        long until = player.getPersistentData().getLongOr(ZERO_COUNTER_UNTIL, 0L);
        return until > now ? (int) Math.min(200L, until - now) : 0;
    }

    public static int getVoidPower(Player player) {
        return VoidPower.get(player);
    }

    public static int getMaxVoidPower() {
        return VoidPower.BASE_MAX;
    }

    public static boolean tryConsumeVoidPower(Player player, int amount) {
        return VoidPower.tryConsume(player, amount);
    }

    public static int countEquippedPieces(Player player) {
        int count = 0;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NIHILITY_HELMET.get())) {
            count++;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NIHILITY_CHESTPLATE.get())) {
            count++;
        }
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.NIHILITY_LEGGINGS.get())) {
            count++;
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.NIHILITY_BOOTS.get())) {
            count++;
        }
        return count;
    }

    public static boolean hasTwoPieceSet(Player player) {
        return countEquippedPieces(player) >= 2;
    }

    public static boolean hasFullSet(Player player) {
        return countEquippedPieces(player) >= 4;
    }

    private static void accelerateHarmfulEffectDecay(Player player) {
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                && !effect.isInfiniteDuration()
                && effect.getDuration() > 20) {
                harmful.add(effect);
            }
        }
        for (MobEffectInstance effect : harmful) {
            int remaining = Math.max(1, effect.getDuration() - 20);
            player.removeEffect(effect.getEffect());
            player.addEffect(new MobEffectInstance(
                effect.getEffect(),
                remaining,
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()));
        }
    }

    private static void emitPhaseShockwave(Player player) {
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(4.0),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (entity instanceof LivingEntity living && !player.isAlliedTo(living)) {
                Vec3 away = living.position().subtract(player.position());
                Vec3 flat = new Vec3(away.x, 0.0, away.z);
                Vec3 dir = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : new Vec3(1.0, 0.0, 0.0);
                living.setDeltaMovement(living.getDeltaMovement().add(dir.scale(0.55).add(0.0, 0.20, 0.0)));
                living.hurtMarked = true;
            }
        }
    }

    private static boolean isAttack(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null;
    }

    private static boolean repairFirstDamagedNihilityItem(Player player) {
        if (repairOne(player.getMainHandItem())) {
            player.getInventory().setChanged();
            return true;
        }

        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (repairOne(player.getItemBySlot(slot))) {
                player.getInventory().setChanged();
                return true;
            }
        }

        if (repairOne(player.getOffhandItem())) {
            player.getInventory().setChanged();
            return true;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (repairOne(player.getInventory().getItem(i))) {
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean repairOne(ItemStack stack) {
        if (!isRepairableNihilityStack(stack)) {
            return false;
        }
        stack.setDamageValue(stack.getDamageValue() - 1);
        return true;
    }

    private static boolean isRepairableNihilityStack(ItemStack stack) {
        return VoidPower.isNihilityDamageable(stack);
    }

    private NihilityArmorEvents() {
    }
}
