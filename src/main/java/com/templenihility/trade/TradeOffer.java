package com.templenihility.trade;

import net.minecraft.world.item.ItemStack;

public class TradeOffer {
    private final ItemStack cost;
    private final ItemStack result;
    private final int tier;

    public TradeOffer(ItemStack cost, ItemStack result, int tier) {
        this.cost = cost;
        this.result = result;
        this.tier = tier;
    }

    public ItemStack getCost() {
        return cost.copy();
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public int getTier() {
        return tier;
    }

    public boolean canAfford(ItemStack playerStack) {
        return ItemStack.isSameItemSameComponents(cost, playerStack) && playerStack.getCount() >= cost.getCount();
    }
}
