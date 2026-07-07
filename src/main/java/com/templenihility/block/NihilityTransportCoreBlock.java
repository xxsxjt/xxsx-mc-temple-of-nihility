package com.templenihility.block;

import com.mojang.serialization.MapCodec;
import com.templenihility.blockentity.NihilityTransportCoreBlockEntity;
import com.templenihility.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NihilityTransportCoreBlock extends BaseEntityBlock {
    public static final MapCodec<NihilityTransportCoreBlock> CODEC = simpleCodec(NihilityTransportCoreBlock::new);

    public NihilityTransportCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NihilityTransportCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
            ? null
            : createTickerHelper(type, ModBlockEntities.NIHILITY_TRANSPORT_CORE.get(), NihilityTransportCoreBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return showStatus(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return showStatus(level, pos, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        level.addParticle(random.nextBoolean() ? ParticleTypes.PORTAL : ParticleTypes.ELECTRIC_SPARK,
            pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.62,
            pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.62,
            pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.62,
            (random.nextDouble() - 0.5) * 0.018,
            (random.nextDouble() - 0.5) * 0.018,
            (random.nextDouble() - 0.5) * 0.018);
    }

    private InteractionResult showStatus(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NihilityTransportCoreBlockEntity core) {
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.transport_core_status",
                core.getLastItemMoved(),
                core.getLastFluidMoved(),
                core.getLastEnergyMoved()));
        }
        return InteractionResult.SUCCESS;
    }
}
