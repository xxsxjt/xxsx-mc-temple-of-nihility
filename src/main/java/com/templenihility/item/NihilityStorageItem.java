package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class NihilityStorageItem extends Item {
    private final int rows;
    private final String tooltipKey;

    public NihilityStorageItem(int rows, Item.Properties properties, String tooltipKey) {
        super(properties);
        this.rows = rows;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
                StackBackedContainer container = new StackBackedContainer(stack, rows * 9);
                return rows >= 6
                    ? ChestMenu.sixRows(id, inventory, container)
                    : ChestMenu.threeRows(id, inventory, container);
            }, Component.translatable(this.getDescriptionId() + ".container")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        long used = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
            .nonEmptyItemCopyStream()
            .count();
        tooltip.accept(Component.translatable(tooltipKey, used, rows * 9).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("tooltip.templenihility.storage_no_nesting").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static final class StackBackedContainer extends SimpleContainer {
        private final ItemStack storageStack;

        private StackBackedContainer(ItemStack storageStack, int size) {
            super(size);
            this.storageStack = storageStack;
            NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
            storageStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < items.size(); i++) {
                super.setItem(i, items.get(i), false);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !(stack.getItem() instanceof NihilityStorageItem);
        }

        @Override
        public boolean stillValid(Player player) {
            return !storageStack.isEmpty();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            storageStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
        }
    }
}
