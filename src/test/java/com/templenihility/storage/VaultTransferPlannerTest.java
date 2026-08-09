package com.templenihility.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class VaultTransferPlannerTest {
    @Test
    void insertFillsMatchingStacksBeforeEmptySlots() {
        List<VaultTransferPlanner.SlotState> slots = List.of(
            slot(VaultTransferPlanner.SlotKind.MATCHING, 60, 64),
            slot(VaultTransferPlanner.SlotKind.BLOCKED, 12, 64),
            slot(VaultTransferPlanner.SlotKind.EMPTY, 0, 64),
            slot(VaultTransferPlanner.SlotKind.MATCHING, 63, 64),
            slot(VaultTransferPlanner.SlotKind.EMPTY, 0, 16)
        );

        VaultTransferPlanner.Plan plan = VaultTransferPlanner.planInsert(70, slots);

        assertEquals(70, plan.moved());
        assertArrayEquals(new int[] {4, 0, 64, 1, 1}, plan.copyDeltas());
    }

    @Test
    void insertReturnsOnlyAvailableCapacity() {
        List<VaultTransferPlanner.SlotState> slots = List.of(
            slot(VaultTransferPlanner.SlotKind.MATCHING, 63, 64),
            slot(VaultTransferPlanner.SlotKind.EMPTY, 0, 64),
            slot(VaultTransferPlanner.SlotKind.BLOCKED, 0, 64)
        );

        VaultTransferPlanner.Plan plan = VaultTransferPlanner.planInsert(100, slots);

        assertEquals(65, plan.moved());
        assertArrayEquals(new int[] {1, 64, 0}, plan.copyDeltas());
    }

    @Test
    void extractSpansSlotsWithoutExceedingRequest() {
        VaultTransferPlanner.Plan plan = VaultTransferPlanner.planExtract(5, List.of(3, 0, 64, 2));

        assertEquals(5, plan.moved());
        assertArrayEquals(new int[] {3, 0, 2, 0}, plan.copyDeltas());
    }

    @Test
    void extractReturnsOnlyWhatExists() {
        VaultTransferPlanner.Plan plan = VaultTransferPlanner.planExtract(20, List.of(4, 3));

        assertEquals(7, plan.moved());
        assertArrayEquals(new int[] {4, 3}, plan.copyDeltas());
    }

    @Test
    void nonPositiveRequestsDoNotMutateAnything() {
        VaultTransferPlanner.Plan insert = VaultTransferPlanner.planInsert(
            -1, List.of(slot(VaultTransferPlanner.SlotKind.EMPTY, 0, 64)));
        VaultTransferPlanner.Plan extract = VaultTransferPlanner.planExtract(0, List.of(10));

        assertEquals(0, insert.moved());
        assertArrayEquals(new int[] {0}, insert.copyDeltas());
        assertEquals(0, extract.moved());
        assertArrayEquals(new int[] {0}, extract.copyDeltas());
    }

    private static VaultTransferPlanner.SlotState slot(
        VaultTransferPlanner.SlotKind kind, int count, int maxCount
    ) {
        return new VaultTransferPlanner.SlotState(kind, count, maxCount);
    }
}
