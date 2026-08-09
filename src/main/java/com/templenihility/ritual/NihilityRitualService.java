package com.templenihility.ritual;

import com.templenihility.init.ModRecipes;
import com.templenihility.ritual.RitualExecutionPolicy.Status;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class NihilityRitualService {
    public static RitualPlan inspect(Level level, ItemStack input, ItemStack output,
                                     int structureTier, int availableEnergy) {
        if (input.isEmpty()) {
            return new RitualPlan(Status.NO_INPUT, Optional.empty());
        }

        Optional<RecipeHolder<NihilityRitualRecipe>> known = findKnownRecipe(level, input);
        if (known.isEmpty()) {
            return new RitualPlan(Status.NO_RECIPE, Optional.empty());
        }

        NihilityRitualRecipe recipe = known.get().value();
        ItemStack result = recipe.result();
        boolean outputFits = output.isEmpty()
            || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
        Status status = RitualExecutionPolicy.evaluate(
            true,
            true,
            input.getCount(),
            recipe.inputCount(),
            structureTier,
            recipe.minTier(),
            outputFits,
            availableEnergy,
            recipe.energyCost());
        return new RitualPlan(status, known);
    }

    public static Optional<RecipeHolder<NihilityRitualRecipe>> findKnownRecipe(Level level, ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        return allRecipes(level).stream()
            .filter(holder -> holder.value().accepts(input))
            .sorted(Comparator
                .comparingInt((RecipeHolder<NihilityRitualRecipe> holder) -> holder.value().minTier())
                .thenComparingInt(holder -> holder.value().inputCount())
                .thenComparing(holder -> holder.id().toString()))
            .findFirst();
    }

    public static List<RecipeHolder<NihilityRitualRecipe>> allRecipes(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        return List.copyOf(serverLevel.recipeAccess().recipeMap().byType(ModRecipes.NIHILITY_RITUAL_TYPE.get()));
    }

    public record RitualPlan(Status status, Optional<RecipeHolder<NihilityRitualRecipe>> recipe) {
        public NihilityRitualRecipe requiredRecipe() {
            return recipe.orElseThrow().value();
        }
    }

    public record RitualPreview(int inputCount, ItemStack result, int minTier, int energyCost) {
    }

    private NihilityRitualService() {
    }
}
