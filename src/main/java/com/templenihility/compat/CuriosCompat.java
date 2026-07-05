package com.templenihility.compat;

import com.templenihility.TempleNihilityMod;
import com.templenihility.init.ModItems;
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
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CuriosCompat {
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
        register(ModItems.NIHILITY_TERMINAL.get(), new ICurioItem() {});
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, path);
    }

    private CuriosCompat() {
    }
}
