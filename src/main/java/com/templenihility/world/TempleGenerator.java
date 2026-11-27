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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 虚无圣殿三阶建筑生成器 — 暗黑黑曜石哥特星盘圣殿架构。
 * 保持与 TempleLayoutPolicy / TempleComplexLayoutPolicy 的功能分区和门洞完全一致，
 * 并通过高耸尖塔、挑檐穹顶、悬垂灯柱、雕花扶壁与浮空星环建立鲜明剪影。
 */
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

    public static void generateTemple(ServerLevelAccessor level, BlockPos pos, TempleType type,
                                      RandomSource random, BoundingBox placementBounds) {
        switch (type) {
            case SHRINE -> generateShrine(level, pos, random, placementBounds);
            case TEMPLE -> generateTempleStructure(level, pos, random, placementBounds);
            case TEMPLE_COMPLEX -> generateTempleComplex(level, pos, random, placementBounds);
        }
    }

    // ==================== TIER 1: SHRINE (虚无祭坛祠) ====================
    private static void generateShrine(ServerLevelAccessor level, BlockPos pos, RandomSource random, BoundingBox bounds) {
        int size = 4;
        clearVolume(level, bounds, pos, -6, 6, 1, 13, -8, 6);

        // Stepped plinth foundation (y=-2..0)
        for (int x = -size - 1; x <= size + 1; x++) {
            for (int z = -size - 1; z <= size + 1; z++) {
                fillFoundationColumn(level, bounds, pos.offset(x, -1, z), 3);
                boolean skirt = Math.abs(x) == size + 1 || Math.abs(z) == size + 1;
                if (skirt) {
                    placeBlock(level, bounds, pos.offset(x, 0, z), slab(Blocks.DEEPSLATE_TILE_SLAB));
                }
            }
        }

        // Ceremonial inlaid floor
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                boolean outer = Math.abs(x) == size || Math.abs(z) == size;
                BlockState floor = outer
                    ? (((x + z) & 1) == 0 ? bricks() : chiseled())
                    : (Math.abs(x) <= 1 && Math.abs(z) <= 1)
                        ? runeBricks()
                        : (((x + z) & 1) == 0 ? tiles() : stone());
                placeBlock(level, bounds, pos.offset(x, 0, z), floor);
            }
        }

        // South ceremonial approach avenue with low braziers
        for (int z = -8; z < -size; z++) {
            for (int x = -2; x <= 2; x++) {
                fillFoundationColumn(level, bounds, pos.offset(x, -1, z), 3);
                if (Math.abs(x) <= 1) {
                    placeBlock(level, bounds, pos.offset(x, 0, z), x == 0 ? runeBricks() : bricks());
                } else if (z == -7) {
                    placeBlock(level, bounds, pos.offset(x, 0, z), chiseled());
                    placeBlock(level, bounds, pos.offset(x, 1, z), pillar());
                    placeBlock(level, bounds, pos.offset(x, 2, z), Blocks.SOUL_LANTERN.defaultBlockState());
                }
            }
        }

        // 4 Sculpted Corner Monolith Pillars (height 6) + Overhanging Arched Ribs
        for (int dx : new int[] {-4, 4}) {
            for (int dz : new int[] {-4, 4}) {
                buildSculptedPillar(level, bounds, pos.offset(dx, 0, dz), 6, true);
                // Corner buttress walls
                placeBlock(level, bounds, pos.offset(dx + (dx < 0 ? -1 : 1), 1, dz), blackstoneWall());
                placeBlock(level, bounds, pos.offset(dx, 1, dz + (dz < 0 ? -1 : 1)), blackstoneWall());
                // Hanging corner lantern inside
                placeBlock(level, bounds, pos.offset(dx + (dx < 0 ? 1 : -1), 5, dz + (dz < 0 ? 1 : -1)),
                    hangingLantern());
            }
        }

        // Overhead arched canopy connecting the 4 corner monoliths (y=6..8)
        for (int i = -3; i <= 3; i++) {
            int archY = Math.abs(i) <= 1 ? 7 : 6;
            BlockState archMat = Math.abs(i) == 0 ? gilded() : bricks();
            placeBlock(level, bounds, pos.offset(i, archY, -4), archMat);
            placeBlock(level, bounds, pos.offset(i, archY, 4), archMat);
            placeBlock(level, bounds, pos.offset(-4, archY, i), archMat);
            placeBlock(level, bounds, pos.offset(4, archY, i), archMat);
            if (Math.abs(i) <= 2) {
                placeBlock(level, bounds, pos.offset(i, 7, 0), tiles());
                placeBlock(level, bounds, pos.offset(0, 7, i), tiles());
            }
        }

        // Central Floating Crystal Spire Crown (y=4..11)
        placeBlock(level, bounds, pos, altar());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            placeBlock(level, bounds, pos.relative(d), runeBricks());
            placeBlock(level, bounds, pos.relative(d, 2).above(1), blackstoneWall());
        }
        placeBlock(level, bounds, pos.above(4), crystal());
        placeBlock(level, bounds, pos.above(5), crystal());
        placeBlock(level, bounds, pos.above(6), lamp());
        placeBlock(level, bounds, pos.above(7), gilded());
        placeBlock(level, bounds, pos.above(8), pillar());
        placeBlock(level, bounds, pos.above(9), crystal());
        placeBlock(level, bounds, pos.above(10), blackstoneWall());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            placeBlock(level, bounds, pos.relative(d, 2).above(8), cryingObsidian());
            placeBlock(level, bounds, pos.relative(d, 1).above(7), stairs(d.getOpposite(), Half.BOTTOM));
        }

        placeLootBarrel(level, bounds, pos.offset(3, 1, 0), Direction.WEST, SHRINE_CACHE_LOOT, random);
        spawnCreature(level, bounds, pos.offset(-2, 1, 2), pos,
            CreatureDispositionPolicy.homeRadius(1), ModEntities.NIHILITY_BELIEVER.get(), random,
            ModConfig.BELIEVER_SPAWN_CHANCE.get());
    }

    // ==================== TIER 2: TEMPLE (虚无圣殿主殿) ====================
    private static void generateTempleStructure(ServerLevelAccessor level, BlockPos pos, RandomSource random, BoundingBox bounds) {
        int size = TempleLayoutPolicy.RADIUS; // 8
        clearVolume(level, bounds, pos, -size - 2, size + 2, 1, 19, -size - 3, size + 2);

        // Grounded foundation & inlaid floor
        for (int x = -size - 1; x <= size + 1; x++) {
            for (int z = -size - 1; z <= size + 1; z++) {
                fillFoundationColumn(level, bounds, pos.offset(x, -1, z), 3);
                if (Math.abs(x) > size || Math.abs(z) > size) {
                    placeBlock(level, bounds, pos.offset(x, 0, z), slab(Blocks.DEEPSLATE_TILE_SLAB));
                }
            }
        }
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                BlockState floor = switch (TempleLayoutPolicy.floorZone(x, z)) {
                    case PERIMETER -> (((x + z) & 1) == 0 ? bricks() : crackedBricks());
                    case AISLE -> x == 0 ? runeBricks() : tiles();
                    case TRADE_WING -> (((x + z) & 1) == 0 ? bricks() : tiles());
                    case ARCHIVE_WING -> chiseled();
                    case RITUAL_HALL -> ((x + z) & 1) == 0 ? runeBricks() : stone();
                };
                placeBlock(level, bounds, pos.offset(x, 0, z), floor);
            }
        }

        // 6-block high Cathedral Outer Walls + Lancet Windows + Gilded Cornice
        for (int y = 1; y <= 6; y++) {
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    boolean onPerimeter = Math.abs(x) == size || Math.abs(z) == size;
                    boolean entranceGap = z == -size && Math.abs(x) <= 1 && y <= 4;
                    if (onPerimeter && !entranceGap) {
                        boolean corner = Math.abs(x) == size && Math.abs(z) == size;
                        boolean buttress = (Math.abs(x) == size && Math.floorMod(z, 4) == 0)
                            || (Math.abs(z) == size && Math.floorMod(x, 4) == 0);
                        boolean lancet = (y >= 2 && y <= 4) && !corner && !buttress
                            && ((Math.abs(x) == size && Math.floorMod(z + 2, 4) == 0)
                                || (Math.abs(z) == size && Math.floorMod(x + 2, 4) == 0));
                        BlockState wallMat;
                        if (corner) {
                            wallMat = (y == 1 || y == 6) ? gilded() : pillar();
                        } else if (lancet) {
                            wallMat = glass();
                        } else if (y == 1) {
                            wallMat = chiseled();
                        } else if (y == 6) {
                            wallMat = gilded();
                        } else if (buttress) {
                            wallMat = pillar();
                        } else {
                            wallMat = (y == 3 && ((x + z) & 1) == 0) ? runeBricks() : bricks();
                        }
                        placeBlock(level, bounds, pos.offset(x, y, z), wallMat);
                    } else if (y <= 4 && TempleLayoutPolicy.isInternalWall(x, z, Math.min(y, 3))) {
                        placeBlock(level, bounds, pos.offset(x, y, z),
                            y == 2 ? runeBricks() : (y == 4 ? chiseled() : bricks()));
                    }
                }
            }
        }

        // Exterior Gothic Flying Buttresses on all 4 facades
        for (int i = -size; i <= size; i += 4) {
            for (int y = 1; y <= 5; y++) {
                BlockState mat = (y == 5) ? stairs(Direction.EAST, Half.BOTTOM) : chiseled();
                placeBlock(level, bounds, pos.offset(-size - 1, y, i), y <= 4 ? (y == 1 ? gilded() : pillar()) : mat);
                placeBlock(level, bounds, pos.offset(size + 1, y, i),
                    y <= 4 ? (y == 1 ? gilded() : pillar()) : stairs(Direction.WEST, Half.BOTTOM));
                if (Math.abs(i) >= 4) {
                    placeBlock(level, bounds, pos.offset(i, y, -size - 1),
                        y <= 4 ? (y == 1 ? gilded() : pillar()) : stairs(Direction.SOUTH, Half.BOTTOM));
                    placeBlock(level, bounds, pos.offset(i, y, size + 1),
                        y <= 4 ? (y == 1 ? gilded() : pillar()) : stairs(Direction.NORTH, Half.BOTTOM));
                }
            }
        }

        // Monumental South Triumphal Portal Arch (z=-8..-10)
        buildSouthPortalArch(level, bounds, pos.offset(0, 0, -size), 9);

        // Stepped Cathedral Pitched Roof (y=7..10) running along Z-axis
        for (int step = 0; step <= 4; step++) {
            int roofY = 7 + step;
            int minX = -size - 1 + step * 2;
            int maxX = size + 1 - step * 2;
            for (int z = -size - 1; z <= size + 1; z++) {
                if (step < 4) {
                    placeBlock(level, bounds, pos.offset(minX, roofY, z), stairs(Direction.EAST, Half.BOTTOM));
                    placeBlock(level, bounds, pos.offset(minX + 1, roofY, z), tiles());
                    placeBlock(level, bounds, pos.offset(maxX, roofY, z), stairs(Direction.WEST, Half.BOTTOM));
                    placeBlock(level, bounds, pos.offset(maxX - 1, roofY, z), tiles());
                } else {
                    for (int rx = -1; rx <= 1; rx++) {
                        placeBlock(level, bounds, pos.offset(rx, roofY, z),
                            rx == 0 ? gilded() : tiles());
                    }
                }
            }
        }

        // Soaring Sanctum Spire Crown above Ritual Hall Altar (0, 0, 5)
        buildFloatingAstralSpire(level, bounds, pos.offset(0, 0, 5), 11, 7);

        // Interior sculpted pillars & chandeliers
        for (int x : new int[] {-4, 4}) {
            for (int z : new int[] {-5, 0, 5}) {
                buildSculptedPillar(level, bounds, pos.offset(x, 0, z), 5, true);
            }
        }
        for (int z : new int[] {-4, 0, 4}) {
            placeBlock(level, bounds, pos.offset(0, 9, z), Blocks.IRON_CHAIN.defaultBlockState());
            placeBlock(level, bounds, pos.offset(0, 8, z), hangingLantern());
        }

        // Ritual Hall Altar & functional blocks (preserving exact coordinates/roles)
        BlockPos ritualCenter = pos.offset(0, 0, 5);
        placeBlock(level, bounds, ritualCenter, altar());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            placeBlock(level, bounds, ritualCenter.relative(d), runeBricks());
        }
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                placeBlock(level, bounds, ritualCenter.offset(dx, 0, dz), crystal());
            }
        }
        placeBlock(level, bounds, pos.offset(6, 1, -1), ModBlocks.NIHILITY_VAULT.get().defaultBlockState());
        placeLootBarrel(level, bounds, pos.offset(6, 1, 1), Direction.WEST, TEMPLE_SUPPLIES_LOOT, random);

        spawnCreature(level, bounds, pos.offset(-5, 1, -1), pos.offset(-5, 0, -1),
            CreatureDispositionPolicy.homeRadius(1), ModEntities.NIHILITY_BELIEVER.get(), random,
            ModConfig.BELIEVER_SPAWN_CHANCE.get());
        spawnCreature(level, bounds, pos.offset(0, 1, -3), pos,
            CreatureDispositionPolicy.homeRadius(2), ModEntities.NIHILITY_FOLLOWER.get(), random,
            ModConfig.FOLLOWER_SPAWN_CHANCE.get());
        spawnCreature(level, bounds, pos.offset(0, 1, 6), ritualCenter,
            CreatureDispositionPolicy.homeRadius(3), ModEntities.NIHILITY_ENVOY.get(), random,
            ModConfig.ENVOY_SPAWN_CHANCE.get());
    }

    // ==================== TIER 3: TEMPLE COMPLEX (虚无圣殿圣域建筑群) ====================
    private static void generateTempleComplex(ServerLevelAccessor level, BlockPos pos, RandomSource random, BoundingBox bounds) {
        int size = TempleComplexLayoutPolicy.RADIUS; // 15
        clearVolume(level, bounds, pos, -size - 2, size + 2, 1, 24, -size - 2, size + 2);

        // 1. Grounded foundation & inlaid floors for every policy zone
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                TempleComplexLayoutPolicy.Zone zone = TempleComplexLayoutPolicy.zone(x, z);
                if (zone == TempleComplexLayoutPolicy.Zone.NONE) {
                    continue;
                }
                fillFoundationColumn(level, bounds, pos.offset(x, -1, z), 3);
                BlockState floor = switch (zone) {
                    case CONNECTOR -> (x == 0 || z == 0) ? runeBricks() : tiles();
                    case SANCTUM -> ((x + z) & 1) == 0 ? tiles() : runeBricks();
                    case VAULT_WING -> ((x + z) & 1) == 0 ? chiseled() : tiles();
                    case ENERGY_WING -> ((x + z) & 1) == 0 ? bricks() : runeBricks();
                    case CHALLENGE_COURT -> (Math.abs(x) <= 1 && Math.abs(z - 11) <= 1)
                        ? runeBricks()
                        : (((x + z) & 1) == 0 ? tiles() : stone());
                    case NONE -> stone();
                };
                placeBlock(level, bounds, pos.offset(x, 0, z), floor);

                // Architectural room walls (replacing 1-block curbs with multi-story cathedral masonry!)
                if (TempleComplexLayoutPolicy.isRoomEdge(zone, x, z)) {
                    boolean door = TempleComplexLayoutPolicy.isDoor(zone, x, z);
                    if (zone == TempleComplexLayoutPolicy.Zone.CHALLENGE_COURT) {
                        // Open-air grand Colosseum balustrade with gilded merlons
                        if (!door) {
                            placeBlock(level, bounds, pos.offset(x, 1, z), chiseled());
                            placeBlock(level, bounds, pos.offset(x, 2, z),
                                ((x + z) & 1) == 0 ? blackstoneWall() : slab(Blocks.DEEPSLATE_TILE_SLAB));
                        }
                    } else {
                        int wallHeight = (zone == TempleComplexLayoutPolicy.Zone.SANCTUM) ? 7 : 5;
                        for (int y = 1; y <= wallHeight; y++) {
                            if (door && y <= 3) {
                                continue; // Preserve exact 3-wide × 3-tall connector doorway
                            }
                            boolean lintel = door && y == 4;
                            boolean window = !door && y >= 2 && y <= Math.min(4, wallHeight - 1)
                                && ((Math.abs(x) == 10 || Math.abs(x) == 0 || Math.abs(z) == 0));
                            BlockState mat;
                            if (lintel) {
                                mat = (x == 0 || z == 0) ? crystal() : gilded();
                            } else if (window) {
                                mat = glass();
                            } else if (y == 1) {
                                mat = chiseled();
                            } else if (y == wallHeight) {
                                mat = gilded();
                            } else {
                                mat = (y == 3 && ((x + z) & 1) == 0) ? runeBricks() : bricks();
                            }
                            placeBlock(level, bounds, pos.offset(x, y, z), mat);
                        }
                    }
                }
            }
        }

        // 2. Monumental South Processional Avenue & Triumphal Entry Gate (z=-15..-5)
        buildSouthPortalArch(level, bounds, pos.offset(0, 0, -14), 8);
        for (int z = -12; z <= -6; z += 3) {
            for (int side : new int[] {-2, 2}) {
                fillFoundationColumn(level, bounds, pos.offset(side, -1, z), 3);
                placeBlock(level, bounds, pos.offset(side, 0, z), chiseled());
                buildSculptedPillar(level, bounds, pos.offset(side, 0, z), 4, true);
            }
            // Overhead processional arch rib
            for (int ax = -2; ax <= 2; ax++) {
                placeBlock(level, bounds, pos.offset(ax, 5, z), ax == 0 ? gilded() : bricks());
            }
            placeBlock(level, bounds, pos.offset(0, 4, z), hangingLantern());
        }

        // 3. Central Sanctum Cathedral Dome (y=8..13) + 22-Block Floating Astral Spire (y=14..22)
        for (int[] corner : new int[][] {{-4, -4}, {-4, 4}, {4, -4}, {4, 4}}) {
            buildSculptedPillar(level, bounds, pos.offset(corner[0], 0, corner[1]), 9, true);
            placeBlock(level, bounds, pos.offset(corner[0], 10, corner[1]), crystal());
            placeBlock(level, bounds, pos.offset(corner[0], 11, corner[1]), blackstoneWall());
        }
        // Stepped pyramid/octagonal Sanctum Dome (y=8..12)
        for (int tier = 0; tier <= 4; tier++) {
            int r = 5 - tier;
            int y = 8 + tier;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                    boolean cutCorner = Math.abs(dx) == r && Math.abs(dz) == r && r >= 3;
                    if (edge && !cutCorner) {
                        BlockState domeMat = (tier == 4 || Math.abs(dx) == 0 || Math.abs(dz) == 0)
                            ? gilded()
                            : (tier % 2 == 0 ? tiles() : bricks());
                        placeBlock(level, bounds, pos.offset(dx, y, dz), domeMat);
                    }
                }
            }
        }
        // Floating Astral Ring & Crystal Spire soaring above the Central Sanctum (y=13..22)
        buildFloatingAstralSpire(level, bounds, pos, 13, 9);
        // Hanging grand Sanctum chandelier
        for (int y = 10; y >= 7; y--) {
            placeBlock(level, bounds, pos.above(y), Blocks.IRON_CHAIN.defaultBlockState());
        }
        placeBlock(level, bounds, pos.above(6), lamp());

        // 4. Vaulted Cathedral Roofs over West Vault Wing (-14..-6) & East Energy Wing (6..14)
        buildWingRoof(level, bounds, pos, -14, -6, -4, 4, 6);
        buildWingRoof(level, bounds, pos, 6, 14, -4, 4, 6);
        for (int[] corner : new int[][] {{-14, -4}, {-14, 4}, {-6, -4}, {-6, 4}, {6, -4}, {6, 4}, {14, -4}, {14, 4}}) {
            buildSculptedPillar(level, bounds, pos.offset(corner[0], 0, corner[1]), 6, true);
        }

        // Covered bridge archways on East/West & North Connectors
        for (int cx : new int[] {-5, 5}) {
            for (int cz = -2; cz <= 2; cz++) {
                if (Math.abs(cz) == 2) {
                    fillFoundationColumn(level, bounds, pos.offset(cx, -1, cz), 3);
                    placeBlock(level, bounds, pos.offset(cx, 0, cz), chiseled());
                    placeBlock(level, bounds, pos.offset(cx, 1, cz), blackstoneWall());
                    placeBlock(level, bounds, pos.offset(cx, 4, cz), bricks());
                } else {
                    placeBlock(level, bounds, pos.offset(cx, 5, cz), cz == 0 ? gilded() : tiles());
                }
            }
            placeBlock(level, bounds, pos.offset(cx, 4, 0), hangingLantern());
        }
        for (int cz : new int[] {5, 6}) {
            for (int cx = -2; cx <= 2; cx++) {
                if (Math.abs(cx) == 2) {
                    fillFoundationColumn(level, bounds, pos.offset(cx, -1, cz), 3);
                    placeBlock(level, bounds, pos.offset(cx, 0, cz), chiseled());
                    placeBlock(level, bounds, pos.offset(cx, 1, cz), blackstoneWall());
                }
            }
            placeBlock(level, bounds, pos.offset(-2, 2, cz), cz == 5 ? pillar() : blackstoneWall());
            placeBlock(level, bounds, pos.offset(2, 2, cz), cz == 5 ? pillar() : blackstoneWall());
        }

        // 5. North Boss Challenge Court: 4 Towering 10-Block Arena Obelisks + Sovereign Throne Arch
        for (int[] obelisk : new int[][] {{-4, 7}, {4, 7}, {-4, 15}, {4, 15}}) {
            buildArenaObelisk(level, bounds, pos.offset(obelisk[0], 0, obelisk[1]), 10);
        }
        // Monumental Throne Backdrop Arch behind Saint (z=15, x=-3..3)
        for (int x = -3; x <= 3; x++) {
            int h = 6 - Math.abs(x);
            for (int y = 1; y <= h; y++) {
                boolean core = x == 0 && (y == 3 || y == 4);
                BlockState mat = core ? crystal()
                    : (y == h ? gilded() : (Math.abs(x) == 2 && y >= 2 && y <= 3 ? glass() : bricks()));
                placeBlock(level, bounds, pos.offset(x, y, 15), mat);
            }
        }
        placeBlock(level, bounds, pos.offset(0, 7, 15), lamp());
        placeBlock(level, bounds, pos.offset(0, 8, 15), crystal());

        // 6. Central Ritual Altar, Vault Wing, Energy Wing, Loot & Guardians (exact gameplay parity!)
        BlockPos ritualCenter = pos;
        placeBlock(level, bounds, ritualCenter, altar());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            placeBlock(level, bounds, ritualCenter.relative(d), runeBricks());
            placeBlock(level, bounds, ritualCenter.relative(d, 2), pillar());
        }
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                placeBlock(level, bounds, ritualCenter.offset(dx, 0, dz), crystal());
            }
        }
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                placeBlock(level, bounds, ritualCenter.offset(dx, 0, dz), chiseled());
            }
        }

        placeBlock(level, bounds, pos.offset(-10, 1, 0), ModBlocks.NIHILITY_VAULT.get().defaultBlockState());
        placeBlock(level, bounds, pos.offset(9, 1, 0), ModBlocks.NIHILITY_ENERGY_PRISM.get().defaultBlockState());
        placeBlock(level, bounds, pos.offset(10, 1, 0), ModBlocks.NIHILITY_TRANSPORT_CORE.get().defaultBlockState());
        placeBlock(level, bounds, pos.offset(11, 1, 0), ModBlocks.NIHILITY_ENERGY_CELL.get().defaultBlockState());
        placeLootBarrel(level, bounds, pos.offset(3, 1, 11), Direction.WEST, COMPLEX_RELICS_LOOT, random);

        spawnCreature(level, bounds, pos.offset(0, 1, -10), pos,
            CreatureDispositionPolicy.homeRadius(2), ModEntities.NIHILITY_FOLLOWER.get(), random,
            ModConfig.FOLLOWER_SPAWN_CHANCE.get());
        spawnCreature(level, bounds, pos.offset(-10, 1, 2), pos.offset(-10, 0, 0),
            CreatureDispositionPolicy.homeRadius(3), ModEntities.NIHILITY_ENVOY.get(), random,
            ModConfig.ENVOY_SPAWN_CHANCE.get());
        spawnCreature(level, bounds, pos.offset(10, 1, 2), pos.offset(10, 0, 0),
            CreatureDispositionPolicy.homeRadius(3), ModEntities.NIHILITY_ENVOY.get(), random,
            ModConfig.ENVOY_SPAWN_CHANCE.get());
        spawnCreature(level, bounds, pos.offset(0, 1, 2), pos,
            CreatureDispositionPolicy.homeRadius(4), ModEntities.NIHILITY_GRAND_ENVOY.get(), random,
            ModConfig.GRAND_ENVOY_SPAWN_CHANCE.get());
        spawnCreature(level, bounds, pos.offset(0, 1, 11), pos.offset(0, 0, 11),
            CreatureDispositionPolicy.homeRadius(5), ModEntities.NIHILITY_SAINT.get(), random,
            ModConfig.SAINT_SPAWN_CHANCE.get());
    }

    // ==================== ARCHITECTURAL PRIMITIVES ====================

    private static void buildSculptedPillar(ServerLevelAccessor level, BoundingBox bounds, BlockPos base, int height, boolean crownLamp) {
        placeBlock(level, bounds, base.above(1), chiseled());
        for (int y = 2; y < height; y++) {
            placeBlock(level, bounds, base.above(y), pillar());
        }
        placeBlock(level, bounds, base.above(height), gilded());
        if (crownLamp) {
            placeBlock(level, bounds, base.above(height + 1), lamp());
        }
    }

    private static void buildArenaObelisk(ServerLevelAccessor level, BoundingBox bounds, BlockPos base, int height) {
        fillFoundationColumn(level, bounds, base.below(), 3);
        placeBlock(level, bounds, base, gilded());
        placeBlock(level, bounds, base.above(1), chiseled());
        for (int y = 2; y <= height - 3; y++) {
            placeBlock(level, bounds, base.above(y), y % 3 == 0 ? runeBricks() : pillar());
        }
        placeBlock(level, bounds, base.above(height - 2), gilded());
        placeBlock(level, bounds, base.above(height - 1), crystal());
        placeBlock(level, bounds, base.above(height), lamp());
        placeBlock(level, bounds, base.above(height + 1), blackstoneWall());
    }

    private static void buildSouthPortalArch(ServerLevelAccessor level, BoundingBox bounds, BlockPos gateCenter, int apexHeight) {
        // Flanking monumental pylons at x=±2
        for (int side : new int[] {-2, 2}) {
            fillFoundationColumn(level, bounds, gateCenter.offset(side, -1, 0), 3);
            fillFoundationColumn(level, bounds, gateCenter.offset(side, -1, -1), 3);
            for (int y = 1; y <= apexHeight - 2; y++) {
                placeBlock(level, bounds, gateCenter.offset(side, y, 0),
                    (y == 1 || y == apexHeight - 2) ? gilded() : pillar());
                if (y <= 4) {
                    placeBlock(level, bounds, gateCenter.offset(side, y, -1),
                        y == 1 ? chiseled() : blackstoneWall());
                }
            }
            placeBlock(level, bounds, gateCenter.offset(side, apexHeight - 1, 0), lamp());
            placeBlock(level, bounds, gateCenter.offset(side, apexHeight, 0), blackstoneWall());
            placeBlock(level, bounds, gateCenter.offset(side, 5, -1), Blocks.SOUL_LANTERN.defaultBlockState());
        }
        // Stepped pointed arch gable spanning x=-1..1
        for (int x = -1; x <= 1; x++) {
            placeBlock(level, bounds, gateCenter.offset(x, 5, 0), x == 0 ? crystal() : gilded());
            placeBlock(level, bounds, gateCenter.offset(x, 6, 0), x == 0 ? lamp() : bricks());
            placeBlock(level, bounds, gateCenter.offset(x, 0, -1), stairs(Direction.NORTH, Half.BOTTOM));
            fillFoundationColumn(level, bounds, gateCenter.offset(x, -1, -1), 3);
        }
        placeBlock(level, bounds, gateCenter.offset(-1, 7, 0), stairs(Direction.EAST, Half.BOTTOM));
        placeBlock(level, bounds, gateCenter.offset(1, 7, 0), stairs(Direction.WEST, Half.BOTTOM));
        placeBlock(level, bounds, gateCenter.offset(0, 7, 0), gilded());
        placeBlock(level, bounds, gateCenter.offset(0, 8, 0), crystal());
        placeBlock(level, bounds, gateCenter.offset(0, 9, 0), blackstoneWall());
    }

    private static void buildWingRoof(ServerLevelAccessor level, BoundingBox bounds, BlockPos origin,
                                      int minX, int maxX, int minZ, int maxZ, int startY) {
        // Pitched cathedral roof along X-axis with overhanging eaves on North/South (minZ..maxZ)
        for (int step = 0; step <= 3; step++) {
            int y = startY + step;
            int zNorth = maxZ + 1 - step;
            int zSouth = minZ - 1 + step;
            for (int x = minX; x <= maxX; x++) {
                if (zSouth < zNorth) {
                    placeBlock(level, bounds, origin.offset(x, y, zSouth), stairs(Direction.SOUTH, Half.BOTTOM));
                    placeBlock(level, bounds, origin.offset(x, y, zNorth), stairs(Direction.NORTH, Half.BOTTOM));
                }
                if (step == 3) {
                    for (int rz = zSouth + 1; rz <= zNorth - 1; rz++) {
                        placeBlock(level, bounds, origin.offset(x, y, rz),
                            rz == 0 ? gilded() : tiles());
                    }
                }
            }
        }
        // Finials at wing roof peaks
        int midX = (minX + maxX) / 2;
        placeBlock(level, bounds, origin.offset(midX, startY + 4, 0), crystal());
        placeBlock(level, bounds, origin.offset(midX, startY + 5, 0), lamp());
        // Interior hanging chandelier
        placeBlock(level, bounds, origin.offset(midX, startY + 2, 0), Blocks.IRON_CHAIN.defaultBlockState());
        placeBlock(level, bounds, origin.offset(midX, startY + 1, 0), hangingLantern());
    }

    private static void buildFloatingAstralSpire(ServerLevelAccessor level, BoundingBox bounds, BlockPos center,
                                                 int baseY, int height) {
        // Central glowing crystal needle
        for (int i = 0; i < height; i++) {
            BlockState mat = switch (i) {
                case 0 -> gilded();
                case 1, 2 -> crystal();
                case 3 -> lamp();
                case 4, 5 -> pillar();
                case 6 -> crystal();
                default -> (i == height - 1) ? blackstoneWall() : gilded();
            };
            placeBlock(level, bounds, center.above(baseY + i), mat);
        }
        // Floating Astral Halo Ring hovering around spire mid-section!
        int ringY = baseY + 3;
        int[][] halo = {
            {-3, 0}, {3, 0}, {0, -3}, {0, 3},
            {-2, -2}, {-2, 2}, {2, -2}, {2, 2}
        };
        for (int i = 0; i < halo.length; i++) {
            int[] pt = halo[i];
            BlockState ringMat = (i < 4) ? crystal() : cryingObsidian();
            placeBlock(level, bounds, center.offset(pt[0], ringY, pt[1]), ringMat);
            if (i < 4) {
                placeBlock(level, bounds, center.offset(pt[0], ringY + 1, pt[1]), gilded());
            }
        }
    }

    private static void clearVolume(ServerLevelAccessor level, BoundingBox bounds, BlockPos origin,
                                    int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    placeBlock(level, bounds, origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void fillFoundationColumn(ServerLevelAccessor level, BoundingBox bounds, BlockPos start, int depth) {
        for (int d = 0; d < depth; d++) {
            BlockPos p = start.below(d);
            if (!isInside(bounds, p)) continue;
            BlockState existing = level.getBlockState(p);
            if (existing.isAir() || existing.liquid() || existing.canBeReplaced()) {
                level.setBlock(p, d == 0 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState(), 2);
            }
        }
    }

    // Block state helpers
    private static BlockState stone() { return ModBlocks.NIHILITY_STONE.get().defaultBlockState(); }
    private static BlockState bricks() { return ModBlocks.NIHILITY_BRICKS.get().defaultBlockState(); }
    private static BlockState crackedBricks() { return ModBlocks.CRACKED_NIHILITY_BRICKS.get().defaultBlockState(); }
    private static BlockState runeBricks() { return ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState(); }
    private static BlockState tiles() { return ModBlocks.NIHILITY_TILES.get().defaultBlockState(); }
    private static BlockState chiseled() { return ModBlocks.CHISELED_NIHILITY_STONE.get().defaultBlockState(); }
    private static BlockState pillar() { return ModBlocks.NIHILITY_PILLAR.get().defaultBlockState(); }
    private static BlockState glass() { return ModBlocks.NIHILITY_GLASS.get().defaultBlockState(); }
    private static BlockState crystal() { return ModBlocks.NIHILITY_CRYSTAL_BLOCK.get().defaultBlockState(); }
    private static BlockState lamp() { return ModBlocks.NIHILITY_LAMP.get().defaultBlockState(); }
    private static BlockState altar() { return ModBlocks.NIHILITY_ALTAR.get().defaultBlockState(); }
    private static BlockState gilded() { return Blocks.GILDED_BLACKSTONE.defaultBlockState(); }
    private static BlockState cryingObsidian() { return Blocks.CRYING_OBSIDIAN.defaultBlockState(); }
    private static BlockState blackstoneWall() { return Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(); }
    private static BlockState slab(net.minecraft.world.level.block.Block block) { return block.defaultBlockState(); }
    private static BlockState stairs(Direction facing, Half half) {
        return Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, facing)
            .setValue(StairBlock.HALF, half);
    }
    private static BlockState hangingLantern() {
        return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
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
