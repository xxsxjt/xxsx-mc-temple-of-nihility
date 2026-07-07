package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;

public class NihilitySword extends Item {
    public NihilitySword(Item.Properties properties) {
        super(properties.sword(NihilityMaterials.TOOL, 7.0f, -2.15f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.NIHILITY.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_gear"));
    }
}
