package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class NihilityTooltipItem extends Item {
    private final boolean foil;
    private final String tooltipKey;
    private final NihilityAspect aspect;

    public NihilityTooltipItem(Item.Properties properties, boolean foil, String tooltipKey) {
        this(properties, foil, tooltipKey, NihilityAspect.fromTooltip(tooltipKey));
    }

    public NihilityTooltipItem(Item.Properties properties, boolean foil, String tooltipKey, NihilityAspect aspect) {
        super(properties);
        this.foil = foil;
        this.tooltipKey = tooltipKey;
        this.aspect = aspect;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(aspect.line());
        tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.DARK_AQUA));
    }
}
