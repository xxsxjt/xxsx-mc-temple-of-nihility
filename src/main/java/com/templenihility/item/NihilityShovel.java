package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class NihilityShovel extends Item {
    public NihilityShovel(Item.Properties properties) {
        super(properties.shovel(NihilityMaterials.TOOL, 4.0f, -2.8f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.NIHILITY.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_gear"));
    }
}
