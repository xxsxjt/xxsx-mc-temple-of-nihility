package com.templenihility.storage;

import com.templenihility.blockentity.NihilityVaultBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes an entire connected Void Vault network through NeoForge's transactional
 * {@link ResourceHandler} capability so AE2 Storage Bus, Refined Storage External Storage,
 * hoppers/pipes, and Easy Autocrafting seamlessly read, insert, and extract across every
 * linked vault block with full simulation & rollback semantics.
 */
public final class NihilityVaultItemHandler implements ResourceHandler<ItemResource> {
    private final NihilityVaultBlockEntity originVault;

    public NihilityVaultItemHandler(NihilityVaultBlockEntity originVault) {
        this.originVault = Objects.requireNonNull(originVault, "originVault");
    }

    private List<NihilityVaultBlockEntity> resolveVaults() {
        if (originVault.isRemoved()) {
            return List.of();
        }
        if (originVault.getLevel() instanceof ServerLevel serverLevel) {
            return NihilityVaultNetwork.scanForAutomation(serverLevel, originVault.getBlockPos());
        }
        return List.of(originVault);
    }

    private record SlotRef(NihilityVaultBlockEntity vault, int localIndex) {
        ItemStack get() {
            return vault.getItems().get(localIndex);
        }

        void set(ItemStack stack) {
            vault.getItems().set(localIndex, stack);
        }
    }

    private SlotRef resolveSlot(int globalIndex) {
        if (globalIndex < 0) {
            return null;
        }
        int cursor = globalIndex;
        for (NihilityVaultBlockEntity vault : resolveVaults()) {
            NonNullList<ItemStack> items = vault.getItems();
            if (cursor < items.size()) {
                return new SlotRef(vault, cursor);
            }
            cursor -= items.size();
        }
        return null;
    }

    @Override
    public int size() {
        int total = 0;
        for (NihilityVaultBlockEntity vault : resolveVaults()) {
            total += vault.getItems().size();
        }
        return total;
    }

    @Override
    public ItemResource getResource(int index) {
        SlotRef ref = resolveSlot(index);
        if (ref == null) {
            return ItemResource.EMPTY;
        }
        ItemStack stack = ref.get();
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        SlotRef ref = resolveSlot(index);
        return ref == null ? 0L : ref.get().getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (resource == null || resource.isEmpty()) {
            return 64L;
        }
        return Math.min(64L, resource.getMaxStackSize());
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resolveSlot(index) != null && resource != null && !resource.isEmpty();
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || resource == null || resource.isEmpty()) {
            return 0;
        }
        SlotRef ref = resolveSlot(index);
        if (ref == null) {
            return 0;
        }
        ItemStack existing = ref.get();
        int cap = (int) getCapacityAsLong(index, resource);
        if (existing.isEmpty()) {
            int inserted = Math.min(amount, cap);
            if (inserted <= 0) {
                return 0;
            }
            recordSnapshot(ref, transaction);
            ref.set(resource.toStack(inserted));
            return inserted;
        }
        if (!resource.matches(existing)) {
            return 0;
        }
        int space = cap - existing.getCount();
        if (space <= 0) {
            return 0;
        }
        int inserted = Math.min(amount, space);
        recordSnapshot(ref, transaction);
        ItemStack updated = existing.copy();
        updated.grow(inserted);
        ref.set(updated);
        return inserted;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || resource == null || resource.isEmpty()) {
            return 0;
        }
        List<NihilityVaultBlockEntity> vaults = resolveVaults();
        int cap = Math.min(64, resource.getMaxStackSize());
        int remaining = amount;

        // First pass: top off matching partial stacks across the connected vault network
        for (NihilityVaultBlockEntity vault : vaults) {
            NonNullList<ItemStack> items = vault.getItems();
            for (int i = 0; i < items.size() && remaining > 0; i++) {
                ItemStack existing = items.get(i);
                if (!existing.isEmpty() && resource.matches(existing) && existing.getCount() < cap) {
                    int add = Math.min(remaining, cap - existing.getCount());
                    SlotRef ref = new SlotRef(vault, i);
                    recordSnapshot(ref, transaction);
                    ItemStack updated = existing.copy();
                    updated.grow(add);
                    ref.set(updated);
                    remaining -= add;
                }
            }
        }

        // Second pass: fill empty slots across the connected vault network
        for (NihilityVaultBlockEntity vault : vaults) {
            NonNullList<ItemStack> items = vault.getItems();
            for (int i = 0; i < items.size() && remaining > 0; i++) {
                if (items.get(i).isEmpty()) {
                    int add = Math.min(remaining, cap);
                    SlotRef ref = new SlotRef(vault, i);
                    recordSnapshot(ref, transaction);
                    ref.set(resource.toStack(add));
                    remaining -= add;
                }
            }
        }
        return amount - remaining;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || resource == null || resource.isEmpty()) {
            return 0;
        }
        SlotRef ref = resolveSlot(index);
        if (ref == null) {
            return 0;
        }
        ItemStack existing = ref.get();
        if (existing.isEmpty() || !resource.matches(existing)) {
            return 0;
        }
        int extracted = Math.min(amount, existing.getCount());
        recordSnapshot(ref, transaction);
        if (extracted >= existing.getCount()) {
            ref.set(ItemStack.EMPTY);
        } else {
            ItemStack updated = existing.copy();
            updated.shrink(extracted);
            ref.set(updated);
        }
        return extracted;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || resource == null || resource.isEmpty()) {
            return 0;
        }
        List<NihilityVaultBlockEntity> vaults = resolveVaults();
        int remaining = amount;
        for (NihilityVaultBlockEntity vault : vaults) {
            NonNullList<ItemStack> items = vault.getItems();
            for (int i = 0; i < items.size() && remaining > 0; i++) {
                ItemStack existing = items.get(i);
                if (!existing.isEmpty() && resource.matches(existing)) {
                    int take = Math.min(remaining, existing.getCount());
                    SlotRef ref = new SlotRef(vault, i);
                    recordSnapshot(ref, transaction);
                    if (take >= existing.getCount()) {
                        ref.set(ItemStack.EMPTY);
                    } else {
                        ItemStack updated = existing.copy();
                        updated.shrink(take);
                        ref.set(updated);
                    }
                    remaining -= take;
                }
            }
        }
        return amount - remaining;
    }

    private static void recordSnapshot(SlotRef ref, TransactionContext transaction) {
        if (transaction == null) {
            ref.vault().setChanged();
            return;
        }
        SlotSnapshotJournal journal = new SlotSnapshotJournal(ref);
        journal.updateSnapshots(transaction);
    }

    private static final class SlotSnapshotJournal extends SnapshotJournal<ItemStack> {
        private final SlotRef ref;

        private SlotSnapshotJournal(SlotRef ref) {
            this.ref = ref;
        }

        @Override
        protected ItemStack createSnapshot() {
            return ref.get().copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            ref.set(snapshot.copy());
        }

        @Override
        protected void onRootCommit(ItemStack originalState) {
            ref.vault().setChanged();
        }
    }
}
