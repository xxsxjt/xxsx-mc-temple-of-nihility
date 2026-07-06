package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

public class NihilityArmor extends Item {
    public NihilityArmor(ArmorType type, Item.Properties properties) {
        super(properties.humanoidArmor(NihilityMaterials.ARMOR, type));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_armor_set_1")
            .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_armor_set_2")
            .withStyle(ChatFormatting.GRAY));
    }
}
