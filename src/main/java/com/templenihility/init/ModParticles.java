package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, TempleNihilityMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NIHILITY_MOTE =
        PARTICLES.register("nihility_mote", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NIHILITY_RIPPLE =
        PARTICLES.register("nihility_ripple", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NIHILITY_SHARD =
        PARTICLES.register("nihility_shard", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ABYSS_EMBER =
        PARTICLES.register("abyss_ember", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHAOS_GLINT =
        PARTICLES.register("chaos_glint", () -> new SimpleParticleType(false) {});

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }

    private ModParticles() {
    }
}
