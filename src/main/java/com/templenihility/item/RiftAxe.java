package com.templenihility.item;

import com.templenihility.action.PlayerWeaponActions;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RiftAxe extends Item {
    private static final int SUNDER_COST = 12;
    private static final int SUNDER_COOLDOWN = 80;

    public RiftAxe(Item.Properties properties) {
        super(properties.axe(NihilityMaterials.RIFT_TOOL, 6.2f, -2.95f));
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
            PlayerWeaponActions.notifyClientSkillStart(player, hand, "AXE_SHATTER");
            return InteractionResult.SUCCESS;
        }
        int effectiveCost = VoidPower.effectiveCost(player, SUNDER_COST);
        if (!VoidPower.tryConsume(player, SUNDER_COST)) {
            player.sendOverlayMessage(Component.translatable(
                "message.templenihility.not_enough_void_power",
                effectiveCost, VoidPower.get(player), VoidPower.getMax(player)));
            return InteractionResult.FAIL;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
        Vec3 forward = flatLook.lengthSqr() > 1.0E-4 ? flatLook.normalize() : new Vec3(0.0, 0.0, 1.0);
        float damage = (float) Math.max(9.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.30);
        for (Entity entity : serverLevel.getEntities(player, player.getBoundingBox().inflate(3.6, 2.0, 3.6),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (!(entity instanceof LivingEntity living) || player.isAlliedTo(living)) {
                continue;
            }
            Vec3 toTarget = living.position().subtract(player.position());
            Vec3 flatTo = new Vec3(toTarget.x, 0.0, toTarget.z);
            double dist = flatTo.length();
            if (dist > 3.5 || (dist > 0.25 && flatTo.normalize().dot(forward) < 0.30)) {
                continue;
            }
            if (living instanceof Player targetPlayer && targetPlayer.isBlocking()) {
                targetPlayer.getCooldowns().addCooldown(targetPlayer.getUseItem(), 80);
                targetPlayer.stopUsingItem();
            }
            if (living.hurtServer(serverLevel, player.damageSources().playerAttack(player), damage)) {
                NihilityRelicEffects.applyArmorShatter(living, 60);
                Vec3 pushDir = dist > 0.1 ? flatTo.normalize() : forward;
                living.setDeltaMovement(living.getDeltaMovement().add(pushDir.scale(0.45).add(0.0, 0.20, 0.0)));
                living.hurtMarked = true;
                NihilityVisualEffects.weaponHitBurst(serverLevel, living, NihilityVisualEffects.WeaponAction.BOUNDARY_BREAK);
            }
        }
        player.swing(hand, true);
        stack.hurtAndBreak(1, player, hand);
        player.getCooldowns().addCooldown(stack, SUNDER_COOLDOWN);
        NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.WAR);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.templenihility.tier_2_badge").withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.templenihility.rift_axe_desc").withStyle(ChatFormatting.WHITE));
        tooltip.accept(Component.translatable("tooltip.templenihility.rift_axe_cost").withStyle(ChatFormatting.GRAY));
    }
}
