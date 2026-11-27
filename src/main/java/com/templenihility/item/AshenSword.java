package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class AshenSword extends Item {
    public AshenSword(Item.Properties properties) {
        super(properties.sword(NihilityMaterials.STILLSTONE_TOOL, 3.5f, -2.35f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.templenihility.tier_1_badge").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.templenihility.ashen_sword_desc").withStyle(ChatFormatting.DARK_AQUA));
    }
}
