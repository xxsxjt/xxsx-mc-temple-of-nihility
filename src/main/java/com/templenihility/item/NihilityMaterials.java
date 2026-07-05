package com.templenihility.item;

import com.templenihility.TempleNihilityMod;
import com.templenihility.init.ModTags;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class NihilityMaterials {
    public static final ToolMaterial TOOL = new ToolMaterial(
        ModTags.Blocks.INCORRECT_FOR_NIHILITY_TOOL,
        2864,
        10.5f,
        5.5f,
        24,
        ModTags.Items.NIHILITY_REPAIR_MATERIALS
    );

    public static final ResourceKey<EquipmentAsset> ARMOR_ASSET = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility")
    );

    public static final ArmorMaterial ARMOR = new ArmorMaterial(
        45,
        Map.of(
            ArmorType.BOOTS, 4,
            ArmorType.LEGGINGS, 7,
            ArmorType.CHESTPLATE, 9,
            ArmorType.HELMET, 4,
            ArmorType.BODY, 14
        ),
        24,
        SoundEvents.ARMOR_EQUIP_NAUTILUS,
        4.0f,
        0.15f,
        ModTags.Items.NIHILITY_REPAIR_MATERIALS,
        ARMOR_ASSET
    );

    private NihilityMaterials() {
    }
}
