package com.templenihility.item;

import net.minecraft.world.item.Item;

public class NihilityDagger extends Item {
    public NihilityDagger(Item.Properties properties) {
        super(properties.sword(NihilityMaterials.TOOL, 4.0f, -1.25f));
    }
}
