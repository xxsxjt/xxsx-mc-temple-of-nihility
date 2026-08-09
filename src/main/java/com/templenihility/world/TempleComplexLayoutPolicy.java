package com.templenihility.world;

public final class TempleComplexLayoutPolicy {
    public static final int RADIUS = 15;

    public enum Zone {
        NONE,
        CONNECTOR,
        SANCTUM,
        VAULT_WING,
        ENERGY_WING,
        CHALLENGE_COURT
    }

    public static Zone zone(int x, int z) {
        if (Math.abs(x) <= 4 && Math.abs(z) <= 4) {
            return Zone.SANCTUM;
        }
        if (x >= -14 && x <= -6 && Math.abs(z) <= 4) {
            return Zone.VAULT_WING;
        }
        if (x >= 6 && x <= 14 && Math.abs(z) <= 4) {
            return Zone.ENERGY_WING;
        }
        if (Math.abs(x) <= 4 && z >= 7 && z <= 15) {
            return Zone.CHALLENGE_COURT;
        }
        boolean southApproach = Math.abs(x) <= 1 && z >= -15 && z <= -5;
        boolean eastWestLink = Math.abs(z) <= 1 && Math.abs(x) >= 5 && Math.abs(x) <= 15;
        boolean northLink = Math.abs(x) <= 1 && z >= 5 && z <= 6;
        return southApproach || eastWestLink || northLink ? Zone.CONNECTOR : Zone.NONE;
    }

    public static boolean isRoomEdge(Zone zone, int x, int z) {
        return switch (zone) {
            case SANCTUM -> Math.abs(x) == 4 || Math.abs(z) == 4;
            case VAULT_WING -> x == -14 || x == -6 || Math.abs(z) == 4;
            case ENERGY_WING -> x == 6 || x == 14 || Math.abs(z) == 4;
            case CHALLENGE_COURT -> Math.abs(x) == 4 || z == 7 || z == 15;
            default -> false;
        };
    }

    public static boolean isDoor(Zone zone, int x, int z) {
        return switch (zone) {
            case SANCTUM -> (Math.abs(z) == 4 && Math.abs(x) <= 1)
                || (Math.abs(x) == 4 && Math.abs(z) <= 1);
            case VAULT_WING -> x == -6 && Math.abs(z) <= 1;
            case ENERGY_WING -> x == 6 && Math.abs(z) <= 1;
            case CHALLENGE_COURT -> z == 7 && Math.abs(x) <= 1;
            default -> false;
        };
    }

    private TempleComplexLayoutPolicy() {
    }
}
