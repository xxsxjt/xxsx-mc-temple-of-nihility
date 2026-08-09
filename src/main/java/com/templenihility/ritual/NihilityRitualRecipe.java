package com.templenihility.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.templenihility.init.ModRecipes;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record NihilityRitualRecipe(
    Ingredient input,
    int inputCount,
    ItemStackTemplate resultTemplate,
    int minTier,
    int energyCost
) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<NihilityRitualRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("input").forGetter(NihilityRitualRecipe::input),
        Codec.intRange(1, 99).fieldOf("input_count").forGetter(NihilityRitualRecipe::inputCount),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(NihilityRitualRecipe::resultTemplate),
        Codec.intRange(0, 3).optionalFieldOf("min_tier", 0).forGetter(NihilityRitualRecipe::minTier),
        Codec.intRange(0, 1_000_000).optionalFieldOf("energy_cost", 0).forGetter(NihilityRitualRecipe::energyCost)
    ).apply(instance, NihilityRitualRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NihilityRitualRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NihilityRitualRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new NihilityRitualRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                buffer.readVarInt(),
                ItemStackTemplate.STREAM_CODEC.decode(buffer),
                buffer.readVarInt(),
                buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, NihilityRitualRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input());
            buffer.writeVarInt(recipe.inputCount());
            ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.resultTemplate());
            buffer.writeVarInt(recipe.minTier());
            buffer.writeVarInt(recipe.energyCost());
        }
    };

    public NihilityRitualRecipe {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(resultTemplate, "resultTemplate");
        if (input.isEmpty()) {
            throw new IllegalArgumentException("input must not be empty");
        }
        if (inputCount < 1 || inputCount > 99) {
            throw new IllegalArgumentException("input_count must be between 1 and 99");
        }
        if (resultTemplate.count() < 1 || resultTemplate.count() > 99) {
            throw new IllegalArgumentException("result count must be between 1 and 99");
        }
        if (minTier < 0 || minTier > 3) {
            throw new IllegalArgumentException("min_tier must be between 0 and 3");
        }
        if (energyCost < 0) {
            throw new IllegalArgumentException("energy_cost must not be negative");
        }
    }

    public ItemStack result() {
        return resultTemplate.create();
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.test(stack);
    }

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        ItemStack stack = recipeInput.item();
        return accepts(stack) && stack.getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "templenihility.ritual";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(input);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<NihilityRitualRecipe> getSerializer() {
        return ModRecipes.NIHILITY_RITUAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<NihilityRitualRecipe> getType() {
        return ModRecipes.NIHILITY_RITUAL_TYPE.get();
    }
}
