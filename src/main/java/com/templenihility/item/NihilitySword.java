package com.templenihility.item;

import net.minecraft.world.item.Item;

public class NihilitySword extends Item {
    public NihilitySword(Item.Properties properties) {
        super(properties.sword(NihilityMaterials.TOOL, 7.0f, -2.15f));
    }
}
