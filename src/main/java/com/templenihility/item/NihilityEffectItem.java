package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class NihilityEffectItem extends Item {
    private final Kind kind;
    private final String tooltipKey;

    public NihilityEffectItem(Kind kind, Item.Properties properties, String tooltipKey) {
        super(properties);
        this.kind = kind;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            kind.apply(player);
            player.getCooldowns().addCooldown(stack, kind.cooldownTicks);
            player.sendSystemMessage(Component.translatable(kind.messageKey));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.DARK_AQUA));
    }

    public enum Kind {
        LANTERN(20 * 30, "message.templenihility.nihility_lantern") {
            @Override
            void apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60 * 6, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 20 * 60 * 2, 0, true, false, true));
            }
        },
        RECOVERY_ORB(20 * 90, "message.templenihility.nihility_recovery_orb") {
            @Override
            void apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 10, 0, true, false, true));
            }
        },
        SHADOW_SIGIL(20 * 45, "message.templenihility.nihility_shadow_sigil") {
            @Override
            void apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 12, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 18, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 18, 0, true, false, true));
            }
        },
        PURIFYING_BELL(20 * 60, "message.templenihility.nihility_purifying_bell") {
            @Override
            void apply(Player player) {
                removeBadEffects(player);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0, true, false, true));
            }
        },
        ECHO_LENS(20 * 40, "message.templenihility.nihility_echo_lens") {
            @Override
            void apply(Player player) {
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(24.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 12, 0, true, false, true));
                    }
                }
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0, true, false, true));
            }
        },
        BARRIER_CORE(20 * 120, "message.templenihility.nihility_barrier_core") {
            @Override
            void apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 16, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 20, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 90, 2, true, false, true));
            }
        },
        PHASE_FEATHER(20 * 55, "message.templenihility.nihility_phase_feather") {
            @Override
            void apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 18, 2, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 18, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 24, 0, true, false, true));
            }
        },
        GRAVITY_SIGIL(20 * 70, "message.templenihility.nihility_gravity_sigil") {
            @Override
            void apply(Player player) {
                Vec3 center = player.position().add(0.0, 0.8, 0.0);
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(10.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 8, 2, true, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 0, true, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 8, 0, true, false, true));
                        Vec3 pull = center.subtract(living.position());
                        if (pull.lengthSqr() > 1.0E-4) {
                            living.setDeltaMovement(living.getDeltaMovement().scale(0.55).add(pull.normalize().scale(0.32)));
                        }
                    }
                }
            }
        },
        WAR_HORN(20 * 85, "message.templenihility.nihility_war_horn") {
            @Override
            void apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 24, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 16, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 45, 1, true, false, true));
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(8.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 10, 1, true, false, true));
                    }
                }
            }
        };

        private final int cooldownTicks;
        private final String messageKey;

        Kind(int cooldownTicks, String messageKey) {
            this.cooldownTicks = cooldownTicks;
            this.messageKey = messageKey;
        }

        abstract void apply(Player player);

        private static void removeBadEffects(Player player) {
            remove(player, MobEffects.POISON);
            remove(player, MobEffects.WITHER);
            remove(player, MobEffects.WEAKNESS);
            remove(player, MobEffects.SLOWNESS);
            remove(player, MobEffects.MINING_FATIGUE);
            remove(player, MobEffects.BLINDNESS);
            remove(player, MobEffects.DARKNESS);
            remove(player, MobEffects.NAUSEA);
            remove(player, MobEffects.HUNGER);
            remove(player, MobEffects.UNLUCK);
            remove(player, MobEffects.BAD_OMEN);
        }

        private static void remove(Player player, Holder<MobEffect> effect) {
            player.removeEffect(effect);
        }
    }
}
