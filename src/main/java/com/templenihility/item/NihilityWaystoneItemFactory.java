package com.templenihility.item;

import com.templenihility.TempleNihilityMod;
import java.lang.reflect.Constructor;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

public final class NihilityWaystoneItemFactory {
    private static final String WAYSTONES_ITEM_CLASS = "com.templenihility.compat.NihilityWaystoneItem";

    public static Item create(Item.Properties properties) {
        if (!ModList.get().isLoaded("waystones")) {
            return fallback(properties);
        }

        try {
            Class<?> type = Class.forName(WAYSTONES_ITEM_CLASS);
            Constructor<?> constructor = type.getConstructor(Item.Properties.class);
            return (Item) constructor.newInstance(properties);
        } catch (ReflectiveOperationException | LinkageError e) {
            TempleNihilityMod.LOGGER.warn("Waystones is loaded but Nihility Waystone item could not be created", e);
            return fallback(properties);
        }
    }

    private static Item fallback(Item.Properties properties) {
        return new NihilityTooltipItem(properties, true, "tooltip.templenihility.nihility_waystone_missing");
    }

    private NihilityWaystoneItemFactory() {
    }
}
