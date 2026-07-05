package com.templenihility.blockentity;

import com.templenihility.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NihilityVaultBlockEntity extends BlockEntity {
    public static final int SLOTS_PER_VAULT = 27 * 9;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_PER_VAULT, ItemStack.EMPTY);
    private boolean chunkLoaded;

    public NihilityVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIHILITY_VAULT.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public boolean isChunkLoaded() {
        return chunkLoaded;
    }

    public void setChunkLoaded(boolean chunkLoaded) {
        this.chunkLoaded = chunkLoaded;
        syncForcedChunk();
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS_PER_VAULT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        chunkLoaded = input.getBooleanOr("ChunkLoaded", false);
        syncForcedChunk();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putBoolean("ChunkLoaded", chunkLoaded);
    }

    private void syncForcedChunk() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ChunkPos chunkPos = ChunkPos.containing(worldPosition);
        if (chunkLoaded || !hasOtherLoadedVaultInChunk(serverLevel, chunkPos)) {
            serverLevel.setChunkForced(chunkPos.x(), chunkPos.z(), chunkLoaded);
        }
    }

    private boolean hasOtherLoadedVaultInChunk(ServerLevel serverLevel, ChunkPos chunkPos) {
        for (BlockEntity blockEntity : serverLevel.getChunk(chunkPos.x(), chunkPos.z()).getBlockEntities().values()) {
            if (blockEntity != this
                && blockEntity instanceof NihilityVaultBlockEntity vault
                && vault.isChunkLoaded()) {
                return true;
            }
        }
        return false;
    }
}
