package com.templenihility.effect;

import com.templenihility.TempleNihilityMod;
import com.templenihility.init.ModEffects;
import com.templenihility.world.NihilityVisualEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class VoidPhaseEvents {
    private static final String ACTIVE_KEY = TempleNihilityMod.MOD_ID + ".VoidPhaseActive";
    private static final String PREV_MAYFLY_KEY = TempleNihilityMod.MOD_ID + ".VoidPhasePrevMayfly";
    private static final String PREV_FLYING_KEY = TempleNihilityMod.MOD_ID + ".VoidPhasePrevFlying";
    private static final String PREV_NO_GRAVITY_KEY = TempleNihilityMod.MOD_ID + ".VoidPhasePrevNoGravity";

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean fullPhase = hasFullPhase(player);
        boolean weakPhase = player.hasEffect(ModEffects.HIDDEN_IN_NIHILITY);

        if (fullPhase) {
            if (!isPhaseActive(player)) {
                rememberAbilities(player);
            }
            applyFullPhase(player);
            clearNearbyMobTargets(player);
            if (!player.level().isClientSide() && player.tickCount % 8 == 0) {
                NihilityVisualEffects.voidPhaseTrail(player);
            }
        } else if (isPhaseActive(player)) {
            restoreAbilities(player);
        }

        if (fullPhase || weakPhase) {
            player.clearFire();
            player.resetFallDistance();
        }
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (hasFullPhase(player)) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(20);
            return;
        }

        if (player.hasEffect(ModEffects.HIDDEN_IN_NIHILITY) && isEnvironmental(event.getSource())) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(10);
        }
    }

    public static void invulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && hasFullPhase(player)) {
            event.setInvulnerable(true);
        } else if (entity instanceof Player player
            && player.hasEffect(ModEffects.HIDDEN_IN_NIHILITY)
            && isEnvironmental(event.getSource())) {
            event.setInvulnerable(true);
        }
    }

    public static void targetChange(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target instanceof Player player && hasFullPhase(player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    public static void attackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.INTO_NIHILITY)) {
            player.removeEffect(ModEffects.INTO_NIHILITY);
            restoreAbilities(player);
            if (!player.level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.templenihility.into_nihility_broken"));
            }
        }

        if (event.getTarget() instanceof Player target && hasFullPhase(target)) {
            event.setCanceled(true);
        }
    }

    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Player target && hasFullPhase(target)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void entityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof Player target && hasFullPhase(target)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void projectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit
            && hit.getEntity() instanceof Player player
            && hasFullPhase(player)) {
            event.setCanceled(true);
        }
    }

    public static boolean hasFullPhase(Player player) {
        return player.hasEffect(ModEffects.INTO_NIHILITY)
            || player.hasEffect(ModEffects.MELT_INTO_NIHILITY);
    }

    private static void applyFullPhase(Player player) {
        player.noPhysics = true;
        player.setNoGravity(true);
        Abilities abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        player.onUpdateAbilities();
        player.clearFire();
        player.resetFallDistance();
    }

    private static void rememberAbilities(Player player) {
        var data = player.getPersistentData();
        Abilities abilities = player.getAbilities();
        data.putInt(ACTIVE_KEY, 1);
        data.putInt(PREV_MAYFLY_KEY, abilities.mayfly ? 1 : 0);
        data.putInt(PREV_FLYING_KEY, abilities.flying ? 1 : 0);
        data.putInt(PREV_NO_GRAVITY_KEY, player.isNoGravity() ? 1 : 0);
    }

    private static void restoreAbilities(Player player) {
        if (!isPhaseActive(player)) {
            player.noPhysics = false;
            player.setNoGravity(false);
            return;
        }

        var data = player.getPersistentData();
        Abilities abilities = player.getAbilities();
        boolean mayfly = data.getIntOr(PREV_MAYFLY_KEY, 0) == 1;
        boolean flying = data.getIntOr(PREV_FLYING_KEY, 0) == 1;
        boolean noGravity = data.getIntOr(PREV_NO_GRAVITY_KEY, 0) == 1;

        player.noPhysics = player.isSpectator();
        player.setNoGravity(noGravity);
        if (!player.isSpectator()) {
            abilities.mayfly = mayfly;
            abilities.flying = mayfly && flying;
            player.onUpdateAbilities();
        }
        data.putInt(ACTIVE_KEY, 0);
    }

    private static boolean isPhaseActive(Player player) {
        return player.getPersistentData().getIntOr(ACTIVE_KEY, 0) == 1;
    }

    private static void clearNearbyMobTargets(Player player) {
        if (player.level().isClientSide() || player.tickCount % 10 != 0) {
            return;
        }
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0))) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    private static boolean isEnvironmental(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            || source.getEntity() != null
            || source.getDirectEntity() != null
            || source.is(DamageTypeTags.IS_PROJECTILE)
            || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        return true;
    }

    private VoidPhaseEvents() {
    }
}
