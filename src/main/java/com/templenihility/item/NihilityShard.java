package com.templenihility.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NihilityShard extends Item {
    public NihilityShard(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // 发光效果
    }
}
