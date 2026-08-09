package com.templenihility.world;

import com.templenihility.TempleNihilityMod;
import com.templenihility.config.ModConfig;
import com.templenihility.entity.*;
import com.templenihility.init.ModBlocks;
import com.templenihility.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

public class TempleGenerator {
    private static final ResourceKey<LootTable> SHRINE_CACHE_LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "chests/nihility_shrine"));
    private static final ResourceKey<LootTable> TEMPLE_SUPPLIES_LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "chests/nihility_temple_supplies"));
    private static final ResourceKey<LootTable> COMPLEX_RELICS_LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "chests/nihility_complex_relics"));

    public static void generateTemple(ServerLevelAccessor level, BlockPos pos, TempleType type) {
        generateTemple(level, pos, type, RandomSource.create(pos.asLong()));
    }

    public static void generateTemple(ServerLevelAccessor level, BlockPos pos, TempleType type, RandomSource random) {
        generateTemple(level, pos, type, random, null);
    }

    public static void generateTemple(ServerLevelAccessor level, BlockPos pos, TempleType type, RandomSource random, BoundingBox placementBounds) {
        switch (type) {
            case SHRINE:
                generateShrine(level, pos, random, placementBounds);
                break;
            case TEMPLE:
                generateTempleStructure(level, pos, random, placementBounds);
                break;
            case TEMPLE_COMPLEX:
                generateTempleComplex(level, pos, random, placementBounds);
                break;
        }
    }

    private static void generateShrine(ServerLevelAccessor level, BlockPos pos, RandomSource random, BoundingBox placementBounds) {
        int size = 4;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                boolean outer = Math.abs(x) == size || Math.abs(z) == size;
                BlockState floor = outer
                    ? ModBlocks.CRACKED_NIHILITY_BRICKS.get().defaultBlockState()
                    : ((x + z) & 1) == 0
                        ? ModBlocks.NIHILITY_TILES.get().defaultBlockState()
                        : ModBlocks.NIHILITY_STONE.get().defaultBlockState();
                placeBlock(level, placementBounds, pos.offset(x, 0, z), floor);
            }
        }

        for (int z = -7; z < -size; z++) {
            for (int x = -1; x <= 1; x++) {
                placeBlock(level, placementBounds, pos.offset(x, 0, z),
                    x == 0 ? ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState()
                        : ModBlocks.NIHILITY_BRICKS.get().defaultBlockState());
            }
        }

        for (int dx : new int[] {-4, 4}) {
            for (int dz : new int[] {-4, 4}) {
                placeBlock(level, placementBounds, pos.offset(dx, 1, dz), ModBlocks.NIHILITY_PILLAR.get().defaultBlockState());
                placeBlock(level, placementBounds, pos.offset(dx, 2, dz), ModBlocks.NIHILITY_PILLAR.get().defaultBlockState());
                placeBlock(level, placementBounds, pos.offset(dx, 3, dz), ModBlocks.NIHILITY_LAMP.get().defaultBlockState());
            }
        }

        placeBlock(level, placementBounds, pos, ModBlocks.NIHILITY_ALTAR.get().defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            placeBlock(level, placementBounds, pos.relative(direction), ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState());
        }
        placeBlock(level, placementBounds, pos.above(3), ModBlocks.NIHILITY_CRYSTAL_BLOCK.get().defaultBlockState());
        placeLootBarrel(level, placementBounds, pos.offset(3, 1, 0), Direction.WEST, SHRINE_CACHE_LOOT, random);

        spawnCreature(level, placementBounds, pos.offset(-2, 1, 2), pos,
            CreatureDispositionPolicy.homeRadius(1), ModEntities.NIHILITY_BELIEVER.get(), random,
            ModConfig.BELIEVER_SPAWN_CHANCE.get());
    }

    private static void generateTempleStructure(ServerLevelAccessor level, BlockPos pos, RandomSource random, BoundingBox placementBounds) {
        int size = TempleLayoutPolicy.RADIUS;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                BlockState floor = switch (TempleLayoutPolicy.floorZone(x, z)) {
                    case PERIMETER -> ModBlocks.CRACKED_NIHILITY_BRICKS.get().defaultBlockState();
                    case AISLE -> ModBlocks.NIHILITY_TILES.get().defaultBlockState();
                    case TRADE_WING -> ModBlocks.NIHILITY_BRICKS.get().defaultBlockState();
                    case ARCHIVE_WING -> ModBlocks.CHISELED_NIHILITY_STONE.get().defaultBlockState();
                    case RITUAL_HALL -> ((x + z) & 1) == 0
                        ? ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState()
                        : ModBlocks.NIHILITY_STONE.get().defaultBlockState();
                };
                placeBlock(level, placementBounds, pos.offset(x, 0, z), floor);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    if (TempleLayoutPolicy.isOuterWall(x, z, y)) {
                        boolean window = (y == 2 || y == 3)
                            && ((Math.abs(x) == size && Math.floorMod(z, 4) == 0)
                                || (Math.abs(z) == size && Math.floorMod(x, 4) == 0));
                        placeBlock(level, placementBounds, pos.offset(x, y, z),
                            window ? ModBlocks.NIHILITY_GLASS.get().defaultBlockState()
                                : ModBlocks.NIHILITY_BRICKS.get().defaultBlockState());
                    } else if (TempleLayoutPolicy.isInternalWall(x, z, y)) {
                        placeBlock(level, placementBounds, pos.offset(x, y, z),
                            y == 2 ? ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState()
                                : ModBlocks.NIHILITY_BRICKS.get().defaultBlockState());
                    }
                }
            }
        }

        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                if (Math.abs(x) == size || Math.abs(z) == size || z == 2 && Math.abs(x) <= 7) {
                    placeBlock(level, placementBounds, pos.offset(x, 5, z), ModBlocks.NIHILITY_TILES.get().defaultBlockState());
                }
            }
        }

        for (int x : new int[] {-4, 4}) {
            for (int z : new int[] {-6, 0, 6}) {
                placeBlock(level, placementBounds, pos.offset(x, 1, z), ModBlocks.NIHILITY_PILLAR.get().defaultBlockState());
                placeBlock(level, placementBounds, pos.offset(x, 2, z), ModBlocks.NIHILITY_PILLAR.get().defaultBlockState());
                placeBlock(level, placementBounds, pos.offset(x, 3, z), ModBlocks.NIHILITY_LAMP.get().defaultBlockState());
            }
        }

        BlockPos ritualCenter = pos.offset(0, 0, 5);
        placeBlock(level, placementBounds, ritualCenter, ModBlocks.NIHILITY_ALTAR.get().defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            placeBlock(level, placementBounds, ritualCenter.relative(direction),
                ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState());
        }
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                placeBlock(level, placementBounds, ritualCenter.offset(dx, 0, dz),
                    ModBlocks.NIHILITY_CRYSTAL_BLOCK.get().defaultBlockState());
            }
        }
        placeBlock(level, placementBounds, ritualCenter.above(4), ModBlocks.NIHILITY_LAMP.get().defaultBlockState());

        placeLootBarrel(level, placementBounds, pos.offset(5, 1, -1), Direction.WEST,
            TEMPLE_SUPPLIES_LOOT, random);

        spawnCreature(level, placementBounds, pos.offset(-5, 1, -1), pos,
            CreatureDispositionPolicy.homeRadius(1), ModEntities.NIHILITY_BELIEVER.get(), random,
            ModConfig.BELIEVER_SPAWN_CHANCE.get());
        spawnCreature(level, placementBounds, pos.offset(0, 1, -5), pos,
            CreatureDispositionPolicy.homeRadius(2), ModEntities.NIHILITY_FOLLOWER.get(), random,
            ModConfig.FOLLOWER_SPAWN_CHANCE.get());
        spawnCreature(level, placementBounds, pos.offset(3, 1, 4), pos,
            CreatureDispositionPolicy.homeRadius(2), ModEntities.NIHILITY_FOLLOWER.get(), random,
            ModConfig.FOLLOWER_SPAWN_CHANCE.get());
        spawnCreature(level, placementBounds, pos.offset(0, 1, 6), ritualCenter,
            CreatureDispositionPolicy.homeRadius(3), ModEntities.NIHILITY_ENVOY.get(), random,
            ModConfig.ENVOY_SPAWN_CHANCE.get());
    }

    private static void generateTempleComplex(ServerLevelAccessor level, BlockPos pos, RandomSource random, BoundingBox placementBounds) {
        int size = TempleComplexLayoutPolicy.RADIUS;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                TempleComplexLayoutPolicy.Zone zone = TempleComplexLayoutPolicy.zone(x, z);
                if (zone == TempleComplexLayoutPolicy.Zone.NONE) {
                    continue;
                }
                BlockState floor = switch (zone) {
                    case CONNECTOR -> (x == 0 || z == 0)
                        ? ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState()
                        : ModBlocks.NIHILITY_BRICKS.get().defaultBlockState();
                    case SANCTUM -> ((x + z) & 1) == 0
                        ? ModBlocks.NIHILITY_TILES.get().defaultBlockState()
                        : ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState();
                    case VAULT_WING -> ModBlocks.CHISELED_NIHILITY_STONE.get().defaultBlockState();
                    case ENERGY_WING -> ModBlocks.NIHILITY_BRICKS.get().defaultBlockState();
                    case CHALLENGE_COURT -> ((x + z) & 1) == 0
                        ? ModBlocks.CRACKED_NIHILITY_BRICKS.get().defaultBlockState()
                        : ModBlocks.NIHILITY_STONE.get().defaultBlockState();
                    case NONE -> ModBlocks.NIHILITY_STONE.get().defaultBlockState();
                };
                placeBlock(level, placementBounds, pos.offset(x, 0, z), floor);
                if (TempleComplexLayoutPolicy.isRoomEdge(zone, x, z)
                    && !TempleComplexLayoutPolicy.isDoor(zone, x, z)) {
                    placeBlock(level, placementBounds, pos.offset(x, 1, z),
                        ((x + z) & 1) == 0 ? ModBlocks.NIHILITY_BRICKS.get().defaultBlockState()
                            : ModBlocks.CRACKED_NIHILITY_BRICKS.get().defaultBlockState());
                }
            }
        }

        int[][] pillars = {
            {-4, -4}, {-4, 4}, {4, -4}, {4, 4},
            {-14, -4}, {-14, 4}, {-6, -4}, {-6, 4},
            {6, -4}, {6, 4}, {14, -4}, {14, 4},
            {-4, 7}, {4, 7}, {-4, 15}, {4, 15}
        };
        for (int[] pillar : pillars) {
            buildComplexPillar(level, placementBounds, pos.offset(pillar[0], 0, pillar[1]));
        }

        BlockPos ritualCenter = pos;
        placeBlock(level, placementBounds, ritualCenter, ModBlocks.NIHILITY_ALTAR.get().defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            placeBlock(level, placementBounds, ritualCenter.relative(direction),
                ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState());
            placeBlock(level, placementBounds, ritualCenter.relative(direction, 2),
                ModBlocks.NIHILITY_PILLAR.get().defaultBlockState());
        }
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                placeBlock(level, placementBounds, ritualCenter.offset(dx, 0, dz),
                    ModBlocks.NIHILITY_CRYSTAL_BLOCK.get().defaultBlockState());
            }
        }
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                placeBlock(level, placementBounds, ritualCenter.offset(dx, 0, dz),
                    ModBlocks.CHISELED_NIHILITY_STONE.get().defaultBlockState());
            }
        }

        placeBlock(level, placementBounds, pos.offset(-10, 1, 0), ModBlocks.NIHILITY_VAULT.get().defaultBlockState());
        placeBlock(level, placementBounds, pos.offset(9, 1, 0), ModBlocks.NIHILITY_ENERGY_PRISM.get().defaultBlockState());
        placeBlock(level, placementBounds, pos.offset(10, 1, 0), ModBlocks.NIHILITY_TRANSPORT_CORE.get().defaultBlockState());
        placeBlock(level, placementBounds, pos.offset(11, 1, 0), ModBlocks.NIHILITY_ENERGY_CELL.get().defaultBlockState());
        placeLootBarrel(level, placementBounds, pos.offset(3, 1, 11), Direction.WEST,
            COMPLEX_RELICS_LOOT, random);

        spawnCreature(level, placementBounds, pos.offset(0, 1, -10), pos,
            CreatureDispositionPolicy.homeRadius(2), ModEntities.NIHILITY_FOLLOWER.get(), random,
            ModConfig.FOLLOWER_SPAWN_CHANCE.get());
        spawnCreature(level, placementBounds, pos.offset(-10, 1, 2), pos.offset(-10, 0, 0),
            CreatureDispositionPolicy.homeRadius(3), ModEntities.NIHILITY_ENVOY.get(), random,
            ModConfig.ENVOY_SPAWN_CHANCE.get());
        spawnCreature(level, placementBounds, pos.offset(10, 1, 2), pos.offset(10, 0, 0),
            CreatureDispositionPolicy.homeRadius(3), ModEntities.NIHILITY_ENVOY.get(), random,
            ModConfig.ENVOY_SPAWN_CHANCE.get());
        spawnCreature(level, placementBounds, pos.offset(0, 1, 2), pos,
            CreatureDispositionPolicy.homeRadius(4), ModEntities.NIHILITY_GRAND_ENVOY.get(), random,
            ModConfig.GRAND_ENVOY_SPAWN_CHANCE.get());
        spawnCreature(level, placementBounds, pos.offset(0, 1, 11), pos.offset(0, 0, 11),
            CreatureDispositionPolicy.homeRadius(5), ModEntities.NIHILITY_SAINT.get(), random,
            ModConfig.SAINT_SPAWN_CHANCE.get());
    }

    private static void buildComplexPillar(ServerLevelAccessor level, BoundingBox placementBounds, BlockPos base) {
        for (int y = 1; y <= 4; y++) {
            placeBlock(level, placementBounds, base.above(y), ModBlocks.NIHILITY_PILLAR.get().defaultBlockState());
        }
        placeBlock(level, placementBounds, base.above(5), ModBlocks.NIHILITY_LAMP.get().defaultBlockState());
    }

    private static void placeLootBarrel(ServerLevelAccessor level, BoundingBox placementBounds,
                                        BlockPos pos, Direction facing, ResourceKey<LootTable> lootTable,
                                        RandomSource random) {
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing);
        placeBlock(level, placementBounds, pos, barrel);
        if (isInside(placementBounds, pos)
            && level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(lootTable);
            container.setLootTableSeed(random.nextLong());
        }
    }

    private static void placeBlock(ServerLevelAccessor level, BoundingBox placementBounds, BlockPos pos, BlockState state) {
        if (isInside(placementBounds, pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    private static boolean isInside(BoundingBox placementBounds, BlockPos pos) {
        return placementBounds == null
            || (pos.getX() >= placementBounds.minX() && pos.getX() <= placementBounds.maxX()
                && pos.getY() >= placementBounds.minY() && pos.getY() <= placementBounds.maxY()
                && pos.getZ() >= placementBounds.minZ() && pos.getZ() <= placementBounds.maxZ());
    }

    private static <T extends Entity> void spawnCreature(ServerLevelAccessor level, BoundingBox placementBounds,
                                                           BlockPos pos, BlockPos home, int homeRadius,
                                                           net.minecraft.world.entity.EntityType<T> type,
                                                           RandomSource random, int spawnChance) {
        int chanceRoll = random.nextInt(100);
        float yaw = random.nextFloat() * 360.0F;
        if (chanceRoll >= spawnChance || !isInside(placementBounds, pos)) {
            return;
        }
        if (!level.isClientSide()) {
            T entity = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (entity != null) {
                entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
                if (entity instanceof PathfinderMob mob) {
                    mob.setHomeTo(home, homeRadius);
                    mob.setPersistenceRequired();
                }
                level.addFreshEntityWithPassengers(entity);
            }
        }
    }
}
