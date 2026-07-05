package com.templenihility.world;

import com.templenihility.init.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
public class TemplePiece extends StructurePiece {
    private TempleType templeType;

    // ContextlessType (no serialization context needed)
    public TemplePiece(CompoundTag tag) {
        super(ModStructures.TEMPLE_PIECE.get(), 0, makeBoundingBox(tag));
        this.templeType = TempleType.valueOf(tag.getString("templeType").orElse(TempleType.SHRINE.name()));
    }

    public TemplePiece(TempleType type, BlockPos pos) {
        super(ModStructures.TEMPLE_PIECE.get(), 0,
            new BoundingBox(pos.getX() - 16, pos.getY() - 1, pos.getZ() - 16,
                           pos.getX() + 16, pos.getY() + 15, pos.getZ() + 16));
        this.templeType = type;
    }

    private static BoundingBox makeBoundingBox(CompoundTag tag) {
        int x = tag.getInt("BBx").orElse(0), y = tag.getInt("BBy").orElse(64), z = tag.getInt("BBz").orElse(0);
        int w = tag.getInt("BBw").orElse(32), h = tag.getInt("BBh").orElse(16), d = tag.getInt("BBd").orElse(32);
        return new BoundingBox(x, y, z, x + w, y + h, z + d);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("templeType", templeType.name());
        tag.putInt("BBx", boundingBox.minX());
        tag.putInt("BBy", boundingBox.minY());
        tag.putInt("BBz", boundingBox.minZ());
        tag.putInt("BBw", boundingBox.getXSpan());
        tag.putInt("BBh", boundingBox.getYSpan());
        tag.putInt("BBd", boundingBox.getZSpan());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockPos center = new BlockPos(boundingBox.minX() + 16, boundingBox.minY() + 1, boundingBox.minZ() + 16);
        TempleGenerator.generateTemple(level.getLevel(), center, templeType);
    }
}
