package com.templenihility.compat.jei;

import com.templenihility.TempleNihilityMod;
import com.templenihility.client.screen.NihilityTerminalScreen;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
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
