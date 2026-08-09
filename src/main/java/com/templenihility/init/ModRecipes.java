package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.ritual.NihilityRitualRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, TempleNihilityMod.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, TempleNihilityMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<NihilityRitualRecipe>> NIHILITY_RITUAL_SERIALIZER =
        SERIALIZERS.register("nihility_ritual", () -> new RecipeSerializer<>(
            NihilityRitualRecipe.CODEC, NihilityRitualRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<NihilityRitualRecipe>> NIHILITY_RITUAL_TYPE =
        TYPES.register("nihility_ritual", () -> RecipeType.simple(
            Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_ritual")));

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
        TYPES.register(bus);
    }

    private ModRecipes() {
    }
}
