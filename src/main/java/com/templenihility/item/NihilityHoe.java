package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class NihilityHoe extends Item {
    public NihilityHoe(Item.Properties properties) {
        super(properties.hoe(NihilityMaterials.TOOL, 0.0f, -0.5f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.NIHILITY.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_gear"));
    }
}
