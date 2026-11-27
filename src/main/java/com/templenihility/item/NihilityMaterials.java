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
    // Tier 1 (低阶 · 空寂/归尘阶): Iron+ entry-tier Void gear
    public static final ToolMaterial STILLSTONE_TOOL = new ToolMaterial(
        ModTags.Blocks.INCORRECT_FOR_STILLSTONE_TOOL,
        580,
        7.0f,
        2.5f,
        14,
        ModTags.Items.STILLSTONE_REPAIR_MATERIALS
    );

    public static final ResourceKey<EquipmentAsset> STILLSTONE_ARMOR_ASSET = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "stillstone")
    );

    public static final ArmorMaterial STILLSTONE_ARMOR = new ArmorMaterial(
        20,
        Map.of(
            ArmorType.BOOTS, 2,
            ArmorType.LEGGINGS, 5,
            ArmorType.CHESTPLATE, 6,
            ArmorType.HELMET, 2,
            ArmorType.BODY, 8
        ),
        14,
        SoundEvents.ARMOR_EQUIP_IRON,
        1.0f,
        0.0f,
        ModTags.Items.STILLSTONE_REPAIR_MATERIALS,
        STILLSTONE_ARMOR_ASSET
    );

    // Tier 2 (中阶 · 裂隙/虚影阶): Diamond+ mid-tier Altar gear
    public static final ToolMaterial RIFT_TOOL = new ToolMaterial(
        ModTags.Blocks.INCORRECT_FOR_RIFT_TOOL,
        1561,
        8.8f,
        3.8f,
        18,
        ModTags.Items.NIHILITY_REPAIR_MATERIALS
    );

    public static final ResourceKey<EquipmentAsset> SHADE_ARMOR_ASSET = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "shade")
    );

    public static final ArmorMaterial SHADE_ARMOR = new ArmorMaterial(
        33,
        Map.of(
            ArmorType.BOOTS, 3,
            ArmorType.LEGGINGS, 6,
            ArmorType.CHESTPLATE, 8,
            ArmorType.HELMET, 3,
            ArmorType.BODY, 11
        ),
        18,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.5f,
        0.05f,
        ModTags.Items.NIHILITY_REPAIR_MATERIALS,
        SHADE_ARMOR_ASSET
    );

    // Tier 3 (高阶 · 虚无圣殿终局阶): Endgame Temple Relic gear
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
