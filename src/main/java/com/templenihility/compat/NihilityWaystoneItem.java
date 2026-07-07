package com.templenihility.compat;

import com.templenihility.energy.VoidPower;
import com.templenihility.item.NihilityAspect;
import com.templenihility.world.NihilityVisualEffects;
import java.util.function.Consumer;
import net.blay09.mods.waystones.api.WarpStoneTypes;
import net.blay09.mods.waystones.item.WarpStoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class NihilityWaystoneItem extends WarpStoneItem {
    public NihilityWaystoneItem(Item.Properties properties) {
        super(WarpStoneTypes.UNSCOPED, properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 0;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            int cost = WaystonesCompat.WAYSTONE_BASE_COST;
            if (VoidPower.get(player) < cost) {
                player.sendSystemMessage(Component.translatable(
                    "message.templenihility.not_enough_void_power", cost, VoidPower.get(player), VoidPower.getMax(player)));
                return InteractionResult.FAIL;
            }
            NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.RIFT);
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.VOID.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_waystone_1").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_waystone_2",
            WaystonesCompat.WAYSTONE_BASE_COST,
            WaystonesCompat.WAYSTONE_BASE_COST + WaystonesCompat.WAYSTONE_DIMENSIONAL_EXTRA_COST).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_waystone_3").withStyle(ChatFormatting.GRAY));
    }
}
