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
    private static final int BOUNDING_BOX_FORMAT = 2;
    private static final int LEGACY_X_SPAN = 33;
    private static final int LEGACY_Y_SPAN = 17;
    private static final int LEGACY_Z_SPAN = 33;

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
        int format = tag.getInt("BBFormat").orElse(1);
        if (format < BOUNDING_BOX_FORMAT) {
            // beta.2 restored inclusive spans as offsets, so repeatedly loaded pieces grew.
            w = Math.min(w, LEGACY_X_SPAN);
            h = Math.min(h, LEGACY_Y_SPAN);
            d = Math.min(d, LEGACY_Z_SPAN);
        }
        return new BoundingBox(x, y, z, x + Math.max(1, w) - 1,
            y + Math.max(1, h) - 1, z + Math.max(1, d) - 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("templeType", templeType.name());
        tag.putInt("BBx", boundingBox.minX());
        tag.putInt("BBy", boundingBox.minY());
        tag.putInt("BBz", boundingBox.minZ());
        tag.putInt("BBFormat", BOUNDING_BOX_FORMAT);
        tag.putInt("BBw", boundingBox.getXSpan());
        tag.putInt("BBh", boundingBox.getYSpan());
        tag.putInt("BBd", boundingBox.getZSpan());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockPos center = new BlockPos(boundingBox.minX() + 16, boundingBox.minY() + 1, boundingBox.minZ() + 16);
        RandomSource structureRandom = RandomSource.create(center.asLong() ^ ((long) templeType.ordinal() << 48));
        TempleGenerator.generateTemple(level, center, templeType, structureRandom, box);
    }
}
