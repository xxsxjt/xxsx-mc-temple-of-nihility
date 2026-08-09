package com.templenihility.item;

import com.templenihility.action.PlayerWeaponActions;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class NihilityDagger extends Item {
    public NihilityDagger(Item.Properties properties) {
        super(properties.sword(NihilityMaterials.TOOL, 4.0f, -1.25f));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return PlayerWeaponActions.startMissingStep(player, hand)
            ? InteractionResult.SUCCESS_SERVER
            : InteractionResult.FAIL;
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
        return PlayerWeaponActions.MISSING_STEP_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.NIHILITY.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_gear"));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_dagger_action")
            .withStyle(NihilityAspect.NIHILITY.detailStyle()));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_dagger_action_cost")
            .withStyle(ChatFormatting.GRAY));
    }
}
