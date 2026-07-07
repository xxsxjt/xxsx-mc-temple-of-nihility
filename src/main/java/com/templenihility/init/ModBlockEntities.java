package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.blockentity.NihilityAltarBlockEntity;
import com.templenihility.blockentity.NihilityEnergyCellBlockEntity;
import com.templenihility.blockentity.NihilityEnergyPrismBlockEntity;
import com.templenihility.blockentity.NihilityTransportCoreBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NihilityAltarBlockEntity>> NIHILITY_ALTAR =
        BLOCK_ENTITIES.register("nihility_altar",
            () -> new BlockEntityType<>(NihilityAltarBlockEntity::new, ModBlocks.NIHILITY_ALTAR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NihilityEnergyPrismBlockEntity>> NIHILITY_ENERGY_PRISM =
        BLOCK_ENTITIES.register("nihility_energy_prism",
            () -> new BlockEntityType<>(NihilityEnergyPrismBlockEntity::new, ModBlocks.NIHILITY_ENERGY_PRISM.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NihilityEnergyCellBlockEntity>> NIHILITY_ENERGY_CELL =
        BLOCK_ENTITIES.register("nihility_energy_cell",
            () -> new BlockEntityType<>(NihilityEnergyCellBlockEntity::new, ModBlocks.NIHILITY_ENERGY_CELL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NihilityTransportCoreBlockEntity>> NIHILITY_TRANSPORT_CORE =
        BLOCK_ENTITIES.register("nihility_transport_core",
            () -> new BlockEntityType<>(NihilityTransportCoreBlockEntity::new, ModBlocks.NIHILITY_TRANSPORT_CORE.get()));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlockEntities() {
    }
}
