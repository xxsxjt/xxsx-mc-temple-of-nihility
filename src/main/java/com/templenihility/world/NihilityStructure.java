package com.templenihility.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.templenihility.init.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import java.util.Optional;

/** 虚无圣殿结构 — 地表生成，三种规模 */
public class NihilityStructure extends Structure {
    public static final MapCodec<NihilityStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(Structure.settingsCodec(instance))
            .apply(instance, NihilityStructure::new));

    public NihilityStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos chunk = ctx.chunkPos();
        BlockPos pos = new BlockPos(
            chunk.getMiddleBlockX(),
            ctx.chunkGenerator().getFirstFreeHeight(
                chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState()),
            chunk.getMiddleBlockZ());

        if (pos.getY() < 60) return Optional.empty();

        final TempleType type;
        int r = ctx.random().nextInt(100);
        if (r < 10) type = TempleType.TEMPLE_COMPLEX;
        else if (r < 35) type = TempleType.TEMPLE;
        else type = TempleType.SHRINE;

        return Optional.of(new GenerationStub(pos, builder ->
            builder.addPiece(new TemplePiece(type, pos))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.NIHILITY_TEMPLE.get();
    }
}
