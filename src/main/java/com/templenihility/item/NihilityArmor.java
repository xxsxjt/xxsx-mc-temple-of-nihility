package com.templenihility.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

public class NihilityArmor extends Item {
    public NihilityArmor(ArmorType type, Item.Properties properties) {
        super(properties.humanoidArmor(NihilityMaterials.ARMOR, type));
    }
}
