package com.templenihility.block;

import com.mojang.serialization.MapCodec;
import com.templenihility.TempleNihilityMod;
import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.init.ModItems;
import com.templenihility.storage.NihilityVaultNetwork;
import com.templenihility.world.NihilityVisualEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NihilityVaultBlock extends BaseEntityBlock {
    public static final MapCodec<NihilityVaultBlock> CODEC = simpleCodec(NihilityVaultBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public NihilityVaultBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(CONNECTED, false)
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NihilityVaultBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.getBlock() instanceof NihilityVaultBlock || super.skipRendering(state, adjacentState, side);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(CONNECTED) ? 5 : 9) != 0) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.72 + random.nextDouble() * 0.32;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.REVERSE_PORTAL,
            x + (random.nextDouble() - 0.5) * 0.55,
            y,
            z + (random.nextDouble() - 0.5) * 0.55,
            0.0, 0.018 + random.nextDouble() * 0.018, 0.0);

        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 0.08, z, 0.0, 0.006, 0.0);
        }

        if (state.getValue(NORTH)) {
            addConnectionSpark(level, random, pos, Direction.NORTH);
        }
        if (state.getValue(EAST)) {
            addConnectionSpark(level, random, pos, Direction.EAST);
        }
        if (state.getValue(SOUTH)) {
            addConnectionSpark(level, random, pos, Direction.SOUTH);
        }
        if (state.getValue(WEST)) {
            addConnectionSpark(level, random, pos, Direction.WEST);
        }
        if (state.getValue(UP)) {
            addConnectionSpark(level, random, pos, Direction.UP);
        }
        if (state.getValue(DOWN)) {
            addConnectionSpark(level, random, pos, Direction.DOWN);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
        return withConnections(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        return withConnections(level, pos, state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        boolean north = hasConnection(state, Direction.NORTH);
        boolean east = hasConnection(state, Direction.EAST);
        boolean south = hasConnection(state, Direction.SOUTH);
        boolean west = hasConnection(state, Direction.WEST);
        return setConnections(rotated,
            rotatedValue(rotation, Direction.NORTH, north, east, south, west),
            rotatedValue(rotation, Direction.EAST, north, east, south, west),
            rotatedValue(rotation, Direction.SOUTH, north, east, south, west),
            rotatedValue(rotation, Direction.WEST, north, east, south, west),
            hasConnection(state, Direction.UP),
            hasConnection(state, Direction.DOWN));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.NIHILITY_VAULT_EXPANSION.get())) {
            if (player.isShiftKeyDown() && stack.is(ModItems.NIHILITY_TERMINAL.get())) {
                return InteractionResult.PASS;
            }
            return openVault(level, pos, player);
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof NihilityVaultBlockEntity vault && vault.addCapacityUpgrade()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            NihilityVisualEffects.vaultUpgrade((ServerLevel) level, pos);
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.vault_upgrade_success",
                vault.getCapacityUpgrades(), vault.getCapacitySlots()));
        } else {
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.vault_upgrade_max",
                NihilityVaultBlockEntity.MAX_CAPACITY_UPGRADES));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openVault(level, pos, player);
    }

    private InteractionResult openVault(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        try {
            NihilityVaultNetwork.open((ServerLevel) level, pos, player);
        } catch (LinkageError | RuntimeException e) {
            TempleNihilityMod.LOGGER.error("Failed to open Nihility Vault at {}", pos, e);
            player.sendSystemMessage(Component.translatable("message.templenihility.vault_open_failed"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
            && !player.preventsBlockDrops()
            && level.getBlockEntity(pos) instanceof NihilityVaultBlockEntity vault) {
            vault.setChunkLoaded(false);
            Containers.dropContents(level, pos, vault.getItems());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static BlockState withConnections(LevelReader level, BlockPos pos, BlockState state) {
        return setConnections(state,
            isVault(level, pos.north()),
            isVault(level, pos.east()),
            isVault(level, pos.south()),
            isVault(level, pos.west()),
            isVault(level, pos.above()),
            isVault(level, pos.below()));
    }

    private static BlockState setConnections(BlockState state, boolean north, boolean east, boolean south, boolean west,
                                             boolean up, boolean down) {
        return state
            .setValue(NORTH, north)
            .setValue(EAST, east)
            .setValue(SOUTH, south)
            .setValue(WEST, west)
            .setValue(UP, up)
            .setValue(DOWN, down)
            .setValue(CONNECTED, north || east || south || west || up || down);
    }

    private static boolean isVault(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof NihilityVaultBlock;
    }

    private static void addConnectionSpark(Level level, RandomSource random, BlockPos pos, Direction direction) {
        if (random.nextInt(3) != 0) {
            return;
        }
        if (direction.getAxis() == Direction.Axis.Y) {
            double x = pos.getX() + 0.26 + random.nextDouble() * 0.48;
            double y = pos.getY() + (direction == Direction.UP ? 1.02 : -0.02);
            double z = pos.getZ() + 0.26 + random.nextDouble() * 0.48;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, direction.getStepY() * 0.012, 0.0);
            return;
        }
        double along = 0.18 + random.nextDouble() * 0.64;
        double x = pos.getX() + 0.5 + direction.getStepX() * 0.52;
        double z = pos.getZ() + 0.5 + direction.getStepZ() * 0.52;
        if (direction.getAxis() == Direction.Axis.X) {
            z = pos.getZ() + along;
        } else {
            x = pos.getX() + along;
        }
        level.addParticle(ParticleTypes.END_ROD, x, pos.getY() + 0.58 + random.nextDouble() * 0.24, z,
            direction.getStepX() * 0.01, 0.002, direction.getStepZ() * 0.01);
    }

    private static boolean hasConnection(BlockState state, Direction direction) {
        return switch (direction) {
            case NORTH -> state.getValue(NORTH);
            case EAST -> state.getValue(EAST);
            case SOUTH -> state.getValue(SOUTH);
            case WEST -> state.getValue(WEST);
            case UP -> state.getValue(UP);
            case DOWN -> state.getValue(DOWN);
            default -> false;
        };
    }

    private static boolean rotatedValue(Rotation rotation, Direction target,
                                        boolean north, boolean east, boolean south, boolean west) {
        for (Direction source : Direction.Plane.HORIZONTAL) {
            if (rotation.rotate(source) == target) {
                return switch (source) {
                    case NORTH -> north;
                    case EAST -> east;
                    case SOUTH -> south;
                    case WEST -> west;
                    default -> false;
                };
            }
        }
        return false;
    }
}
