package com.templenihility.compat.jei;

import com.templenihility.TempleNihilityMod;
import com.templenihility.client.NihilityClientRecipes;
import com.templenihility.client.screen.NihilityTerminalScreen;
import com.templenihility.init.ModBlocks;
import com.templenihility.init.ModItems;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class NihilityJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new NihilityRitualCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(ModItems.NIHILITY_GUIDE.get()),
            Component.translatable("jei.templenihility.guide.0").withStyle(ChatFormatting.DARK_AQUA),
            Component.translatable("jei.templenihility.guide.1").withStyle(ChatFormatting.GRAY),
            Component.translatable("jei.templenihility.guide.2").withStyle(ChatFormatting.AQUA));
        registration.addItemStackInfo(new ItemStack(ModBlocks.NIHILITY_TRANSPORT_CORE.get()),
            Component.translatable("jei.templenihility.transport.0").withStyle(ChatFormatting.DARK_AQUA),
            Component.translatable("jei.templenihility.transport.1").withStyle(ChatFormatting.GRAY),
            Component.translatable("jei.templenihility.transport.2").withStyle(ChatFormatting.AQUA));
        registration.addRecipes(NihilityRitualCategory.TYPE.get(), NihilityClientRecipes.rituals());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(NihilityRitualCategory.TYPE.get(), ModBlocks.NIHILITY_ALTAR.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(NihilityTerminalScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(NihilityTerminalScreen screen) {
                return List.of(new Rect2i(screen.getLeftPos(), screen.getTopPos(), screen.getImageWidth(), 44));
            }

            @Override
            public java.util.Optional<? extends mezz.jei.api.runtime.IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    mezz.jei.api.gui.builder.IClickableIngredientFactory factory,
                    NihilityTerminalScreen screen,
                    double mouseX,
                    double mouseY) {
                return screen.getJeiIngredientUnderMouse(mouseX, mouseY)
                    .flatMap(stack -> factory.createBuilder(stack).buildWithArea(screen.getJeiSlotArea(mouseX, mouseY)));
            }
        });

        registration.addGhostIngredientHandler(NihilityTerminalScreen.class, new IGhostIngredientHandler<>() {
            @Override
            public <I> List<Target<I>> getTargetsTyped(NihilityTerminalScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
                java.util.Optional<ItemStack> stack = ingredient.getItemStack();
                if (stack.isEmpty()) {
                    return List.of();
                }
                Rect2i searchArea = screen.getSearchAreaForJei();
                return List.of(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return searchArea;
                    }

                    @Override
                    public void accept(I ignored) {
                        screen.searchFromJei(stack.get());
                    }
                });
            }

            @Override
            public void onComplete() {
            }
        });
    }
}
