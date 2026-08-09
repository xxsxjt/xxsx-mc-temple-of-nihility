package com.templenihility.storage;

import java.util.List;

public final class VaultTransferPlanner {
    public enum SlotKind {
        MATCHING,
        EMPTY,
        BLOCKED
    }

    public record SlotState(SlotKind kind, int count, int maxCount) {
        public SlotState {
            count = Math.max(0, count);
            maxCount = Math.max(0, maxCount);
        }
    }

    public interface InsertSlotAccess {
        SlotKind kind(int index);

        int count(int index);

        int maxCount(int index);
    }

    @FunctionalInterface
    public interface CountAccess {
        int count(int index);
    }

    public record Plan(int[] deltas, int moved) {
        public Plan {
            deltas = deltas.clone();
            moved = Math.max(0, moved);
        }

        public int deltaAt(int index) {
            return deltas[index];
        }

        public int[] copyDeltas() {
            return deltas.clone();
        }
    }

    public static Plan planInsert(int requested, List<SlotState> slots) {
        return planInsert(requested, slots.size(), new InsertSlotAccess() {
            @Override
            public SlotKind kind(int index) {
                return slots.get(index).kind();
            }

            @Override
            public int count(int index) {
                return slots.get(index).count();
            }

            @Override
            public int maxCount(int index) {
                return slots.get(index).maxCount();
            }
        });
    }

    public static Plan planInsert(int requested, int slotCount, InsertSlotAccess slots) {
        int remaining = Math.max(0, requested);
        int[] deltas = new int[Math.max(0, slotCount)];
        remaining = planInsertPass(slotCount, slots, deltas, remaining, SlotKind.MATCHING);
        remaining = planInsertPass(slotCount, slots, deltas, remaining, SlotKind.EMPTY);
        return new Plan(deltas, Math.max(0, requested) - remaining);
    }

    public static Plan planExtract(int requested, List<Integer> availableCounts) {
        return planExtract(requested, availableCounts.size(), availableCounts::get);
    }

    public static Plan planExtract(int requested, int slotCount, CountAccess availableCounts) {
        int remaining = Math.max(0, requested);
        int[] deltas = new int[Math.max(0, slotCount)];
        for (int index = 0; index < slotCount && remaining > 0; index++) {
            int taken = Math.min(remaining, Math.max(0, availableCounts.count(index)));
            deltas[index] = taken;
            remaining -= taken;
        }
        return new Plan(deltas, Math.max(0, requested) - remaining);
    }

    private static int planInsertPass(int slotCount, InsertSlotAccess slots, int[] deltas,
                                      int remaining, SlotKind kind) {
        for (int index = 0; index < slotCount && remaining > 0; index++) {
            if (slots.kind(index) != kind) {
                continue;
            }
            int capacity = kind == SlotKind.EMPTY
                ? Math.max(0, slots.maxCount(index))
                : Math.max(0, slots.maxCount(index) - Math.max(0, slots.count(index)));
            int moved = Math.min(remaining, capacity);
            deltas[index] = moved;
            remaining -= moved;
        }
        return remaining;
    }

    private VaultTransferPlanner() {
    }
}
