package com.templenihility.compat;

import com.templenihility.TempleNihilityMod;
import com.templenihility.blockentity.NihilityEnergyCellBlockEntity;
import com.templenihility.energy.VoidPower;
import com.templenihility.effect.NihilityRelicEffects;
import com.templenihility.init.ModEffects;
import com.templenihility.init.ModItems;
import com.templenihility.world.MagnetManager;
import com.templenihility.world.NihilityDynamicLight;
import com.templenihility.world.NihilityVisualEffects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
    private static final String RIFT_RING_LAST_HIT = STATE_PREFIX + "RiftRingLastHit";
    private static final String HOURGLASS_LAST_TICK = STATE_PREFIX + "HourglassLastTick";

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

    private static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) {
            return;
        }
        if (!hasLantern(player)) {
            NihilityDynamicLight.clear(player);
        }
    }

    private static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NihilityDynamicLight.clear(event.getEntity());
        MagnetManager.clear(event.getEntity());
    }

    private static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
            || event.isCanceled()
            || player.level().isClientSide()
            || event.getAmount() <= 0.0F) {
            return;
        }

        if (hasCurio(player, ModItems.NIHILITY_ABYSS_MANTLE.get())
            && (event.getSource().is(DamageTypeTags.IS_FIRE)
                || event.getSource().is(DamageTypeTags.IS_FALL))) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(6);
            player.clearFire();
            player.resetFallDistance();
            return;
        }

        if (hasCurio(player, ModItems.NIHILITY_WARD.get())) {
            event.setAmount(event.getAmount() * 0.90F);
        }

        long now = player.level().getGameTime();
        if (hasCurio(player, ModItems.NIHILITY_AEGIS_CHARM.get())
            && player.getHealth() - event.getAmount() <= player.getMaxHealth() * 0.60F
            && ready(player, AEGIS_READY_AT, now)) {
            player.getPersistentData().putLong(AEGIS_READY_AT, now + 20L * 30L);
            event.setAmount(event.getAmount() * 0.55F);
            NihilityRelicEffects.grantSoulShield(player, 4.0F, 20 * 12);
            notifyTriggered(player, "message.templenihility.nihility_aegis_charm",
                NihilityVisualEffects.Burst.BARRIER);
        }

        if (hasCurio(player, ModItems.NIHILITY_RECOVERY_ORB.get())
            && player.getHealth() - event.getAmount() <= player.getMaxHealth() * 0.30F
            && ready(player, RECOVERY_READY_AT, now)) {
            player.getPersistentData().putLong(RECOVERY_READY_AT, now + 20L * 90L);
            player.heal(6.0F);
            event.setAmount(event.getAmount() * 0.35F);
            NihilityRelicEffects.grantSoulShield(player, 6.0F, 20 * 20);
            notifyTriggered(player, "message.templenihility.nihility_recovery_orb",
                NihilityVisualEffects.Burst.HEAL);
        }
    }

    private static void damagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)
            || attacker.level().isClientSide()
            || event.getHealthDamage() <= 0.0F) {
            return;
        }

        if (hasCurio(attacker, ModItems.NIHILITY_SIPHON_RING.get())) {
            attacker.heal(Math.min(2.0F, event.getHealthDamage() * 0.18F));
        }

        long now = attacker.level().getGameTime();
        if (hasCurio(attacker, ModItems.NIHILITY_RIFT_RING.get())
            && ready(attacker, RIFT_RING_LAST_HIT, now)) {
            attacker.getPersistentData().putLong(RIFT_RING_LAST_HIT, now + 10L);
            LivingEntity target = event.getEntity();
            Vec3 pull = attacker.position().subtract(target.position());
            if (pull.lengthSqr() > 0.04) {
                target.setDeltaMovement(target.getDeltaMovement().scale(0.65)
                    .add(pull.normalize().scale(0.28)));
                target.hurtMarked = true;
            }
            NihilityRelicEffects.markEcho(target, 40);
            VoidPower.add(attacker, 1);
        }
    }

    private static void livingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        long now = player.level().getGameTime();
        if (hasCurio(player, ModItems.NIHILITY_SOUL_ANCHOR.get())
            && ready(player, SOUL_ANCHOR_READY_AT, now)) {
            player.getPersistentData().putLong(SOUL_ANCHOR_READY_AT, now + 20L * 60L * 10L);
            preventDeath(event, player, 8.0F, 10.0F,
                "message.templenihility.nihility_soul_anchor");
            return;
        }

        if (hasCurio(player, ModItems.NIHILITY_CROWN.get())
            && ready(player, CROWN_READY_AT, now)
            && VoidPower.tryConsume(player, 60)) {
            player.getPersistentData().putLong(CROWN_READY_AT, now + 20L * 120L);
            preventDeath(event, player, 6.0F, 8.0F,
                "message.templenihility.nihility_crown");
        }
    }

    private static void preventDeath(LivingDeathEvent event, Player player, float health,
                                     float shield, String messageKey) {
        event.setCanceled(true);
        player.setHealth(Math.min(player.getMaxHealth(), health));
        player.clearFire();
        player.resetFallDistance();
        player.invulnerableTime = 60;
        NihilityRelicEffects.grantSoulShield(player, shield, 20 * 30);
        NihilityVisualEffects.Burst burst = messageKey.equals("message.templenihility.nihility_crown")
            ? NihilityVisualEffects.Burst.CHAOS
            : NihilityVisualEffects.Burst.HEAL;
        notifyTriggered(player, messageKey, burst);
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
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.MAX_HEALTH,
                    new AttributeModifier(id("nihility_ring_health"), 4.0, AttributeModifier.Operation.ADD_VALUE),
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
                if (!entity.level().isClientSide() && entity.tickCount % 80 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, true));
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.OXYGEN_BONUS,
                    new AttributeModifier(id("nihility_amulet_oxygen"), 1.0, AttributeModifier.Operation.ADD_VALUE),
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
                        new AttributeModifier(id("nihility_belt_speed"), 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.STEP_HEIGHT,
                        new AttributeModifier(id("nihility_belt_step"), 0.5, AttributeModifier.Operation.ADD_VALUE),
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
                if (!entity.level().isClientSide() && entity.getDeltaMovement().y < -0.55) {
                    Vec3 movement = entity.getDeltaMovement();
                    entity.setDeltaMovement(movement.x, -0.55, movement.z);
                    entity.hurtMarked = true;
                    entity.resetFallDistance();
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.FALL_DAMAGE_MULTIPLIER,
                    new AttributeModifier(id("nihility_cloak_fall"), -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
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
                    new AttributeModifier(id("nihility_charm_luck"), 1.0, AttributeModifier.Operation.ADD_VALUE),
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
                if (!entity.level().isClientSide() && entity.tickCount % 120 == 0 && entity.getHealth() < entity.getMaxHealth()) {
                    entity.heal(1.0f);
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.MAX_ABSORPTION,
                    new AttributeModifier(id("nihility_regenerator_absorption"), 2.0, AttributeModifier.Operation.ADD_VALUE),
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
                        new AttributeModifier(id("nihility_miner_efficiency"), 3.0, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.BLOCK_BREAK_SPEED,
                        new AttributeModifier(id("nihility_miner_break_speed"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
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
                        new AttributeModifier(id("nihility_ward_armor"), 2.0, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(id("nihility_ward_knockback"), 0.15, AttributeModifier.Operation.ADD_VALUE),
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
                        new AttributeModifier(id("nihility_gauntlet_damage"), 2.0, AttributeModifier.Operation.ADD_VALUE),
                        "hands"
                    )
                    .withModifierAdded(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(id("nihility_gauntlet_speed"), 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
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
                        new AttributeModifier(id("nihility_rift_ring_damage"), 1.5, AttributeModifier.Operation.ADD_VALUE),
                        "ring"
                    )
                    .withModifierAdded(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(id("nihility_rift_ring_speed"), 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
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
                if (entity.level().isClientSide() || entity.tickCount % 40 != 0) {
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
                        new AttributeModifier(id("nihility_aegis_armor"), 3.0, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(id("nihility_aegis_knockback"), 0.12, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    );
            }
        };
    }

    private static ICurioItem wayfinder() {
        return new ICurioItem() {
            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_wayfinder_speed_charm"), 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_wayfinder_speed_belt"), 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.LUCK,
                        new AttributeModifier(id("nihility_wayfinder_luck_charm"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                        "charm"
                    )
                    .withModifierAdded(
                        Attributes.LUCK,
                        new AttributeModifier(id("nihility_wayfinder_luck_belt"), 1.0, AttributeModifier.Operation.ADD_VALUE),
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
                if (entity.level().isClientSide() || entity.tickCount % 80 != 0) {
                    return;
                }
                for (Entity nearby : entity.level().getEntities(entity, entity.getBoundingBox().inflate(18.0),
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
                        new AttributeModifier(id("nihility_crown_luck"), 1.0, AttributeModifier.Operation.ADD_VALUE),
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
                if (!entity.level().isClientSide() && entity.getDeltaMovement().y < -0.35) {
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
                        new AttributeModifier(id("nihility_abyss_mantle_fall"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "back"
                    )
                    .withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(id("nihility_abyss_mantle_speed"), 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
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
                        new AttributeModifier(id("nihility_siphon_ring_damage"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                        "ring"
                    )
                    .withModifierAdded(
                        Attributes.MAX_ABSORPTION,
                        new AttributeModifier(id("nihility_siphon_ring_absorption"), 2.0, AttributeModifier.Operation.ADD_VALUE),
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
                        new AttributeModifier(id("nihility_pearl_belt_speed"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.STEP_HEIGHT,
                        new AttributeModifier(id("nihility_pearl_belt_step"), 0.75, AttributeModifier.Operation.ADD_VALUE),
                        "belt"
                    )
                    .withModifierAdded(
                        Attributes.JUMP_STRENGTH,
                        new AttributeModifier(id("nihility_pearl_belt_jump"), 0.12, AttributeModifier.Operation.ADD_VALUE),
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
                if (entity.level().isClientSide() || entity.tickCount % 60 != 0) {
                    return;
                }
                for (Entity nearby : entity.level().getEntities(entity, entity.getBoundingBox().inflate(24.0),
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
