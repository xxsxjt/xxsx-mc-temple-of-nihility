package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class AshenAxe extends Item {
    public AshenAxe(Item.Properties properties) {
        super(properties.axe(NihilityMaterials.STILLSTONE_TOOL, 5.5f, -3.05f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.templenihility.tier_1_badge").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.templenihility.ashen_axe_desc").withStyle(ChatFormatting.DARK_AQUA));
    }
}
