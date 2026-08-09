package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class NihilityGuideItem extends Item {
    private static final String CLIENT_BRIDGE = "com.templenihility.client.NihilityGuideClient";

    public NihilityGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openClientGuide();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_guide_use")
            .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_guide_scope")
            .withStyle(ChatFormatting.GRAY));
    }

    private static void openClientGuide() {
        try {
            Class.forName(CLIENT_BRIDGE).getMethod("open").invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
