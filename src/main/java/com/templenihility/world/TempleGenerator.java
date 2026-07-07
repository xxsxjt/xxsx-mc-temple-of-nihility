package com.templenihility.world;

import com.templenihility.entity.*;
import com.templenihility.init.ModBlocks;
import com.templenihility.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class TempleGenerator {
    public static void generateTemple(ServerLevelAccessor level, BlockPos pos, TempleType type) {
        generateTemple(level, pos, type, RandomSource.create(pos.asLong()));
    }

    public static void generateTemple(ServerLevelAccessor level, BlockPos pos, TempleType type, RandomSource random) {
        switch (type) {
            case SHRINE:
                generateShrine(level, pos, random);
                break;
            case TEMPLE:
                generateTempleStructure(level, pos, random);
                break;
            case TEMPLE_COMPLEX:
                generateTempleComplex(level, pos, random);
                break;
        }
    }

    private static void generateShrine(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        // 简单神龛结构
        int size = 4;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                level.setBlock(pos.offset(x, 0, z), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
                if (Math.abs(x) == size || Math.abs(z) == size) {
                    level.setBlock(pos.offset(x, 1, z), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
                }
            }
        }
        // 中心祭坛
        level.setBlock(pos, ModBlocks.NIHILITY_ALTAR.get().defaultBlockState(), 2);
        // 水晶装饰
        level.setBlock(pos.above(2), ModBlocks.NIHILITY_CRYSTAL_BLOCK.get().defaultBlockState(), 2);
        // 生成信徒
        spawnCreature(level, pos.above(1), ModEntities.NIHILITY_BELIEVER.get(), random);
    }

    private static void generateTempleStructure(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        // 中型圣殿结构
        int size = 8;
        // 地基
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                level.setBlock(pos.offset(x, 0, z), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
            }
        }
        // 墙壁
        for (int y = 1; y <= 5; y++) {
            for (int x = -size; x <= size; x++) {
                level.setBlock(pos.offset(x, y, -size), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
                level.setBlock(pos.offset(x, y, size), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
            }
            for (int z = -size; z <= size; z++) {
                level.setBlock(pos.offset(-size, y, z), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
                level.setBlock(pos.offset(size, y, z), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
            }
        }
        // 入口
        for (int y = 1; y <= 3; y++) {
            level.setBlock(pos.offset(0, y, -size), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(pos.offset(1, y, -size), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(pos.offset(-1, y, -size), Blocks.AIR.defaultBlockState(), 2);
        }
        // 中心祭坛
        level.setBlock(pos, ModBlocks.NIHILITY_ALTAR.get().defaultBlockState(), 2);
        level.setBlock(pos.above(3), ModBlocks.NIHILITY_CRYSTAL_BLOCK.get().defaultBlockState(), 2);
        // 生成守卫
        for (int i = 0; i < 2; i++) {
            int x = random.nextInt(size * 2) - size;
            int z = random.nextInt(size * 2) - size;
            spawnCreature(level, pos.offset(x, 1, z), ModEntities.NIHILITY_FOLLOWER.get(), random);
        }
    }

    private static void generateTempleComplex(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        // 大型圣殿群 - 简化版
        int size = 15;
        // 主平台
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                level.setBlock(pos.offset(x, 0, z), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
            }
        }
        // 中心塔
        for (int y = 1; y <= 10; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    level.setBlock(pos.offset(x, y, z), ModBlocks.NIHILITY_STONE.get().defaultBlockState(), 2);
                }
            }
        }
        // 顶部祭坛
        level.setBlock(pos.above(11), ModBlocks.NIHILITY_ALTAR.get().defaultBlockState(), 2);
        // 生成守卫
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(size * 2) - size;
            int z = random.nextInt(size * 2) - size;
            spawnCreature(level, pos.offset(x, 1, z), ModEntities.NIHILITY_ENVOY.get(), random);
        }
        // 生成Boss
        spawnCreature(level, pos.above(12), ModEntities.NIHILITY_SAINT.get(), random);
    }

    private static <T extends Entity> void spawnCreature(ServerLevelAccessor level, BlockPos pos,
                                                         net.minecraft.world.entity.EntityType<T> type,
                                                         RandomSource random) {
        if (!level.isClientSide()) {
            T entity = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (entity != null) {
                entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
                level.addFreshEntityWithPassengers(entity);
            }
        }
    }
}
