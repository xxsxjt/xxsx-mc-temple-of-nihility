package com.templenihility.energy;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class VoidEnergyBlockHandler implements EnergyHandler {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final IntSupplier capacity;
    private final int maxInsert;
    private final int maxExtract;
    private final Runnable onChanged;
    private final SnapshotJournal<Integer> journal = new SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return getter.getAsInt();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            setter.accept(snapshot);
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            if (getter.getAsInt() != originalState) {
                onChanged.run();
            }
        }
    };

    public VoidEnergyBlockHandler(IntSupplier getter, IntConsumer setter, IntSupplier capacity,
                                  int maxInsert, int maxExtract, Runnable onChanged) {
        this.getter = getter;
        this.setter = setter;
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.onChanged = onChanged;
    }

    @Override
    public long getAmountAsLong() {
        return getter.getAsInt();
    }

    @Override
    public long getCapacityAsLong() {
        return capacity.getAsInt();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (amount <= 0) {
            return 0;
        }
        int stored = getter.getAsInt();
        int inserted = Math.min(amount, Math.min(maxInsert, capacity.getAsInt() - stored));
        if (inserted <= 0) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        setter.accept(stored + inserted);
        return inserted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        if (amount <= 0) {
            return 0;
        }
        int stored = getter.getAsInt();
        int extracted = Math.min(amount, Math.min(maxExtract, stored));
        if (extracted <= 0) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        setter.accept(stored - extracted);
        return extracted;
    }
}
