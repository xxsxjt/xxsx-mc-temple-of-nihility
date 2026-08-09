package com.templenihility.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TempleLayoutPolicyTest {
    @Test
    void southEntranceIsThreeBlocksWideAndTall() {
        for (int y = 1; y <= 3; y++) {
            assertTrue(TempleLayoutPolicy.isEntranceOpening(-1, -8, y));
            assertTrue(TempleLayoutPolicy.isEntranceOpening(0, -8, y));
            assertTrue(TempleLayoutPolicy.isEntranceOpening(1, -8, y));
        }
        assertFalse(TempleLayoutPolicy.isEntranceOpening(2, -8, 2));
        assertFalse(TempleLayoutPolicy.isEntranceOpening(0, -8, 4));
    }

    @Test
    void ritualHallHasACentralDoor() {
        assertFalse(TempleLayoutPolicy.isInternalWall(0, 2, 2));
        assertFalse(TempleLayoutPolicy.isInternalWall(1, 2, 2));
        assertTrue(TempleLayoutPolicy.isInternalWall(2, 2, 2));
    }

    @Test
    void bothSideWingsHaveDoorwaysAtTheSameDepth() {
        assertFalse(TempleLayoutPolicy.isInternalWall(-3, -1, 2));
        assertFalse(TempleLayoutPolicy.isInternalWall(3, -1, 2));
        assertTrue(TempleLayoutPolicy.isInternalWall(-3, 0, 2));
        assertTrue(TempleLayoutPolicy.isInternalWall(3, 0, 2));
    }

    @Test
    void floorZonesSeparatePlayerPurposes() {
        assertEquals(TempleLayoutPolicy.FloorZone.AISLE, TempleLayoutPolicy.floorZone(0, -5));
        assertEquals(TempleLayoutPolicy.FloorZone.TRADE_WING, TempleLayoutPolicy.floorZone(-5, 0));
        assertEquals(TempleLayoutPolicy.FloorZone.ARCHIVE_WING, TempleLayoutPolicy.floorZone(5, 0));
        assertEquals(TempleLayoutPolicy.FloorZone.RITUAL_HALL, TempleLayoutPolicy.floorZone(0, 5));
    }

    @Test
    void perimeterNeverBecomesAnInteriorZone() {
        assertEquals(TempleLayoutPolicy.FloorZone.PERIMETER, TempleLayoutPolicy.floorZone(8, 0));
        assertEquals(TempleLayoutPolicy.FloorZone.PERIMETER, TempleLayoutPolicy.floorZone(0, -8));
    }
}
