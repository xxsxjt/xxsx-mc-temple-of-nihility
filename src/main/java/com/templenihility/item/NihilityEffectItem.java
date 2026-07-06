package com.templenihility.item;

import com.templenihility.world.GravityFieldManager;
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
import net.minecraft.world.entity.MobCategory;
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
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.templenihility.item_cooldown"));
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            int affected = kind.apply(player);
            if (affected <= 0) {
                player.sendSystemMessage(Component.translatable("message.templenihility.item_no_targets"));
                return InteractionResult.FAIL;
            }
            player.getCooldowns().addCooldown(stack, kind.cooldownTicks);
            kind.consumeCost(stack, player, hand);
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
        tooltip.accept(Component.translatable(kind.useCost.tooltipKey, kind.cooldownTicks / 20)
            .withStyle(ChatFormatting.GRAY));
    }

    public enum Kind {
        SHADOW_SIGIL(20 * 20, "message.templenihility.nihility_shadow_sigil", UseCost.CONSUME_ONE) {
            @Override
            int apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 12, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 18, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 18, 0, true, false, true));
                Vec3 dash = player.getLookAngle().normalize().scale(1.25).add(0.0, 0.18, 0.0);
                player.setDeltaMovement(player.getDeltaMovement().add(dash));
                player.hurtMarked = true;
                player.resetFallDistance();
                return 1;
            }
        },
        PURIFYING_BELL(20 * 30, "message.templenihility.nihility_purifying_bell", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                removeBadEffects(player);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0, true, false, true));
                int affected = 1;
                Vec3 center = player.position().add(0.0, 0.8, 0.0);
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(7.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && isHostile(living)) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 0, true, false, true));
                        pushAway(living, center, 0.55);
                        affected++;
                    }
                }
                return affected;
            }
        },
        ECHO_LENS(20 * 20, "message.templenihility.nihility_echo_lens", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(24.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 12, 0, true, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 0, true, false, true));
                        affected++;
                    }
                }
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0, true, false, true));
                return affected + 1;
            }
        },
        BARRIER_CORE(20 * 45, "message.templenihility.nihility_barrier_core", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 16, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 20, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 90, 2, true, false, true));
                int affected = 1;
                Vec3 center = player.position().add(0.0, 0.8, 0.0);
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(6.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        pushAway(living, center, 0.75);
                        affected++;
                    }
                }
                return affected;
            }
        },
        PHASE_FEATHER(20 * 25, "message.templenihility.nihility_phase_feather", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 18, 2, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 18, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 24, 0, true, false, true));
                Vec3 dash = player.getLookAngle().normalize().scale(1.65).add(0.0, 0.22, 0.0);
                player.setDeltaMovement(player.getDeltaMovement().add(dash));
                player.hurtMarked = true;
                player.resetFallDistance();
                return 1;
            }
        },
        GRAVITY_SIGIL(20 * 30, "message.templenihility.nihility_gravity_sigil", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                GravityFieldManager.create(player);
                return 1;
            }
        },
        WAR_HORN(20 * 35, "message.templenihility.nihility_war_horn", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 24, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 16, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 45, 1, true, false, true));
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(8.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && isHostile(living)) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 10, 1, true, false, true));
                        affected++;
                    }
                }
                return affected + 1;
            }
        },
        SOUL_FLASK(20 * 15, "message.templenihility.nihility_soul_flask", UseCost.CONSUME_ONE) {
            @Override
            int apply(Player player) {
                player.heal(6.0f);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 45, 0, true, false, true));
                return 1;
            }
        },
        NULL_SCROLL(20 * 20, "message.templenihility.nihility_null_scroll", UseCost.CONSUME_ONE) {
            @Override
            int apply(Player player) {
                removeBadEffects(player);
                player.clearFire();
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 12, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 20, 0, true, false, true));
                return 1;
            }
        },
        STASIS_WATCH(20 * 30, "message.templenihility.nihility_stasis_watch", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(12.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 10, 4, true, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20 * 10, 1, true, false, true));
                        living.setDeltaMovement(Vec3.ZERO);
                        living.hurtMarked = true;
                        affected++;
                    }
                }
                return affected;
            }
        },
        RIFT_SNARE(20 * 25, "message.templenihility.nihility_rift_snare", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                Vec3 center = player.position().add(0.0, 1.0, 0.0);
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(9.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 10, 0, true, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 8, 2, true, false, true));
                        affected++;
                        Vec3 pull = center.subtract(living.position());
                        if (pull.lengthSqr() > 1.0E-4) {
                            living.setDeltaMovement(living.getDeltaMovement().scale(0.35).add(pull.normalize().scale(0.45)).add(0, 0.18, 0));
                        }
                    }
                }
                return affected;
            }
        },
        ABYSSAL_DRUM(20 * 40, "message.templenihility.nihility_abyssal_drum", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 30, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 18, 1, true, false, true));
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(14.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && isHostile(living)) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 12, 1, true, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 12, 1, true, false, true));
                        pushAway(living, player.position().add(0.0, 0.8, 0.0), 0.5);
                        affected++;
                    }
                }
                return affected + 1;
            }
        },
        VOID_BEACON(20 * 45, "message.templenihility.nihility_void_beacon", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 90, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 90, 1, true, false, true));
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(28.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 30, 0, true, false, true));
                        affected++;
                    }
                }
                return affected + 1;
            }
        };

        private final int cooldownTicks;
        private final String messageKey;
        private final UseCost useCost;

        Kind(int cooldownTicks, String messageKey, UseCost useCost) {
            this.cooldownTicks = cooldownTicks;
            this.messageKey = messageKey;
            this.useCost = useCost;
        }

        abstract int apply(Player player);

        void consumeCost(ItemStack stack, Player player, InteractionHand hand) {
            if (player.getAbilities().instabuild) {
                return;
            }
            if (useCost == UseCost.CONSUME_ONE) {
                stack.shrink(1);
            } else if (useCost == UseCost.DAMAGE_ONE) {
                stack.hurtAndBreak(1, player, hand);
            }
        }

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

        private static boolean isHostile(LivingEntity living) {
            return !(living instanceof Player) && living.getType().getCategory() == MobCategory.MONSTER;
        }

        private static void pushAway(LivingEntity living, Vec3 center, double strength) {
            Vec3 away = living.position().subtract(center);
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(0.0, 0.0, 1.0);
            }
            living.setDeltaMovement(living.getDeltaMovement().scale(0.25)
                .add(away.normalize().scale(strength))
                .add(0.0, 0.16, 0.0));
            living.hurtMarked = true;
            living.resetFallDistance();
        }
    }

    private enum UseCost {
        CONSUME_ONE("tooltip.templenihility.use_cost.consume"),
        DAMAGE_ONE("tooltip.templenihility.use_cost.durability");

        private final String tooltipKey;

        UseCost(String tooltipKey) {
            this.tooltipKey = tooltipKey;
        }
    }
}
