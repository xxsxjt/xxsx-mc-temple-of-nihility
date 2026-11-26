package com.templenihility.compat;

import com.templenihility.TempleNihilityMod;
import com.templenihility.blockentity.NihilityEnergyCellBlockEntity;
import com.templenihility.effect.NihilityRelicEffects;
import com.templenihility.effect.RelicBuildPolicy;
import com.templenihility.energy.VoidPower;
import com.templenihility.init.ModEffects;
import com.templenihility.init.ModItems;
import com.templenihility.world.MagnetManager;
import com.templenihility.world.NihilityDynamicLight;
import com.templenihility.world.NihilityVisualEffects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CuriosCompat {
    private static boolean eventsRegistered;
    private static final String STATE_PREFIX = TempleNihilityMod.MOD_ID + ".Curio.";
    private static final String AEGIS_READY_AT = STATE_PREFIX + "AegisReadyAt";
    private static final String RECOVERY_READY_AT = STATE_PREFIX + "RecoveryReadyAt";
    private static final String SOUL_ANCHOR_READY_AT = STATE_PREFIX + "SoulAnchorReadyAt";
    private static final String CROWN_READY_AT = STATE_PREFIX + "CrownReadyAt";
    private static final String CHAOS_OVERDRIVE_UNTIL = STATE_PREFIX + "ChaosOverdriveUntil";
    private static final String RIFT_RING_LAST_HIT = STATE_PREFIX + "RiftRingLastHit";
    private static final String HOURGLASS_LAST_TICK = STATE_PREFIX + "HourglassLastTick";
    private static final String GAUNTLET_STACKS = STATE_PREFIX + "GauntletStacks";
    private static final String GAUNTLET_LAST_HIT = STATE_PREFIX + "GauntletLastHit";
    private static final String BLOOD_SPLASH_READY_AT = STATE_PREFIX + "BloodSplashReadyAt";
    private static final String THORNS_READY_AT = STATE_PREFIX + "ThornsReadyAt";
    private static final String ABYSS_SLAM_PRIMED = STATE_PREFIX + "AbyssSlamPrimed";
    private static final String ABYSS_SLAM_READY_AT = STATE_PREFIX + "AbyssSlamReadyAt";
    private static final String WARD_MEMBRANE_READY_AT = STATE_PREFIX + "WardMembraneReadyAt";
    private static final String SLIPSTEP_READY_AT = STATE_PREFIX + "SlipstepReadyAt";
    private static final String SENTINEL_DEFLECT_READY_AT = STATE_PREFIX + "SentinelDeflectReadyAt";
    private static final String ECLIPSE_AMBUSH_UNTIL = STATE_PREFIX + "EclipseAmbushUntil";
    private static final String LAST_COMBAT_TICK = STATE_PREFIX + "LastCombatTick";

    public static void register() {
        register(ModItems.NIHILITY_RING.get(), ring());
        register(ModItems.NIHILITY_AMULET.get(), amulet());
        register(ModItems.NIHILITY_BELT.get(), belt());
        register(ModItems.NIHILITY_CLOAK.get(), cloak());
        register(ModItems.NIHILITY_CHARM.get(), charm());
        register(ModItems.NIHILITY_MAGNET.get(), magnet());
        register(ModItems.NIHILITY_REGENERATOR.get(), regenerator());
        register(ModItems.NIHILITY_MINER_CHARM.get(), minerCharm());
        register(ModItems.NIHILITY_WARD.get(), ward());
        register(ModItems.NIHILITY_GAUNTLET.get(), gauntlet());
        register(ModItems.NIHILITY_HOURGLASS.get(), hourglass());
        register(ModItems.NIHILITY_SOUL_ANCHOR.get(), soulAnchor());
        register(ModItems.NIHILITY_LANTERN.get(), lantern());
        register(ModItems.NIHILITY_RECOVERY_ORB.get(), recoveryOrb());
        register(ModItems.NIHILITY_RIFT_RING.get(), riftRing());
        register(ModItems.NIHILITY_ECLIPSE_AMULET.get(), eclipseAmulet());
        register(ModItems.NIHILITY_AEGIS_CHARM.get(), aegisCharm());
        register(ModItems.NIHILITY_WAYFINDER.get(), wayfinder());
        register(ModItems.NIHILITY_STAR_COMPASS.get(), starCompass());
        register(ModItems.NIHILITY_CROWN.get(), crown());
        register(ModItems.NIHILITY_ABYSS_MANTLE.get(), abyssMantle());
        register(ModItems.NIHILITY_SIPHON_RING.get(), siphonRing());
        register(ModItems.NIHILITY_PEARL_BELT.get(), pearlBelt());
        register(ModItems.NIHILITY_SENTINEL_EYE.get(), sentinelEye());
        register(ModItems.NIHILITY_TRADER_SEAL.get(), traderSeal());
        register(ModItems.NIHILITY_TERMINAL.get(), new ICurioItem() {});
        register(ModItems.NIHILITY_POWER_CHARM.get(), powerCharm());
        register(ModItems.NIHILITY_CONDUIT_CHARM.get(), conduitCharm());
        if (!eventsRegistered) {
            NeoForge.EVENT_BUS.addListener(CuriosCompat::playerTick);
            NeoForge.EVENT_BUS.addListener(CuriosCompat::playerLoggedOut);
            NeoForge.EVENT_BUS.addListener(CuriosCompat::incomingDamage);
            NeoForge.EVENT_BUS.addListener(CuriosCompat::damagePost);
            NeoForge.EVENT_BUS.addListener(CuriosCompat::livingDeath);
            NeoForge.EVENT_BUS.addListener(CuriosCompat::projectileImpact);
            NeoForge.EVENT_BUS.addListener(CuriosCompat::blockBreak);
            eventsRegistered = true;
        }
        TempleNihilityMod.LOGGER.info("Temple of Nihility Curios integration loaded");
    }

    private static void register(Item item, ICurioItem curio) {
        CuriosApi.registerCurio(item, curio);
    }

    public static Optional<ItemStack> findTerminal(Player player) {
        return CuriosApi.getCuriosInventory(player)
            .flatMap(handler -> handler.findFirstCurio(stack -> stack.is(ModItems.NIHILITY_TERMINAL.get())))
            .map(result -> result.stack());
    }

    public static boolean hasVoidPowerBattery(Player player) {
        return hasCurio(player, ModItems.NIHILITY_POWER_CHARM.get());
    }

    public static boolean hasVoidConduitCharm(Player player) {
        return hasCurio(player, ModItems.NIHILITY_CONDUIT_CHARM.get());
    }

    public static boolean hasTraderSeal(Player player) {
        return hasCurio(player, ModItems.NIHILITY_TRADER_SEAL.get());
    }

    public static boolean isChaosOverdriveActive(Player player) {
        long until = player.getPersistentData().getLongOr(CHAOS_OVERDRIVE_UNTIL, 0L);
        return until > 0L && until >= player.level().getGameTime();
    }

    public static void equipShowcaseLoadout(Player player, String profile) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (String slot : new String[] { "head", "necklace", "back", "belt", "hands", "ring", "charm", "curio" }) {
                handler.getStacksHandler(slot).ifPresent(stacks -> {
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        handler.setEquippedCurio(slot, i, ItemStack.EMPTY);
                    }
                });
            }
            switch (profile) {
                case "chaos" -> {
                    setCurioSafe(handler, "head", 0, new ItemStack(ModItems.NIHILITY_CROWN.get()));
                    setCurioSafe(handler, "ring", 0, new ItemStack(ModItems.NIHILITY_RING.get()));
                    setCurioSafe(handler, "hands", 0, new ItemStack(ModItems.NIHILITY_GAUNTLET.get()));
                    setCurioSafe(handler, "charm", 0, new ItemStack(ModItems.NIHILITY_CHARM.get()));
                }
                case "abyss" -> {
                    setCurioSafe(handler, "ring", 0, new ItemStack(ModItems.NIHILITY_SIPHON_RING.get()));
                    setCurioSafe(handler, "ring", 1, new ItemStack(ModItems.NIHILITY_REGENERATOR.get()));
                    setCurioSafe(handler, "back", 0, new ItemStack(ModItems.NIHILITY_ABYSS_MANTLE.get()));
                    setCurioSafe(handler, "charm", 0, new ItemStack(ModItems.NIHILITY_RECOVERY_ORB.get()));
                    setCurioSafe(handler, "necklace", 0, new ItemStack(ModItems.NIHILITY_SOUL_ANCHOR.get()));
                }
                case "void_nihility" -> {
                    setCurioSafe(handler, "ring", 0, new ItemStack(ModItems.NIHILITY_RIFT_RING.get()));
                    setCurioSafe(handler, "charm", 0, new ItemStack(ModItems.NIHILITY_SENTINEL_EYE.get()));
                    setCurioSafe(handler, "charm", 1, new ItemStack(ModItems.NIHILITY_WARD.get()));
                    setCurioSafe(handler, "belt", 0, new ItemStack(ModItems.NIHILITY_PEARL_BELT.get()));
                    setCurioSafe(handler, "necklace", 0, new ItemStack(ModItems.NIHILITY_ECLIPSE_AMULET.get()));
                }
                default -> {}
            }
        });
    }

    private static void setCurioSafe(top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler,
                                     String slot, int index, ItemStack stack) {
        handler.getStacksHandler(slot).ifPresent(stacks -> {
            if (index < stacks.getSlots()) {
                handler.setEquippedCurio(slot, index, stack);
            }
        });
    }

    private static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 20 == 0 && !hasLantern(player)) {
            NihilityDynamicLight.clear(player);
        }
    }

    private static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NihilityDynamicLight.clear(event.getEntity());
        MagnetManager.clear(event.getEntity());
    }

    private static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
            || !(hit.getEntity() instanceof Player player)
            || player.level().isClientSide()
            || event.isCanceled()
            || event.getProjectile().getOwner() == player) {
            return;
        }

        long now = player.level().getGameTime();
        if (hasCurio(player, ModItems.NIHILITY_SENTINEL_EYE.get())
            && ready(player, SENTINEL_DEFLECT_READY_AT, now)) {
            player.getPersistentData().putLong(SENTINEL_DEFLECT_READY_AT, now + 200L);
            event.setCanceled(true);
            event.getProjectile().discard();
            NihilityVisualEffects.barrierImpact(player, hit.getLocation());
            player.sendOverlayMessage(Component.translatable("message.templenihility.sentinel_deflect"));
        }
    }

    private static void blockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || !hasCurio(player, ModItems.NIHILITY_MINER_CHARM.get())) {
            return;
        }
        String path = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).getPath();
        if (path.contains("_ore") || path.equals("ancient_debris") || path.contains("nihility_ore")) {
            if (player.getRandom().nextFloat() < 0.20F) {
                VoidPower.add(player, 2);
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(event.getPos()), 3);
            }
        }
    }

    private static void incomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }

        LivingEntity target = event.getEntity();
        long now = target.level().getGameTime();

        // Offensive Curios modifiers when a Player attacks
        if (event.getSource().getEntity() instanceof Player attacker && target != attacker) {
            markCombat(attacker, now);
            applyOffensiveCurios(attacker, target, event, now);
        }

        if (!(target instanceof Player player)) {
            return;
        }
        markCombat(player, now);

        // Abyss Mantle: fire & fall immunity
        if (hasCurio(player, ModItems.NIHILITY_ABYSS_MANTLE.get())
            && (event.getSource().is(DamageTypeTags.IS_FIRE)
                || event.getSource().is(DamageTypeTags.IS_FALL))) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(6);
            player.clearFire();
            player.resetFallDistance();
            return;
        }

        // Void Cloak: -20% projectile damage
        if (hasCurio(player, ModItems.NIHILITY_CLOAK.get())
            && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            event.setAmount(event.getAmount() * 0.80F);
        }

        // Nihility Ward: -12% direct DR + 12s Zero Membrane halving next hit & +4 VoidPower
        if (hasCurio(player, ModItems.NIHILITY_WARD.get())) {
            event.setAmount(event.getAmount() * 0.88F);
            if (ready(player, WARD_MEMBRANE_READY_AT, now)) {
                player.getPersistentData().putLong(WARD_MEMBRANE_READY_AT, now + 240L);
                event.setAmount(event.getAmount() * 0.50F);
                VoidPower.add(player, 4);
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.NULLIFY);
                player.sendOverlayMessage(Component.translatable("message.templenihility.ward_membrane"));
            }
        }

        // Pearl Belt: Spatial Slipstep burst speed on hit (8s CD)
        if (hasCurio(player, ModItems.NIHILITY_PEARL_BELT.get())
            && event.getSource().getEntity() != null
            && ready(player, SLIPSTEP_READY_AT, now)) {
            player.getPersistentData().putLong(SLIPSTEP_READY_AT, now + 160L);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, true, false, true));
            event.setInvulnerabilityTicks(8);
            NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.PHASE);
        }

        // Abyss Mantle: 35% dark-flame thorns + 4s ignite against melee attackers
        if (hasCurio(player, ModItems.NIHILITY_ABYSS_MANTLE.get())
            && event.getSource().getDirectEntity() instanceof LivingEntity attackerEntity
            && attackerEntity != player
            && ready(player, THORNS_READY_AT, now)
            && player.level() instanceof ServerLevel serverLevel) {
            player.getPersistentData().putLong(THORNS_READY_AT, now + 10L);
            float reflect = Math.min(12.0F, Math.max(2.0F, event.getAmount() * 0.35F));
            attackerEntity.igniteForSeconds(4.0F);
            attackerEntity.hurtServer(serverLevel, player.damageSources().thorns(player), reflect);
        }

        // Nihility Aegis Charm: 20s CD when <=60% HP -> -55% dmg, +8 Soul Shield, 5-block knockback, 1.5s INTO_NIHILITY
        if (hasCurio(player, ModItems.NIHILITY_AEGIS_CHARM.get())
            && player.getHealth() - event.getAmount() <= player.getMaxHealth() * 0.60F
            && ready(player, AEGIS_READY_AT, now)) {
            player.getPersistentData().putLong(AEGIS_READY_AT, now + 20L * 20L);
            event.setAmount(event.getAmount() * 0.45F);
            NihilityRelicEffects.grantSoulShield(player, 8.0F, 20 * 14);
            player.addEffect(new MobEffectInstance(ModEffects.INTO_NIHILITY, 30, 0, true, false, true));
            knockbackNearby(player, 5.0, 0.62, 0.0F, false);
            notifyTriggered(player, "message.templenihility.nihility_aegis_charm",
                NihilityVisualEffects.Burst.BARRIER);
        }

        // Abyss Recovery Orb: 60s CD when <=35% HP -> +8 HP, +8 Soul Shield, -65% dmg, 6-block knockback + 6s suppress
        if (hasCurio(player, ModItems.NIHILITY_RECOVERY_ORB.get())
            && player.getHealth() - event.getAmount() <= player.getMaxHealth() * 0.35F
            && ready(player, RECOVERY_READY_AT, now)) {
            player.getPersistentData().putLong(RECOVERY_READY_AT, now + 20L * 60L);
            player.heal(8.0F);
            event.setAmount(event.getAmount() * 0.35F);
            NihilityRelicEffects.grantSoulShield(player, 8.0F, 20 * 20);
            knockbackNearby(player, 6.0, 0.72, 0.0F, true);
            notifyTriggered(player, "message.templenihility.nihility_recovery_orb",
                NihilityVisualEffects.Burst.HEAL);
        }
    }

    private static void applyOffensiveCurios(Player attacker, LivingEntity target,
                                             LivingIncomingDamageEvent event, long now) {
        float multiplier = 1.0F;
        float flatBonus = 0.0F;

        // Chaos Crown Overdrive: +40% damage
        if (isChaosOverdriveActive(attacker)) {
            multiplier *= 1.40F;
        }

        // Chaos Blood Ring: up to +56% based on missing HP
        if (hasCurio(attacker, ModItems.NIHILITY_RING.get())) {
            multiplier *= RelicBuildPolicy.chaosBloodDamageMultiplier(attacker.getHealth(), attacker.getMaxHealth());
        }

        // Chaos Gauntlet: 5-stack combo (+1.5/stack, 5th hit 2.0x Chaos Burst + CD trim)
        if (hasCurio(attacker, ModItems.NIHILITY_GAUNTLET.get())) {
            var data = attacker.getPersistentData();
            int prevStacks = data.getIntOr(GAUNTLET_STACKS, 0);
            long lastHit = data.getLongOr(GAUNTLET_LAST_HIT, -1000L);
            int nextStacks = RelicBuildPolicy.nextChaosGauntletStacks(prevStacks, now - lastHit);
            data.putInt(GAUNTLET_STACKS, nextStacks);
            data.putLong(GAUNTLET_LAST_HIT, now);
            flatBonus += nextStacks * 1.5F;
            if (nextStacks == RelicBuildPolicy.CHAOS_GAUNTLET_MAX_STACKS) {
                multiplier *= 2.0F;
                for (int i = 0; i < 10; i++) {
                    attacker.getCooldowns().tick();
                }
                Vec3 push = target.position().subtract(attacker.position());
                Vec3 flat = new Vec3(push.x, 0.0, push.z);
                if (flat.lengthSqr() > 1.0E-4) {
                    target.setDeltaMovement(target.getDeltaMovement().add(flat.normalize().scale(0.60).add(0.0, 0.22, 0.0)));
                    target.hurtMarked = true;
                }
                NihilityVisualEffects.itemUse(attacker, NihilityVisualEffects.Burst.CHAOS);
                attacker.sendOverlayMessage(Component.translatable("message.templenihility.gauntlet_chaos_burst"));
            }
        }

        // Chaos Fate Charm: 18% Fate Crit -> 1.65x dmg + +6 VoidPower
        if (hasCurio(attacker, ModItems.NIHILITY_CHARM.get()) && attacker.getRandom().nextFloat() < 0.18F) {
            multiplier *= 1.65F;
            VoidPower.add(attacker, 6);
            attacker.sendOverlayMessage(Component.translatable("message.templenihility.fate_crit"));
        }

        // Abyss Regenerator: +15% damage while Soul Shield is active
        if (hasCurio(attacker, ModItems.NIHILITY_REGENERATOR.get())
            && NihilityRelicEffects.hasSoulShield(attacker)) {
            multiplier *= 1.15F;
        }

        // Void Rift Ring: +25% Spatial Rend against Echoed or Beaconed targets
        if (hasCurio(attacker, ModItems.NIHILITY_RIFT_RING.get())
            && (NihilityRelicEffects.isEchoed(target, now) || NihilityRelicEffects.isBeaconed(target, now))) {
            multiplier *= 1.25F;
        }

        // Void Star Compass: +25% Starlight Snipe beyond 6 blocks
        if (hasCurio(attacker, ModItems.NIHILITY_STAR_COMPASS.get())
            && attacker.distanceToSqr(target) > 36.0) {
            multiplier *= 1.25F;
        }

        // Void Sentinel Eye: 35% armor pierce against Beaconed targets
        if (hasCurio(attacker, ModItems.NIHILITY_SENTINEL_EYE.get())
            && NihilityRelicEffects.isBeaconed(target, now)) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * 0.65F);
        }

        // Nihility Eclipse Amulet: Eclipse Ambush (+45% dmg & +8 VoidPower from/after stealth)
        if (hasCurio(attacker, ModItems.NIHILITY_ECLIPSE_AMULET.get())) {
            long ambushUntil = attacker.getPersistentData().getLongOr(ECLIPSE_AMBUSH_UNTIL, 0L);
            if (attacker.hasEffect(ModEffects.HIDDEN_IN_NIHILITY) || ambushUntil >= now) {
                attacker.getPersistentData().remove(ECLIPSE_AMBUSH_UNTIL);
                multiplier *= 1.45F;
                VoidPower.add(attacker, 8);
                NihilityVisualEffects.itemUse(attacker, NihilityVisualEffects.Burst.SHADOW);
                attacker.sendOverlayMessage(Component.translatable("message.templenihility.eclipse_ambush"));
            }
        }

        if (flatBonus > 0.0F || multiplier != 1.0F) {
            event.setAmount((event.getAmount() + flatBonus) * multiplier);
        }
    }

    private static void damagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)
            || attacker.level().isClientSide()
            || event.getHealthDamage() <= 0.0F) {
            return;
        }

        LivingEntity target = event.getEntity();
        long now = attacker.level().getGameTime();

        // Abyss Siphon Ring: 22% lifesteal (max 4.0) + 100% overheal -> Soul Shield (cap 12.0)
        if (hasCurio(attacker, ModItems.NIHILITY_SIPHON_RING.get())) {
            float[] healAndShield = RelicBuildPolicy.computeSiphonHealAndShield(
                event.getHealthDamage(),
                attacker.getHealth(),
                attacker.getMaxHealth(),
                NihilityRelicEffects.getActiveSoulShield(attacker));
            if (healAndShield[0] > 0.0F) {
                attacker.heal(healAndShield[0]);
            }
            if (healAndShield[1] > 0.0F) {
                NihilityRelicEffects.addSoulShield(attacker, healAndShield[1],
                    RelicBuildPolicy.SIPHON_SHIELD_CAP, 20 * 14);
            }
        }

        // Chaos Blood Ring: below 40% HP unleashes 3-block Chaos Splash (1s ICD)
        if (hasCurio(attacker, ModItems.NIHILITY_RING.get())
            && attacker.getHealth() <= attacker.getMaxHealth() * 0.40F
            && ready(attacker, BLOOD_SPLASH_READY_AT, now)
            && attacker.level() instanceof ServerLevel serverLevel) {
            attacker.getPersistentData().putLong(BLOOD_SPLASH_READY_AT, now + 20L);
            float splashDamage = Math.max(2.0F, event.getHealthDamage() * 0.35F);
            for (Entity nearby : serverLevel.getEntities(target, target.getBoundingBox().inflate(3.0),
                    candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
                if (nearby instanceof LivingEntity living && living != attacker && !attacker.isAlliedTo(living)) {
                    living.hurtServer(serverLevel, attacker.damageSources().indirectMagic(attacker, attacker), splashDamage);
                }
            }
            NihilityVisualEffects.itemUse(attacker, NihilityVisualEffects.Burst.CHAOS);
        }

        // Void Rift Ring: pull + Echo mark (60 ticks) + +3 VoidPower every 0.5s
        if (hasCurio(attacker, ModItems.NIHILITY_RIFT_RING.get())
            && ready(attacker, RIFT_RING_LAST_HIT, now)) {
            attacker.getPersistentData().putLong(RIFT_RING_LAST_HIT, now + 10L);
            Vec3 pull = attacker.position().subtract(target.position());
            if (pull.lengthSqr() > 0.04) {
                target.setDeltaMovement(target.getDeltaMovement().scale(0.65)
                    .add(pull.normalize().scale(0.32)));
                target.hurtMarked = true;
            }
            NihilityRelicEffects.markEcho(target, 60);
            VoidPower.add(attacker, 3);
        }
    }

    private static void livingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        // Hostile kill rewards for Chaos Charm (+25% XP) & Trader Seal (20% bonus shard/emerald)
        if (!(entity instanceof Player) && entity.getType().getCategory() == MobCategory.MONSTER
            && event.getSource().getEntity() instanceof Player killer
            && entity.level() instanceof ServerLevel serverLevel) {
            if (hasCurio(killer, ModItems.NIHILITY_CHARM.get())) {
                int bonusXp = Math.max(2, Math.round(entity.getMaxHealth() * 0.12F));
                ExperienceOrb.award(serverLevel, entity.position(), bonusXp);
            }
            if (hasCurio(killer, ModItems.NIHILITY_TRADER_SEAL.get())
                && killer.getRandom().nextFloat() < 0.20F) {
                Item reward = killer.getRandom().nextBoolean() ? ModItems.NIHILITY_SHARD.get() : Items.EMERALD;
                serverLevel.addFreshEntity(new ItemEntity(
                    serverLevel, entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(reward)));
            }
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        long now = player.level().getGameTime();
        RelicBuildPolicy.DeathSaveChoice choice = RelicBuildPolicy.resolveDeathSave(
            hasCurio(player, ModItems.NIHILITY_CROWN.get()),
            ready(player, CROWN_READY_AT, now),
            VoidPower.get(player),
            hasCurio(player, ModItems.NIHILITY_SOUL_ANCHOR.get()),
            ready(player, SOUL_ANCHOR_READY_AT, now)
        );

        if (choice == RelicBuildPolicy.DeathSaveChoice.CHAOS_CROWN
            && VoidPower.tryConsume(player, RelicBuildPolicy.CHAOS_CROWN_ENERGY_COST)) {
            player.getPersistentData().putLong(CROWN_READY_AT, now + 20L * 120L);
            player.getPersistentData().putLong(CHAOS_OVERDRIVE_UNTIL, now + 20L * 8L);
            preventDeath(event, player, 8.0F, 10.0F, 80,
                "message.templenihility.nihility_crown");
            knockbackNearby(player, 6.0, 0.90, 12.0F, false);
            return;
        }

        if (choice == RelicBuildPolicy.DeathSaveChoice.SOUL_ANCHOR) {
            player.getPersistentData().putLong(SOUL_ANCHOR_READY_AT, now + 20L * 60L * 10L);
            preventDeath(event, player, 12.0F, 14.0F, 80,
                "message.templenihility.nihility_soul_anchor");
            knockbackNearby(player, 7.0, 0.85, 10.0F, false);
            applySlownessWave(player, 7.0, 80, 3);
        }
    }

    private static void preventDeath(LivingDeathEvent event, Player player, float health,
                                     float shield, int invulnTicks, String messageKey) {
        event.setCanceled(true);
        player.setHealth(Math.min(player.getMaxHealth(), health));
        player.clearFire();
        player.resetFallDistance();
        player.invulnerableTime = invulnTicks;
        NihilityRelicEffects.grantSoulShield(player, shield, 20 * 30);
        NihilityVisualEffects.Burst burst = messageKey.equals("message.templenihility.nihility_crown")
            ? NihilityVisualEffects.Burst.CHAOS
            : NihilityVisualEffects.Burst.HEAL;
        notifyTriggered(player, messageKey, burst);
    }

    private static void knockbackNearby(Player player, double radius, double force,
                                        float damage, boolean applySuppress) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Entity entity : serverLevel.getEntities(player, player.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (!(entity instanceof LivingEntity living) || player.isAlliedTo(living)) {
                continue;
            }
            Vec3 away = living.position().subtract(player.position());
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            Vec3 dir = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : new Vec3(1.0, 0.0, 0.0);
            living.setDeltaMovement(living.getDeltaMovement().add(dir.scale(force).add(0.0, 0.28, 0.0)));
            living.hurtMarked = true;
            if (damage > 0.0F) {
                living.hurtServer(serverLevel, player.damageSources().indirectMagic(player, player), damage);
            }
            if (applySuppress) {
                NihilityRelicEffects.suppress(living, 120);
            }
        }
    }

    private static void applySlownessWave(Player player, double radius, int durationTicks, int amplifier) {
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (entity instanceof LivingEntity living && !player.isAlliedTo(living)) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, amplifier, true, false, true));
            }
        }
    }

    private static void markCombat(Player player, long now) {
        player.getPersistentData().putLong(LAST_COMBAT_TICK, now);
    }

    private static boolean isOutOfCombat(Player player, long now) {
        long lastCombat = player.getPersistentData().getLongOr(LAST_COMBAT_TICK, -1000L);
        return now - lastCombat >= 120L;
    }

    private static boolean ready(Player player, String key, long now) {
        return player.getPersistentData().getLongOr(key, 0L) <= now;
    }

    private static boolean hasLantern(Player player) {
        return hasCurio(player, ModItems.NIHILITY_LANTERN.get());
    }

    private static boolean hasCurio(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
            .flatMap(handler -> handler.findFirstCurio(stack -> stack.is(item)))
            .isPresent();
    }

    private static ICurioItem ring() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MAX_HEALTH,
                        new AttributeModifier(id("nihility_ring_health"), 4.0, AttributeModifier.Operation.ADD_VALUE),
                        "ring"
                    )
                    .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(id("nihility_ring_damage"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                        "ring"
                    );
            }
        };
    }

    private static ICurioItem amulet() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide()) {
                    return;
                }
                if (entity.getTicksFrozen() > 0) {
                    entity.setTicksFrozen(0);
                }
                if (entity.tickCount % 80 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, true));
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.OXYGEN_BONUS,
                        new AttributeModifier(id("nihility_amulet_oxygen"), 3.0, AttributeModifier.Operation.ADD_VALUE),
                        "necklace"
                    )
                    .withModifierAdded(
                        Attributes.WATER_MOVEMENT_EFFICIENCY,
                        new AttributeModifier(id("nihility_amulet_water_move"), 0.45, AttributeModifier.Operation.ADD_VALUE),
                        "necklace"
                    )
                    .withModifierAdded(
                        Attributes.SUBMERGED_MINING_SPEED,
                        new AttributeModifier(id("nihility_amulet_submerged_mine"), 0.6, AttributeModifier.Operation.ADD_VALUE),
                        "necklace"
                    );
            }
        };
    }

    private static ICurioItem belt() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_belt_speed"), 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.STEP_HEIGHT,
                        new AttributeModifier(id("nihility_belt_step"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.FALL_DAMAGE_MULTIPLIER,
                        new AttributeModifier(id("nihility_belt_fall"), -0.40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    );
            }
        };
    }

    private static ICurioItem cloak() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide() || entity.onGround()) {
                    return;
                }
                Vec3 movement = entity.getDeltaMovement();
                boolean changed = false;
                double nextY = movement.y;
                if (nextY < -0.48) {
                    nextY = -0.48;
                    entity.resetFallDistance();
                    changed = true;
                }
                double nextX = movement.x;
                double nextZ = movement.z;
                if (entity.isShiftKeyDown() && movement.y < -0.05) {
                    Vec3 look = entity.getLookAngle();
                    Vec3 flat = new Vec3(look.x, 0.0, look.z);
                    if (flat.lengthSqr() > 1.0E-4) {
                        Vec3 glide = flat.normalize().scale(0.055);
                        nextX = Math.max(-0.55, Math.min(0.55, nextX + glide.x));
                        nextZ = Math.max(-0.55, Math.min(0.55, nextZ + glide.z));
                        nextY = Math.max(nextY, -0.24);
                        entity.resetFallDistance();
                        changed = true;
                    }
                }
                if (changed) {
                    entity.setDeltaMovement(nextX, nextY, nextZ);
                    entity.hurtMarked = true;
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.FALL_DAMAGE_MULTIPLIER,
                    new AttributeModifier(id("nihility_cloak_fall"), -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    "back"
                );
            }
        };
    }

    private static ICurioItem charm() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.LUCK,
                    new AttributeModifier(id("nihility_charm_luck"), 2.0, AttributeModifier.Operation.ADD_VALUE),
                    "charm"
                );
            }
        };
    }

    private static ICurioItem magnet() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity instanceof Player player) {
                    MagnetManager.tick(player);
                }
            }
        };
    }

    private static ICurioItem regenerator() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide() || entity.getHealth() >= entity.getMaxHealth()) {
                    return;
                }
                if (entity instanceof Player player) {
                    long now = player.level().getGameTime();
                    if (isOutOfCombat(player, now)) {
                        if (entity.tickCount % 40 == 0) {
                            player.heal(2.5F);
                        }
                    } else if (entity.tickCount % 60 == 0) {
                        player.heal(1.5F);
                    }
                } else if (entity.tickCount % 60 == 0) {
                    entity.heal(1.5F);
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.MAX_ABSORPTION,
                    new AttributeModifier(id("nihility_regenerator_absorption"), 4.0, AttributeModifier.Operation.ADD_VALUE),
                    "ring"
                );
            }
        };
    }

    private static ICurioItem minerCharm() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MINING_EFFICIENCY,
                        new AttributeModifier(id("nihility_miner_efficiency"), 5.0, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.BLOCK_BREAK_SPEED,
                        new AttributeModifier(id("nihility_miner_break_speed"), 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "charm"
                    );
            }
        };
    }

    private static ICurioItem ward() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.ARMOR,
                        new AttributeModifier(id("nihility_ward_armor"), 3.0, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(id("nihility_ward_knockback"), 0.20, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    );
            }
        };
    }

    private static ICurioItem gauntlet() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(id("nihility_gauntlet_damage"), 2.5, AttributeModifier.Operation.ADD_VALUE),
                        "hands"
                    )
                    .withModifierAdded(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(id("nihility_gauntlet_speed"), 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "hands"
                    );
            }
        };
    }

    private static ICurioItem hourglass() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity instanceof Player player && !entity.level().isClientSide() && entity.tickCount % 2 == 0) {
                    long now = player.level().getGameTime();
                    if (player.getPersistentData().getLongOr(HOURGLASS_LAST_TICK, -1L) != now) {
                        player.getPersistentData().putLong(HOURGLASS_LAST_TICK, now);
                        player.getCooldowns().tick();
                    }
                }
            }
        };
    }

    private static ICurioItem soulAnchor() {
        return new ICurioItem() {};
    }

    private static ICurioItem lantern() {
        return new ICurioItem() {
            @Override
            public void onEquip(SlotContext slotContext, ItemStack oldStack, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    NihilityDynamicLight.update(player);
                    NihilityVisualEffects.lanternPulse(player);
                    player.sendSystemMessage(Component.translatable("message.templenihility.nihility_lantern"));
                }
            }

            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide()) {
                    return;
                }
                if (entity instanceof Player player && entity.tickCount % 5 == 0) {
                    NihilityDynamicLight.update(player);
                }
            }

            @Override
            public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
                if (slotContext.entity() instanceof Player player) {
                    NihilityDynamicLight.clear(player);
                }
            }
        };
    }

    private static ICurioItem recoveryOrb() {
        return new ICurioItem() {};
    }

    private static ICurioItem riftRing() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(id("nihility_rift_ring_damage"), 2.0, AttributeModifier.Operation.ADD_VALUE),
                        "ring"
                    )
                    .withModifierAdded(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(id("nihility_rift_ring_speed"), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "ring"
                    );
            }
        };
    }

    private static ICurioItem eclipseAmulet() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide()) {
                    return;
                }
                if (entity.isShiftKeyDown()) {
                    entity.getPersistentData().putLong(ECLIPSE_AMBUSH_UNTIL, entity.level().getGameTime() + 60L);
                }
                if (entity.tickCount % 40 != 0) {
                    return;
                }
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, true, false, true));
                if (entity.isShiftKeyDown()) {
                    entity.addEffect(new MobEffectInstance(ModEffects.HIDDEN_IN_NIHILITY, 60, 0, true, false, true));
                }
            }
        };
    }

    private static ICurioItem aegisCharm() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.ARMOR,
                        new AttributeModifier(id("nihility_aegis_armor"), 4.0, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(id("nihility_aegis_knockback"), 0.15, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    );
            }
        };
    }

    private static ICurioItem wayfinder() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!(entity instanceof Player player) || player.level().isClientSide() || player.tickCount % 20 != 0) {
                    return;
                }
                if (isOutOfCombat(player, player.level().getGameTime())) {
                    player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, true, false, true));
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_wayfinder_speed_charm"), 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_wayfinder_speed_belt"), 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.FALL_DAMAGE_MULTIPLIER,
                        new AttributeModifier(id("nihility_wayfinder_fall_charm"), -0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.FALL_DAMAGE_MULTIPLIER,
                        new AttributeModifier(id("nihility_wayfinder_fall_belt"), -0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.LUCK,
                        new AttributeModifier(id("nihility_wayfinder_luck_charm"), 1.5, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.LUCK,
                        new AttributeModifier(id("nihility_wayfinder_luck_belt"), 1.5, AttributeModifier.Operation.ADD_VALUE),
                        "belt"
                    );
            }
        };
    }

    private static ICurioItem starCompass() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide() || entity.tickCount % 60 != 0) {
                    return;
                }
                for (Entity nearby : entity.level().getEntities(entity, entity.getBoundingBox().inflate(22.0),
                        target -> target instanceof LivingEntity && target.isAlive())) {
                    if (nearby instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, true, false, true));
                        NihilityRelicEffects.markEcho(living, 120);
                    }
                }
            }
        };
    }

    private static ICurioItem crown() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MAX_HEALTH,
                        new AttributeModifier(id("nihility_crown_health"), 6.0, AttributeModifier.Operation.ADD_VALUE),
                        "head"
                    )
                    .withModifierAdded(
                        Attributes.LUCK,
                        new AttributeModifier(id("nihility_crown_luck"), 2.0, AttributeModifier.Operation.ADD_VALUE),
                        "head"
                    );
            }
        };
    }

    private static ICurioItem abyssMantle() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide()) {
                    return;
                }
                if (entity instanceof Player player) {
                    var data = player.getPersistentData();
                    long now = player.level().getGameTime();
                    if (!player.onGround()) {
                        if (player.isShiftKeyDown() && player.getDeltaMovement().y < -0.20) {
                            data.putBoolean(ABYSS_SLAM_PRIMED, true);
                        }
                    } else if (data.getBooleanOr(ABYSS_SLAM_PRIMED, false)) {
                        data.remove(ABYSS_SLAM_PRIMED);
                        if (ready(player, ABYSS_SLAM_READY_AT, now)) {
                            data.putLong(ABYSS_SLAM_READY_AT, now + 80L);
                            knockbackNearby(player, 5.0, 0.70, 8.0F, false);
                            NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.GRAVITY);
                            player.sendOverlayMessage(Component.translatable("message.templenihility.abyss_mantle_slam"));
                        }
                    }
                }
                if (!entity.onGround() && !entity.isShiftKeyDown() && entity.getDeltaMovement().y < -0.35) {
                    Vec3 movement = entity.getDeltaMovement();
                    entity.setDeltaMovement(movement.x, -0.35, movement.z);
                    entity.hurtMarked = true;
                    entity.resetFallDistance();
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.FALL_DAMAGE_MULTIPLIER,
                        new AttributeModifier(id("nihility_abyss_mantle_fall"), -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "back"
                    )
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_abyss_mantle_speed"), 0.06, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "back"
                    );
            }
        };
    }

    private static ICurioItem siphonRing() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(id("nihility_siphon_ring_damage"), 1.5, AttributeModifier.Operation.ADD_VALUE),
                        "ring"
                    )
                    .withModifierAdded(
                        Attributes.MAX_ABSORPTION,
                        new AttributeModifier(id("nihility_siphon_ring_absorption"), 4.0, AttributeModifier.Operation.ADD_VALUE),
                        "ring"
                    );
            }
        };
    }

    private static ICurioItem pearlBelt() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_pearl_belt_speed"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.STEP_HEIGHT,
                        new AttributeModifier(id("nihility_pearl_belt_step"), 1.1, AttributeModifier.Operation.ADD_VALUE),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.JUMP_STRENGTH,
                        new AttributeModifier(id("nihility_pearl_belt_jump"), 0.16, AttributeModifier.Operation.ADD_VALUE),
                        "belt"
                    );
            }
        };
    }

    private static ICurioItem sentinelEye() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide() || entity.tickCount % 50 != 0) {
                    return;
                }
                for (Entity nearby : entity.level().getEntities(entity, entity.getBoundingBox().inflate(26.0),
                        target -> target instanceof LivingEntity && target.isAlive())) {
                    if (nearby instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, true));
                        NihilityRelicEffects.markBeacon(living, 100);
                    }
                }
            }
        };
    }

    private static ICurioItem traderSeal() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.LUCK,
                    new AttributeModifier(id("nihility_trader_seal_luck"), 2.0, AttributeModifier.Operation.ADD_VALUE),
                    "charm"
                );
            }
        };
    }

    private static ICurioItem powerCharm() {
        return new ICurioItem() {
            @Override
            public void onEquip(SlotContext slotContext, ItemStack oldStack, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    VoidPower.syncArmorTooltipPower(player);
                    player.sendSystemMessage(Component.translatable(
                        "message.templenihility.power_charm_equipped", VoidPower.getMax(player)));
                }
            }

            @Override
            public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
                if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
                    VoidPower.set(player, VoidPower.get(player));
                }
            }
        };
    }

    private static ICurioItem conduitCharm() {
        return new ICurioItem() {
            @Override
            public void onEquip(SlotContext slotContext, ItemStack oldStack, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    VoidPower.syncArmorTooltipPower(player);
                    player.sendSystemMessage(Component.translatable(
                        "message.templenihility.conduit_charm_equipped", VoidPower.getMax(player)));
                }
            }

            @Override
            public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
                if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
                    VoidPower.set(player, VoidPower.get(player));
                }
            }

            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!(entity instanceof Player player) || player.level().isClientSide() || player.tickCount % 20 != 0) {
                    return;
                }
                transferWithNearbyCells(player);
            }
        };
    }

    private static void transferWithNearbyCells(Player player) {
        BlockPos base = player.blockPosition();
        int range = 4;
        int perSecond = 24;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-range, -range, -range), base.offset(range, range, range))) {
            if (!(player.level().getBlockEntity(pos) instanceof NihilityEnergyCellBlockEntity cell)) {
                continue;
            }
            if (player.isShiftKeyDown()) {
                int extracted = Math.min(perSecond, VoidPower.get(player));
                if (extracted <= 0) {
                    return;
                }
                int accepted = cell.addEnergy(extracted);
                if (accepted > 0) {
                    VoidPower.tryConsume(player, accepted);
                    return;
                }
            } else {
                int need = VoidPower.getMax(player) - VoidPower.get(player);
                if (need <= 0) {
                    return;
                }
                int extracted = cell.extractEnergy(Math.min(perSecond, need));
                if (extracted > 0) {
                    VoidPower.add(player, extracted);
                    return;
                }
            }
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, path);
    }

    private static void notifyTriggered(LivingEntity entity, String messageKey, NihilityVisualEffects.Burst burst) {
        if (entity instanceof Player player && !player.level().isClientSide()) {
            player.sendSystemMessage(Component.translatable(messageKey));
            NihilityVisualEffects.itemUse(player, burst);
        }
    }

    private CuriosCompat() {
    }
}
