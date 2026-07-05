package com.templenihility.block;

import com.mojang.serialization.MapCodec;
import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.storage.NihilityVaultNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NihilityVaultBlock extends BaseEntityBlock {
    public static final MapCodec<NihilityVaultBlock> CODEC = simpleCodec(NihilityVaultBlock::new);

    public NihilityVaultBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof NihilityVaultBlockEntity vault) {
            ServerLevel serverLevel = (ServerLevel) level;
            boolean enabled = !vault.isChunkLoaded();
            java.util.List<NihilityVaultBlockEntity> vaults = NihilityVaultNetwork.collect(serverLevel, pos);
            NihilityVaultNetwork.setNetworkChunkLoaded(serverLevel, pos, enabled);
            player.sendSystemMessage(Component.translatable(
                enabled ? "message.templenihility.vault_chunkload_on" : "message.templenihility.vault_chunkload_off",
                vaults.size()));
            return InteractionResult.SUCCESS;
        }

        NihilityVaultNetwork.open((ServerLevel) level, pos, player);
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
}
