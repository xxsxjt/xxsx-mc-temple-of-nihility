package com.templenihility.item;

import com.templenihility.action.PlayerWeaponActions;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

public class NihilitySword extends Item {
    public NihilitySword(Item.Properties properties) {
        super(properties.sword(NihilityMaterials.TOOL, 7.0f, -2.15f));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && !PlayerWeaponActions.prepareZeroSlash(player, stack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int chargeTicks = getUseDuration(stack, entity) - remainingTime;
        if (chargeTicks < 8) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.templenihility.zero_slash_charge_short"));
            }
            return false;
        }
        return level.isClientSide() || PlayerWeaponActions.startZeroSlash(player, player.getUsedItemHand(), chargeTicks);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.NIHILITY.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_gear"));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_sword_action")
            .withStyle(NihilityAspect.NIHILITY.detailStyle()));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_sword_action_cost")
            .withStyle(ChatFormatting.GRAY));
    }
}
