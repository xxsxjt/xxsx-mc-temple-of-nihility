package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.blockentity.NihilityVaultBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TempleNihilityMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NihilityVaultBlockEntity>> NIHILITY_VAULT =
        BLOCK_ENTITIES.register("nihility_vault",
            () -> new BlockEntityType<>(NihilityVaultBlockEntity::new, ModBlocks.NIHILITY_VAULT.get()));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlockEntities() {
    }
}
