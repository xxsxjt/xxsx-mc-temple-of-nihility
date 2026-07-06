package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.effect.VoidPhaseEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, TempleNihilityMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> INTO_NIHILITY =
        EFFECTS.register("into_nihility", () -> new VoidPhaseEffect(0x45206F));

    public static final DeferredHolder<MobEffect, MobEffect> MELT_INTO_NIHILITY =
        EFFECTS.register("melt_into_nihility", () -> new VoidPhaseEffect(0x2F8FA3));

    public static final DeferredHolder<MobEffect, MobEffect> HIDDEN_IN_NIHILITY =
        EFFECTS.register("hidden_in_nihility", () -> new VoidPhaseEffect(0x1D2739));

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private ModEffects() {
    }
}
