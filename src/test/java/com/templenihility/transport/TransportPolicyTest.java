package com.templenihility.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.templenihility.transport.TransportPolicy.BlockedReason;
import com.templenihility.transport.TransportPolicy.FilterMode;
import com.templenihility.transport.TransportPolicy.Priority;
import com.templenihility.transport.TransportPolicy.RedstoneMode;
import com.templenihility.transport.TransportPolicy.ResourceMode;
import com.templenihility.transport.TransportPolicy.ResourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransportPolicyTest {
    @Test
    void redstoneModesMatchTheirNames() {
        assertTrue(TransportPolicy.allowsRedstone(RedstoneMode.IGNORE, false));
        assertTrue(TransportPolicy.allowsRedstone(RedstoneMode.IGNORE, true));
        assertTrue(TransportPolicy.allowsRedstone(RedstoneMode.REQUIRE_POWER, true));
        assertFalse(TransportPolicy.allowsRedstone(RedstoneMode.REQUIRE_POWER, false));
        assertTrue(TransportPolicy.allowsRedstone(RedstoneMode.REQUIRE_NO_POWER, false));
        assertFalse(TransportPolicy.allowsRedstone(RedstoneMode.REQUIRE_NO_POWER, true));
    }

    @Test
    void balancedPriorityRotatesTheFirstResource() {
        assertEquals(List.of(ResourceType.ITEMS, ResourceType.FLUIDS, ResourceType.ENERGY),
            TransportPolicy.orderedResources(ResourceMode.ALL, Priority.BALANCED, 0));
        assertEquals(List.of(ResourceType.FLUIDS, ResourceType.ENERGY, ResourceType.ITEMS),
            TransportPolicy.orderedResources(ResourceMode.ALL, Priority.BALANCED, 1));
        assertEquals(List.of(ResourceType.ENERGY, ResourceType.ITEMS, ResourceType.FLUIDS),
            TransportPolicy.orderedResources(ResourceMode.ALL, Priority.BALANCED, 2));
    }

    @Test
    void fixedPriorityFallsBackInAStableOrder() {
        assertEquals(List.of(ResourceType.ENERGY, ResourceType.ITEMS, ResourceType.FLUIDS),
            TransportPolicy.orderedResources(ResourceMode.ALL, Priority.ENERGY_FIRST, 99));
        assertEquals(List.of(ResourceType.FLUIDS),
            TransportPolicy.orderedResources(ResourceMode.FLUIDS, Priority.ITEMS_FIRST, 0));
    }

    @Test
    void whitelistAndBlacklistHandleEmptyFiltersSafely() {
        assertFalse(TransportPolicy.allowsItem(FilterMode.WHITELIST, false, false));
        assertTrue(TransportPolicy.allowsItem(FilterMode.WHITELIST, true, true));
        assertFalse(TransportPolicy.allowsItem(FilterMode.WHITELIST, true, false));
        assertTrue(TransportPolicy.allowsItem(FilterMode.BLACKLIST, false, false));
        assertFalse(TransportPolicy.allowsItem(FilterMode.BLACKLIST, true, true));
        assertTrue(TransportPolicy.allowsItem(FilterMode.BLACKLIST, true, false));
    }

    @Test
    void diagnosticPrefersActionableBlockageOverMissingOptionalCapabilities() {
        assertEquals(BlockedReason.FILTER_REJECTED, TransportPolicy.chooseDiagnostic(List.of(
            BlockedReason.SOURCE_CAPABILITY_MISSING,
            BlockedReason.SOURCE_EMPTY,
            BlockedReason.FILTER_REJECTED)));
        assertEquals(BlockedReason.TARGET_BLOCKED, TransportPolicy.chooseDiagnostic(List.of(
            BlockedReason.SOURCE_CAPABILITY_MISSING,
            BlockedReason.TARGET_BLOCKED)));
    }

    @Test
    void persistedIdsAreBoundedInsteadOfThrowing() {
        assertEquals(ResourceMode.ENERGY, ResourceMode.byId(-1));
        assertEquals(FilterMode.WHITELIST, FilterMode.byId(4));
        assertEquals(BlockedReason.IDLE, BlockedReason.byId(BlockedReason.values().length));
    }

    @Test
    void directionCyclingAlwaysSkipsTheOpposingSelection() {
        assertEquals(2, TransportPolicy.nextDistinctDirectionId(0, 1, 6));
        assertEquals(0, TransportPolicy.nextDistinctDirectionId(5, 1, 6));
    }
}
