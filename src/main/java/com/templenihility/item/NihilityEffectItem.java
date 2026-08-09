package com.templenihility.item;

import com.templenihility.world.GravityFieldManager;
import com.templenihility.effect.NihilityRelicEffects;
import com.templenihility.init.ModEffects;
import com.templenihility.world.NihilityVisualEffects;
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
import net.minecraft.world.item.ItemUseAnimation;
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

        if (kind.channelTicks > 0) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide() && !applyAndConsume(player, hand, stack)) {
            return InteractionResult.FAIL;
        }
        player.swing(hand, true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return kind.channelTicks;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return kind.useAnimation;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide()) {
            InteractionHand hand = player.getUsedItemHand();
            applyAndConsume(player, hand, stack);
            player.swing(hand, true);
        }
        return stack;
    }

    private boolean applyAndConsume(Player player, InteractionHand hand, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            player.sendSystemMessage(Component.translatable("message.templenihility.item_cooldown"));
            return false;
        }
        int affected = kind.apply(player);
        if (affected <= 0) {
            player.sendSystemMessage(Component.translatable("message.templenihility.item_no_targets"));
            return false;
        }
        player.getCooldowns().addCooldown(stack, kind.cooldownTicks);
        kind.consumeCost(stack, player, hand);
        player.sendSystemMessage(Component.translatable(kind.messageKey));
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return NihilityAspect.fromTooltip(tooltipKey).usesFoil() || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        NihilityAspect aspect = NihilityAspect.fromTooltip(tooltipKey);
        tooltip.accept(aspect.line());
        tooltip.accept(Component.translatable(tooltipKey).withStyle(aspect.detailStyle()));
        tooltip.accept(Component.translatable(kind.useCost.tooltipKey, kind.cooldownTicks / 20)
            .withStyle(ChatFormatting.GRAY));
    }

    public enum Kind {
        SHADOW_SIGIL(20 * 8, "message.templenihility.nihility_shadow_sigil", UseCost.CONSUME_ONE) {
            @Override
            int apply(Player player) {
                player.addEffect(new MobEffectInstance(ModEffects.INTO_NIHILITY, 20 * 12, 0, true, false, true));
                Vec3 dash = player.getLookAngle().normalize().scale(1.25).add(0.0, 0.18, 0.0);
                player.setDeltaMovement(player.getDeltaMovement().add(dash));
                player.hurtMarked = true;
                player.resetFallDistance();
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.SHADOW);
                return 1;
            }
        },
        PURIFYING_BELL(20 * 8, "message.templenihility.nihility_purifying_bell", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                removeBadEffects(player);
                player.heal(4.0F);
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
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.PURIFY);
                return affected;
            }
        },
        ECHO_LENS(20 * 8, "message.templenihility.nihility_echo_lens", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(24.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 12, 0, true, false, true));
                        NihilityRelicEffects.markEcho(living, 20 * 12);
                        affected++;
                    }
                }
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0, true, false, true));
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.ECHO);
                return affected + 1;
            }
        },
        BARRIER_CORE(20 * 12, "message.templenihility.nihility_barrier_core", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                NihilityRelicEffects.activateBarrier(player, 20 * 16, 8);
                int affected = 1;
                Vec3 center = player.position().add(0.0, 0.8, 0.0);
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(6.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        pushAway(living, center, 0.75);
                        affected++;
                    }
                }
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.BARRIER);
                return affected;
            }
        },
        PHASE_FEATHER(20 * 10, "message.templenihility.nihility_phase_feather", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                player.addEffect(new MobEffectInstance(ModEffects.MELT_INTO_NIHILITY, 20 * 8, 0, true, false, true));
                Vec3 dash = player.getLookAngle().normalize().scale(1.65).add(0.0, 0.22, 0.0);
                player.setDeltaMovement(player.getDeltaMovement().add(dash));
                player.hurtMarked = true;
                player.resetFallDistance();
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.PHASE);
                return 1;
            }
        },
        GRAVITY_SIGIL(20 * 10, "message.templenihility.nihility_gravity_sigil", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                GravityFieldManager.create(player);
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.GRAVITY);
                return 1;
            }
        },
        WAR_HORN(20 * 12, "message.templenihility.nihility_war_horn", UseCost.DAMAGE_ONE,
                20, ItemUseAnimation.TOOT_HORN) {
            @Override
            int apply(Player player) {
                int affected = 0;
                NihilityRelicEffects.activateWarResonance(player, 20 * 24);
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(8.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && isHostile(living)) {
                        NihilityRelicEffects.suppress(living, 20 * 10);
                        affected++;
                    }
                }
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.WAR);
                return affected + 1;
            }
        },
        SOUL_FLASK(20 * 6, "message.templenihility.nihility_soul_flask", UseCost.CONSUME_ONE) {
            @Override
            int apply(Player player) {
                player.heal(10.0F);
                NihilityRelicEffects.grantSoulShield(player, 6.0F, 20 * 45);
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.HEAL);
                return 1;
            }
        },
        NULL_SCROLL(20 * 8, "message.templenihility.nihility_null_scroll", UseCost.CONSUME_ONE) {
            @Override
            int apply(Player player) {
                removeBadEffects(player);
                player.clearFire();
                NihilityRelicEffects.activateNullification(player, 20 * 12);
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.NULLIFY);
                return 1;
            }
        },
        STASIS_WATCH(20 * 10, "message.templenihility.nihility_stasis_watch", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(12.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        NihilityRelicEffects.applyStasis(living, 20 * 10);
                        affected++;
                    }
                }
                if (affected > 0) {
                    NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.STASIS);
                }
                return affected;
            }
        },
        RIFT_SNARE(20 * 8, "message.templenihility.nihility_rift_snare", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                NihilityRelicEffects.createRift(player);
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.RIFT);
                return 1;
            }
        },
        ABYSSAL_DRUM(20 * 12, "message.templenihility.nihility_abyssal_drum", UseCost.DAMAGE_ONE,
                16, ItemUseAnimation.BRUSH) {
            @Override
            int apply(Player player) {
                NihilityRelicEffects.activateDrum(player, 20 * 12);
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.WAR);
                return 1;
            }
        },
        VOID_BEACON(20 * 12, "message.templenihility.nihility_void_beacon", UseCost.DAMAGE_ONE) {
            @Override
            int apply(Player player) {
                int affected = 0;
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(28.0),
                        entity -> entity instanceof LivingEntity && entity.isAlive())) {
                    if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 30, 0, true, false, true));
                        NihilityRelicEffects.markBeacon(living, 20 * 30);
                        affected++;
                    }
                }
                NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.VOID_BEACON);
                return affected + 1;
            }
        };

        private final int cooldownTicks;
        private final String messageKey;
        private final UseCost useCost;
        private final int channelTicks;
        private final ItemUseAnimation useAnimation;

        Kind(int cooldownTicks, String messageKey, UseCost useCost) {
            this(cooldownTicks, messageKey, useCost, 0, ItemUseAnimation.NONE);
        }

        Kind(int cooldownTicks, String messageKey, UseCost useCost,
             int channelTicks, ItemUseAnimation useAnimation) {
            this.cooldownTicks = cooldownTicks;
            this.messageKey = messageKey;
            this.useCost = useCost;
            this.channelTicks = channelTicks;
            this.useAnimation = useAnimation;
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
