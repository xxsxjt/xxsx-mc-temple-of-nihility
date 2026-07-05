package com.templenihility.item;

import net.minecraft.world.item.Item;

public class NihilityShovel extends Item {
    public NihilityShovel(Item.Properties properties) {
        super(properties.shovel(NihilityMaterials.TOOL, 4.0f, -2.8f));
    }
}
