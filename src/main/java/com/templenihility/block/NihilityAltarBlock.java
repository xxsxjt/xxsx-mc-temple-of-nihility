package com.templenihility.block;

import com.mojang.serialization.MapCodec;
import com.templenihility.blockentity.NihilityAltarBlockEntity;
import com.templenihility.menu.NihilityAltarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NihilityAltarBlock extends BaseEntityBlock {
    public static final MapCodec<NihilityAltarBlock> CODEC = simpleCodec(NihilityAltarBlock::new);

    public NihilityAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NihilityAltarBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof NihilityAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }

        altar.refreshStructure();
        altar.playStructurePreviewEffects();
        MenuProvider provider = new SimpleMenuProvider(
            (id, inventory, p) -> new NihilityAltarMenu(id, inventory, altar),
            Component.translatable("container.templenihility.nihility_altar"));
        player.openMenu(provider);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
            && !player.preventsBlockDrops()
            && level.getBlockEntity(pos) instanceof NihilityAltarBlockEntity altar) {
            Containers.dropContents(level, pos, altar.getItems());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
