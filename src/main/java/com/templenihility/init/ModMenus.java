package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.menu.NihilityAltarMenu;
import com.templenihility.menu.NihilityTerminalMenu;
import com.templenihility.menu.NihilityTradeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, TempleNihilityMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<NihilityTerminalMenu>> NIHILITY_TERMINAL =
        MENUS.register("nihility_terminal", () -> IMenuTypeExtension.create(NihilityTerminalMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NihilityTradeMenu>> NIHILITY_TRADE =
        MENUS.register("nihility_trade", () -> IMenuTypeExtension.create(NihilityTradeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NihilityAltarMenu>> NIHILITY_ALTAR =
        MENUS.register("nihility_altar", () -> IMenuTypeExtension.create(NihilityAltarMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private ModMenus() {
    }
}
