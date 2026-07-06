package com.templenihility.compat;

import com.templenihility.TempleNihilityMod;
import com.templenihility.init.ModItems;
import com.templenihility.world.NihilityDynamicLight;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CuriosCompat {
    private static boolean eventsRegistered;

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
        if (!eventsRegistered) {
            NeoForge.EVENT_BUS.addListener(CuriosCompat::playerTick);
            NeoForge.EVENT_BUS.addListener(CuriosCompat::playerLoggedOut);
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
    }

    private static boolean hasLantern(Player player) {
        return CuriosApi.getCuriosInventory(player)
            .flatMap(handler -> handler.findFirstCurio(stack -> stack.is(ModItems.NIHILITY_LANTERN.get())))
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
                if (!entity.level().isClientSide() && entity.tickCount % 40 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, true));
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
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 100 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 220, 0, true, false, true));
                }
            }

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
                if (!(entity instanceof Player player) || entity.level().isClientSide() || entity.tickCount % 5 != 0) {
                    return;
                }

                Vec3 target = player.position().add(0.0, 0.75, 0.0);
                for (Entity nearby : player.level().getEntities(player, player.getBoundingBox().inflate(7.0),
                        e -> e instanceof ItemEntity && e.isAlive())) {
                    ItemEntity itemEntity = (ItemEntity) nearby;
                    if (itemEntity.hasPickUpDelay()) {
                        itemEntity.setNoPickUpDelay();
                    }

                    Vec3 pull = target.subtract(itemEntity.position());
                    if (pull.lengthSqr() < 1.25) {
                        itemEntity.playerTouch(player);
                    } else {
                        itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.65).add(pull.normalize().scale(0.18)));
                    }
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
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 100 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 140, 0, true, false, true));
                }
            }

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
                        "curio"
                    )
                    .withModifierAdded(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(id("nihility_gauntlet_speed"), 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        "curio"
                    );
            }
        };
    }

    private static ICurioItem hourglass() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 100 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.HASTE, 140, 0, true, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0, true, false, true));
                }
            }
        };
    }

    private static ICurioItem soulAnchor() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide()
                    && entity.tickCount % 160 == 0
                    && entity.getHealth() <= entity.getMaxHealth() * 0.35f) {
                    entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 12, 1, true, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 8, 0, true, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0, true, false, true));
                }
            }
        };
    }

    private static ICurioItem lantern() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide()) {
                    return;
                }
                if (entity instanceof Player player && entity.tickCount % 5 == 0) {
                    NihilityDynamicLight.update(player);
                }
                if (entity.tickCount % 80 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, true));
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
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (entity.level().isClientSide()
                    || entity.tickCount % 160 != 0
                    || entity.getHealth() > entity.getMaxHealth() * 0.55f) {
                    return;
                }

                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 0, true, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 0, true, false, true));
            }
        };
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
                    entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false, true));
                }
            }
        };
    }

    private static ICurioItem aegisCharm() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide()
                    && entity.tickCount % 100 == 0
                    && entity.getHealth() <= entity.getMaxHealth() * 0.6f) {
                    entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, false, true));
                }
            }

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
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 100 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0, true, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 140, 0, true, false, true));
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY.withModifierAdded(
                    Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(id("nihility_wayfinder_speed"), 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    "charm"
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
                    }
                }
            }
        };
    }

    private static ICurioItem crown() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 120 == 0 && entity.getHealth() <= entity.getMaxHealth() * 0.5f) {
                    entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
                }
            }

            @Override
            public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
                return CurioAttributeModifiers.EMPTY
                    .withModifierAdded(
                        Attributes.MAX_HEALTH,
                        new AttributeModifier(id("nihility_crown_health"), 6.0, AttributeModifier.Operation.ADD_VALUE),
                        "curio"
                    )
                    .withModifierAdded(
                        Attributes.LUCK,
                        new AttributeModifier(id("nihility_crown_luck"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                        "curio"
                    );
            }
        };
    }

    private static ICurioItem abyssMantle() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 60 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
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
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 160 == 0 && entity.getHealth() < entity.getMaxHealth()) {
                    entity.heal(1.0f);
                }
            }

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
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 80 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 120, 0, true, false, true));
                }
            }

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
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false, true));
                    }
                }
            }
        };
    }

    private static ICurioItem traderSeal() {
        return new ICurioItem() {
            @Override
            public void curioTick(SlotContext slotContext, ItemStack stack) {
                LivingEntity entity = slotContext.entity();
                if (!entity.level().isClientSide() && entity.tickCount % 120 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 180, 1, true, false, true));
                }
            }

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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, path);
    }

    private CuriosCompat() {
    }
}
