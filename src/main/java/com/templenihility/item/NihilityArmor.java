package com.templenihility.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public class NihilityArmor extends Item {
    public enum Tier {
        STILLSTONE(1, NihilityMaterials.STILLSTONE_ARMOR),
        SHADE(2, NihilityMaterials.SHADE_ARMOR),
        NIHILITY(3, NihilityMaterials.ARMOR);

        private final int level;
        private final ArmorMaterial material;

        Tier(int level, ArmorMaterial material) {
            this.level = level;
            this.material = material;
        }

        public int level() {
            return level;
        }

        public ArmorMaterial material() {
            return material;
        }
    }

    private final Tier tier;

    public NihilityArmor(ArmorType type, Item.Properties properties) {
        this(Tier.NIHILITY, type, properties);
    }

    public NihilityArmor(Tier tier, ArmorType type, Item.Properties properties) {
        super(properties.humanoidArmor(tier.material(), type));
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (tier == Tier.STILLSTONE) {
            tooltip.accept(Component.translatable("tooltip.templenihility.tier_1_badge").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.templenihility.stillstone_armor_set_1").withStyle(ChatFormatting.WHITE));
            tooltip.accept(Component.translatable("tooltip.templenihility.stillstone_armor_set_2").withStyle(ChatFormatting.GRAY));
        } else if (tier == Tier.SHADE) {
            tooltip.accept(Component.translatable("tooltip.templenihility.tier_2_badge").withStyle(ChatFormatting.AQUA));
            tooltip.accept(Component.translatable("tooltip.templenihility.shade_armor_set_1").withStyle(ChatFormatting.WHITE));
            tooltip.accept(Component.translatable("tooltip.templenihility.shade_armor_set_2").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(NihilityAspect.NIHILITY.line());
            tooltip.accept(Component.translatable("tooltip.templenihility.nihility_armor_set_1")
                .withStyle(ChatFormatting.WHITE));
            tooltip.accept(Component.translatable("tooltip.templenihility.nihility_armor_set_2")
                .withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.templenihility.nihility_armor_set_3")
                .withStyle(ChatFormatting.GRAY));
        }
        int power = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag()
            .getIntOr(NihilityArmorEvents.STACK_VOID_POWER_KEY, 0);
        int maxPower = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag()
            .getIntOr(NihilityArmorEvents.STACK_VOID_POWER_MAX_KEY, NihilityArmorEvents.getMaxVoidPower());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_armor_power",
            power, maxPower).withStyle(ChatFormatting.AQUA));
    }
}
