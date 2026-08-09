package com.templenihility.blockentity;

import com.templenihility.TempleNihilityMod;
import com.templenihility.block.NihilityVaultBlock;
import com.templenihility.init.ModBlockEntities;
import java.util.Optional;
import java.util.UUID;
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
    private UUID owner;
    private boolean legacyVanillaChunkTicket;

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

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(owner);
    }

    public boolean claimOwner(UUID newOwner) {
        if (owner != null) {
            return false;
        }
        owner = newOwner;
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
        legacyVanillaChunkTicket = chunkLoaded && input.getIntOr("ChunkTicketFormat", 1) < 2;
        String ownerText = input.getStringOr("Owner", "");
        try {
            owner = ownerText.isEmpty() ? null : UUID.fromString(ownerText);
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putBoolean("ChunkLoaded", chunkLoaded);
        output.putBoolean("BreakProtected", breakProtected);
        output.putInt("CapacityUpgrades", capacityUpgrades);
        output.putInt("ChunkTicketFormat", 2);
        if (owner != null) {
            output.putString("Owner", owner.toString());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ChunkPos chunkPos = ChunkPos.containing(worldPosition);
            if (legacyVanillaChunkTicket) {
                serverLevel.setChunkForced(chunkPos.x(), chunkPos.z(), false);
                legacyVanillaChunkTicket = false;
                setChanged();
            }
            syncForcedChunk();
            NihilityVaultBlock.refreshConnections(serverLevel, worldPosition);
        }
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
        TempleNihilityMod.VAULT_CHUNK_TICKETS.forceChunk(
            serverLevel, worldPosition, chunkPos.x(), chunkPos.z(), chunkLoaded, false);
    }
}
