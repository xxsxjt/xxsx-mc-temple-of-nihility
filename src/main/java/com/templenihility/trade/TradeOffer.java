package com.templenihility.trade;

import com.templenihility.config.ModConfig;
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
        ItemStack adjusted = cost.copy();
        adjusted.setCount(adjustedCostCount());
        return adjusted;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public int getTier() {
        return tier;
    }

    public boolean canAfford(ItemStack playerStack) {
        ItemStack adjusted = getCost();
        return ItemStack.isSameItemSameComponents(adjusted, playerStack) && playerStack.getCount() >= adjusted.getCount();
    }

    private int adjustedCostCount() {
        int count = (int) Math.ceil(cost.getCount() * ModConfig.TRADE_PRICE_MULTIPLIER.get());
        return Math.max(1, Math.min(cost.getMaxStackSize(), count));
    }
}
