package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.world.NihilityStructure;
import com.templenihility.world.TemplePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, TempleNihilityMod.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, TempleNihilityMod.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<NihilityStructure>> NIHILITY_TEMPLE =
        STRUCTURES.register("nihility_temple", () -> () -> NihilityStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TEMPLE_PIECE =
        PIECES.register("temple_piece", () -> (StructurePieceType.ContextlessType) TemplePiece::new);

    public static void register(IEventBus bus) {
        STRUCTURES.register(bus);
        PIECES.register(bus);
    }
}
