package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class RiftPickaxe extends Item {
    public RiftPickaxe(Item.Properties properties) {
        super(properties.pickaxe(NihilityMaterials.RIFT_TOOL, 1.5f, -2.8f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.templenihility.tier_2_badge").withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.templenihility.rift_pickaxe_desc").withStyle(ChatFormatting.GRAY));
    }
}
