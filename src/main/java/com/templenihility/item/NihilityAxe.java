package com.templenihility.item;

import com.templenihility.effect.NihilityRelicEffects;
import com.templenihility.energy.VoidPower;
import com.templenihility.world.NihilityVisualEffects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class NihilityAxe extends Item {
    private static final int SHATTER_CLEAVE_COST = 10;
    private static final int SHATTER_CLEAVE_COOLDOWN = 70;

    public NihilityAxe(Item.Properties properties) {
        super(properties.axe(NihilityMaterials.TOOL, 8.5f, -2.9f));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.templenihility.item_cooldown"));
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!VoidPower.tryConsume(player, SHATTER_CLEAVE_COST)) {
            player.sendOverlayMessage(Component.translatable(
                "message.templenihility.not_enough_void_power",
                SHATTER_CLEAVE_COST, VoidPower.get(player), VoidPower.getMax(player)));
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
        Vec3 forward = flatLook.lengthSqr() > 1.0E-4 ? flatLook.normalize() : new Vec3(0.0, 0.0, 1.0);

        float damage = (float) Math.max(10.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.50);
        int hits = 0;
        for (Entity entity : serverLevel.getEntities(player, player.getBoundingBox().inflate(4.4, 2.2, 4.4),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (!(entity instanceof LivingEntity living) || player.isAlliedTo(living)) {
                continue;
            }
            Vec3 toTarget = living.position().subtract(player.position());
            Vec3 flatTo = new Vec3(toTarget.x, 0.0, toTarget.z);
            double dist = flatTo.length();
            if (dist > 4.25 || (dist > 0.25 && flatTo.normalize().dot(forward) < 0.25)) {
                continue;
            }
            if (living instanceof Player targetPlayer && targetPlayer.isBlocking()) {
                targetPlayer.getCooldowns().addCooldown(targetPlayer.getUseItem(), 100);
                targetPlayer.stopUsingItem();
            }
            if (living.hurtServer(serverLevel, player.damageSources().playerAttack(player), damage)) {
                NihilityRelicEffects.applyArmorShatter(living, 100);
                if (living instanceof Mob mob) {
                    mob.getNavigation().stop();
                }
                Vec3 pushDir = dist > 0.1 ? flatTo.normalize() : forward;
                living.setDeltaMovement(living.getDeltaMovement().add(pushDir.scale(0.72).add(0.0, 0.28, 0.0)));
                living.hurtMarked = true;
                NihilityVisualEffects.weaponHitBurst(serverLevel, living, NihilityVisualEffects.WeaponAction.BOUNDARY_BREAK);
                hits++;
            }
        }

        player.swing(hand, true);
        stack.hurtAndBreak(1, player, hand);
        player.getCooldowns().addCooldown(stack, SHATTER_CLEAVE_COOLDOWN);
        NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.WAR);
        if (hits > 0) {
            player.sendOverlayMessage(Component.translatable("message.templenihility.axe_shatter_cleave", hits));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.NIHILITY.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_gear"));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_axe_action")
            .withStyle(NihilityAspect.NIHILITY.detailStyle()));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_axe_action_cost")
            .withStyle(ChatFormatting.GRAY));
    }
}
