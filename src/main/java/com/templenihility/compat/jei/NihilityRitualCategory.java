package com.templenihility.compat.jei;

import com.templenihility.init.ModBlocks;
import com.templenihility.ritual.NihilityRitualRecipe;
import java.util.function.Supplier;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class NihilityRitualCategory implements IRecipeCategory<RecipeHolder<NihilityRitualRecipe>> {
    public static final Supplier<IRecipeHolderType<NihilityRitualRecipe>> TYPE =
        IRecipeHolderType.createDeferred(com.templenihility.init.ModRecipes.NIHILITY_RITUAL_TYPE);

    private final IDrawable icon;
    private final IDrawable arrow;

    public NihilityRitualCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.NIHILITY_ALTAR.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public IRecipeType<RecipeHolder<NihilityRitualRecipe>> getRecipeType() {
        return TYPE.get();
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.templenihility.ritual.title");
    }

    @Override
    public int getWidth() {
        return 118;
    }

    @Override
    public int getHeight() {
        return 54;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<NihilityRitualRecipe> holder,
                          IFocusGroup focuses) {
        NihilityRitualRecipe recipe = holder.value();
        builder.addInputSlot(8, 17)
            .setStandardSlotBackground()
            .add(recipe.input());
        builder.addOutputSlot(94, 17)
            .setOutputSlotBackground()
            .add(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<NihilityRitualRecipe> holder, IRecipeSlotsView slots,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        NihilityRitualRecipe recipe = holder.value();
        arrow.draw(graphics, 43, 18);
        graphics.text(Minecraft.getInstance().font, Component.literal("x" + recipe.inputCount()),
            27, 22, 0xFF60686A, false);
        graphics.text(Minecraft.getInstance().font,
            Component.translatable("jei.templenihility.ritual.tier", recipe.minTier()),
            4, 42, 0xFF60686A, false);
        graphics.text(Minecraft.getInstance().font,
            Component.translatable("jei.templenihility.ritual.energy", recipe.energyCost()),
            62, 42, 0xFF52777A, false);
    }

}
