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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class TemplePiece extends StructurePiece {
    private static final int BOUNDING_BOX_FORMAT = 3;
    private static final int LEGACY_X_SPAN = 33;
    private static final int LEGACY_Y_SPAN = 17;
    private static final int LEGACY_Z_SPAN = 33;
    private static final int HALF_XZ_SPAN = 18;
    private static final int DEPTH_BELOW = 4;
    private static final int HEIGHT_ABOVE = 26;

    private TempleType templeType;

    public TemplePiece(CompoundTag tag) {
        super(ModStructures.TEMPLE_PIECE.get(), 0, makeBoundingBox(tag));
        this.templeType = TempleType.valueOf(tag.getString("templeType").orElse(TempleType.SHRINE.name()));
    }

    public TemplePiece(TempleType type, BlockPos pos) {
        super(ModStructures.TEMPLE_PIECE.get(), 0,
            new BoundingBox(
                pos.getX() - HALF_XZ_SPAN, pos.getY() - DEPTH_BELOW, pos.getZ() - HALF_XZ_SPAN,
                pos.getX() + HALF_XZ_SPAN, pos.getY() + HEIGHT_ABOVE, pos.getZ() + HALF_XZ_SPAN));
        this.templeType = type;
    }

    private static BoundingBox makeBoundingBox(CompoundTag tag) {
        int x = tag.getInt("BBx").orElse(0);
        int y = tag.getInt("BBy").orElse(64);
        int z = tag.getInt("BBz").orElse(0);
        int w = tag.getInt("BBw").orElse(HALF_XZ_SPAN * 2 + 1);
        int h = tag.getInt("BBh").orElse(DEPTH_BELOW + HEIGHT_ABOVE + 1);
        int d = tag.getInt("BBd").orElse(HALF_XZ_SPAN * 2 + 1);
        int format = tag.getInt("BBFormat").orElse(1);
        if (format < 2) {
            w = Math.min(w, LEGACY_X_SPAN);
            h = Math.min(h, LEGACY_Y_SPAN);
            d = Math.min(d, LEGACY_Z_SPAN);
        }
        return new BoundingBox(
            x, y, z,
            x + Math.max(1, w) - 1,
            y + Math.max(1, h) - 1,
            z + Math.max(1, d) - 1);
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
        int centerX = (boundingBox.minX() + boundingBox.maxX()) / 2;
        int centerZ = (boundingBox.minZ() + boundingBox.maxZ()) / 2;
        int baseY = boundingBox.getYSpan() >= 28 ? boundingBox.minY() + DEPTH_BELOW : boundingBox.minY() + 1;
        BlockPos center = new BlockPos(centerX, baseY, centerZ);
        RandomSource structureRandom = RandomSource.create(center.asLong() ^ ((long) templeType.ordinal() << 48));
        TempleGenerator.generateTemple(level, center, templeType, structureRandom, box);
    }
}
