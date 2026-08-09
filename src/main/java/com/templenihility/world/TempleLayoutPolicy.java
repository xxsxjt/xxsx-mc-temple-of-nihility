package com.templenihility.world;

public final class TempleLayoutPolicy {
    public static final int RADIUS = 8;

    public enum FloorZone {
        PERIMETER,
        AISLE,
        TRADE_WING,
        ARCHIVE_WING,
        RITUAL_HALL
    }

    public static FloorZone floorZone(int x, int z) {
        if (Math.abs(x) == RADIUS || Math.abs(z) == RADIUS) {
            return FloorZone.PERIMETER;
        }
        if (z >= 3) {
            return FloorZone.RITUAL_HALL;
        }
        if (x <= -3 && z >= -4) {
            return FloorZone.TRADE_WING;
        }
        if (x >= 3 && z >= -4) {
            return FloorZone.ARCHIVE_WING;
        }
        return FloorZone.AISLE;
    }

    public static boolean isEntranceOpening(int x, int z, int y) {
        return z == -RADIUS && Math.abs(x) <= 1 && y >= 1 && y <= 3;
    }

    public static boolean isInternalWall(int x, int z, int y) {
        if (y < 1 || y > 3) {
            return false;
        }
        if (z == 2 && Math.abs(x) <= 7) {
            return Math.abs(x) > 1;
        }
        if ((x == -3 || x == 3) && z >= -4 && z <= 1) {
            return z != -1;
        }
        return false;
    }

    public static boolean isOuterWall(int x, int z, int y) {
        return y >= 1 && y <= 4
            && (Math.abs(x) == RADIUS || Math.abs(z) == RADIUS)
            && !isEntranceOpening(x, z, y);
    }

    private TempleLayoutPolicy() {
    }
}
