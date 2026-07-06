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
    public static final int MAX_CAPACITY_UPGRADES = 3;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS_PER_VAULT, ItemStack.EMPTY);
    private boolean chunkLoaded;
    private boolean breakProtected = true;
    private int capacityUpgrades;

    public NihilityVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIHILITY_VAULT.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public int getCapacityUpgrades() {
        return capacityUpgrades;
    }

    public int getCapacitySlots() {
        return SLOTS_PER_VAULT * (1 + capacityUpgrades);
    }

    public boolean addCapacityUpgrade() {
        if (capacityUpgrades >= MAX_CAPACITY_UPGRADES) {
            return false;
        }
        capacityUpgrades++;
        resizeItems(getCapacitySlots());
        setChanged();
        return true;
    }

    public boolean isChunkLoaded() {
        return chunkLoaded;
    }

    public void setChunkLoaded(boolean chunkLoaded) {
        this.chunkLoaded = chunkLoaded;
        syncForcedChunk();
        setChanged();
    }

    public boolean isBreakProtected() {
        return breakProtected;
    }

    public void setBreakProtected(boolean breakProtected) {
        this.breakProtected = breakProtected;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        capacityUpgrades = Math.max(0, Math.min(MAX_CAPACITY_UPGRADES, input.getIntOr("CapacityUpgrades", 0)));
        items = NonNullList.withSize(getCapacitySlots(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        chunkLoaded = input.getBooleanOr("ChunkLoaded", false);
        breakProtected = input.getBooleanOr("BreakProtected", true);
        syncForcedChunk();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putBoolean("ChunkLoaded", chunkLoaded);
        output.putBoolean("BreakProtected", breakProtected);
        output.putInt("CapacityUpgrades", capacityUpgrades);
    }

    private void resizeItems(int size) {
        if (items.size() == size) {
            return;
        }
        NonNullList<ItemStack> resized = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(items.size(), resized.size()); i++) {
            resized.set(i, items.get(i));
        }
        items = resized;
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
