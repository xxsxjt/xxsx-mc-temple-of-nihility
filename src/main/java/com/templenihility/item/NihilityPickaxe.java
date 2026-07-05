package com.templenihility.item;

import net.minecraft.world.item.Item;

public class NihilityPickaxe extends Item {
    public NihilityPickaxe(Item.Properties properties) {
        super(properties.pickaxe(NihilityMaterials.TOOL, 4.5f, -2.55f));
    }
}
