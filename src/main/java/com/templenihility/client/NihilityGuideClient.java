package com.templenihility.client;

import com.templenihility.blockentity.NihilityAltarBlockEntity;
import com.templenihility.ritual.NihilityRitualRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class NihilityGuideClient {
    public static void open() {
        Minecraft.getInstance().setScreen(new BookViewScreen(
            new BookViewScreen.BookAccess(createPages())));
    }

    private static List<Component> createPages() {
        List<Component> pages = new ArrayList<>();
        for (int page = 0; page <= 7; page++) {
            pages.add(page("guide.templenihility.page." + page + ".title",
                Component.translatable("guide.templenihility.page." + page + ".body")));
        }
        pages.add(page("guide.templenihility.page.8.title", ritualPage(tier -> tier <= 1)));
        pages.add(page("guide.templenihility.page.9.title", ritualPage(tier -> tier >= 2)));
        pages.add(page("guide.templenihility.page.10.title",
            Component.translatable("guide.templenihility.page.10.body")));
        for (int page = 16; page <= 20; page++) {
            pages.add(page("guide.templenihility.page." + page + ".title",
                Component.translatable("guide.templenihility.page." + page + ".body")));
        }
        for (int page = 11; page <= 13; page++) {
            pages.add(page("guide.templenihility.page." + page + ".title",
                Component.translatable("guide.templenihility.page." + page + ".body")));
        }
        pages.add(page("guide.templenihility.page.14.title",
            Component.translatable("guide.templenihility.page.14.body",
                integrationState("jei"), integrationState("waystones"), integrationState("curios"))));
        pages.add(page("guide.templenihility.page.15.title",
            Component.translatable("guide.templenihility.page.15.body")));
        return List.copyOf(pages);
    }

    private static Component ritualPage(IntPredicate tierFilter) {
        var recipes = NihilityClientRecipes.rituals().stream()
            .filter(holder -> tierFilter.test(holder.value().minTier()))
            .sorted(Comparator
                .comparingInt((net.minecraft.world.item.crafting.RecipeHolder<NihilityRitualRecipe> holder) ->
                    holder.value().minTier())
                .thenComparing(holder -> holder.id().identifier().toString()))
            .toList();
        if (recipes.isEmpty()) {
            return Component.translatable("guide.templenihility.ritual_sync_missing");
        }

        MutableComponent body = Component.empty();
        for (int index = 0; index < recipes.size(); index++) {
            NihilityRitualRecipe recipe = recipes.get(index).value();
            ItemStack input = recipe.input().getValues().isBound() && recipe.input().getValues().size() > 0
                ? new ItemStack(recipe.input().getValues().get(0))
                : ItemStack.EMPTY;
            Component inputName = input.isEmpty()
                ? Component.translatable("guide.templenihility.ritual_unknown_input")
                : input.getHoverName();
            if (index > 0) {
                body.append(Component.literal("\n\n"));
            }
            body.append(Component.translatable("guide.templenihility.ritual_line",
                Component.translatable(NihilityAltarBlockEntity.tierNameKey(recipe.minTier())),
                recipe.inputCount(), inputName,
                recipe.result().getCount(), recipe.result().getHoverName(),
                recipe.energyCost()));
        }
        return body;
    }

    private static Component page(String titleKey, Component body) {
        return Component.empty()
            .append(Component.translatable(titleKey).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA))
            .append(Component.literal("\n\n"))
            .append(body.copy().withStyle(ChatFormatting.BLACK));
    }

    private static Component integrationState(String modId) {
        return Component.translatable(ModList.get().isLoaded(modId)
                ? "guide.templenihility.integration.loaded"
                : "guide.templenihility.integration.missing")
            .withStyle(ModList.get().isLoaded(modId) ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED);
    }

    private NihilityGuideClient() {
    }
}
