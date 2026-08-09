package com.templenihility.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TempleComplexLayoutPolicyTest {
    @Test
    void fourPurposeRoomsAreDistinct() {
        assertEquals(TempleComplexLayoutPolicy.Zone.SANCTUM, TempleComplexLayoutPolicy.zone(0, 0));
        assertEquals(TempleComplexLayoutPolicy.Zone.VAULT_WING, TempleComplexLayoutPolicy.zone(-10, 0));
        assertEquals(TempleComplexLayoutPolicy.Zone.ENERGY_WING, TempleComplexLayoutPolicy.zone(10, 0));
        assertEquals(TempleComplexLayoutPolicy.Zone.CHALLENGE_COURT, TempleComplexLayoutPolicy.zone(0, 11));
    }

    @Test
    void southApproachIsThreeBlocksWide() {
        assertEquals(TempleComplexLayoutPolicy.Zone.CONNECTOR, TempleComplexLayoutPolicy.zone(-1, -12));
        assertEquals(TempleComplexLayoutPolicy.Zone.CONNECTOR, TempleComplexLayoutPolicy.zone(0, -12));
        assertEquals(TempleComplexLayoutPolicy.Zone.CONNECTOR, TempleComplexLayoutPolicy.zone(1, -12));
        assertEquals(TempleComplexLayoutPolicy.Zone.NONE, TempleComplexLayoutPolicy.zone(2, -12));
    }

    @Test
    void eastAndWestRoomsConnectToTheSanctum() {
        assertEquals(TempleComplexLayoutPolicy.Zone.CONNECTOR, TempleComplexLayoutPolicy.zone(-5, 0));
        assertEquals(TempleComplexLayoutPolicy.Zone.CONNECTOR, TempleComplexLayoutPolicy.zone(5, 0));
    }

    @Test
    void unusedCornersRemainOpenTerrain() {
        assertEquals(TempleComplexLayoutPolicy.Zone.NONE, TempleComplexLayoutPolicy.zone(12, 12));
        assertEquals(TempleComplexLayoutPolicy.Zone.NONE, TempleComplexLayoutPolicy.zone(-12, -12));
    }

    @Test
    void roomEdgesDoNotMarkConnectorsAsWalls() {
        assertTrue(TempleComplexLayoutPolicy.isRoomEdge(TempleComplexLayoutPolicy.Zone.SANCTUM, 4, 0));
        assertFalse(TempleComplexLayoutPolicy.isRoomEdge(TempleComplexLayoutPolicy.Zone.CONNECTOR, 5, 0));
    }

    @Test
    void everyRoomUsesThreeBlockWideConnectorDoors() {
        for (int offset = -1; offset <= 1; offset++) {
            assertTrue(TempleComplexLayoutPolicy.isDoor(TempleComplexLayoutPolicy.Zone.SANCTUM, offset, -4));
            assertTrue(TempleComplexLayoutPolicy.isDoor(TempleComplexLayoutPolicy.Zone.SANCTUM, 4, offset));
            assertTrue(TempleComplexLayoutPolicy.isDoor(TempleComplexLayoutPolicy.Zone.VAULT_WING, -6, offset));
            assertTrue(TempleComplexLayoutPolicy.isDoor(TempleComplexLayoutPolicy.Zone.ENERGY_WING, 6, offset));
            assertTrue(TempleComplexLayoutPolicy.isDoor(TempleComplexLayoutPolicy.Zone.CHALLENGE_COURT, offset, 7));
        }
        assertFalse(TempleComplexLayoutPolicy.isDoor(TempleComplexLayoutPolicy.Zone.SANCTUM, 2, -4));
    }
}
