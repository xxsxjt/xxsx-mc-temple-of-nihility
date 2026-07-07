package com.templenihility.block;

import com.mojang.serialization.MapCodec;
import com.templenihility.blockentity.NihilityEnergyPrismBlockEntity;
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

public class NihilityEnergyPrismBlock extends BaseEntityBlock {
    public static final MapCodec<NihilityEnergyPrismBlock> CODEC = simpleCodec(NihilityEnergyPrismBlock::new);

    public NihilityEnergyPrismBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NihilityEnergyPrismBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
            ? null
            : createTickerHelper(type, ModBlockEntities.NIHILITY_ENERGY_PRISM.get(), NihilityEnergyPrismBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return showEnergy(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return showEnergy(level, pos, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        level.addParticle(random.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
            pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.35,
            pos.getY() + 1.02 + random.nextDouble() * 0.22,
            pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.35,
            0.0, 0.018, 0.0);
    }

    private InteractionResult showEnergy(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NihilityEnergyPrismBlockEntity prism) {
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.energy_prism_status", prism.getEnergy(), NihilityEnergyPrismBlockEntity.MAX_BUFFER));
        }
        return InteractionResult.SUCCESS;
    }
}
