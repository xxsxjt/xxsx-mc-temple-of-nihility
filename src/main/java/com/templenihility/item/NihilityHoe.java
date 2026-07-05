package com.templenihility.item;

import net.minecraft.world.item.Item;

public class NihilityHoe extends Item {
    public NihilityHoe(Item.Properties properties) {
        super(properties.hoe(NihilityMaterials.TOOL, 0.0f, -0.5f));
    }
}
