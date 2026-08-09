package com.templenihility.client;

import com.templenihility.init.ModRecipes;
import com.templenihility.ritual.NihilityRitualRecipe;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

public final class NihilityClientRecipes {
    private static volatile List<RecipeHolder<NihilityRitualRecipe>> rituals = List.of();

    public static void recipesReceived(RecipesReceivedEvent event) {
        if (!event.getRecipeTypes().contains(ModRecipes.NIHILITY_RITUAL_TYPE.get())) {
            return;
        }
        rituals = List.copyOf(event.getRecipeMap().byType(ModRecipes.NIHILITY_RITUAL_TYPE.get()));
    }

    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        rituals = List.of();
    }

    public static List<RecipeHolder<NihilityRitualRecipe>> rituals() {
        return rituals;
    }

    private NihilityClientRecipes() {
    }
}
