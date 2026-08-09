package com.templenihility.entity;

public final class CreatureDispositionPolicy {
    public enum Disposition {
        CALM,
        TRADING,
        ALERT,
        HOSTILE,
        CHALLENGE;

        public static Disposition byId(int id) {
            Disposition[] values = values();
            return id >= 0 && id < values.length ? values[id] : CALM;
        }

        public boolean blocksTrading() {
            return this == ALERT || isCombat();
        }

        public boolean isCombat() {
            return this == HOSTILE || this == CHALLENGE;
        }
    }

    public static Disposition resolve(boolean hasTarget, boolean pendingProvocation, boolean activelyTrading) {
        if (hasTarget) {
            return Disposition.HOSTILE;
        }
        if (pendingProvocation) {
            return Disposition.ALERT;
        }
        return activelyTrading ? Disposition.TRADING : Disposition.CALM;
    }

    public static int warningTicks(int tier) {
        return switch (clampTier(tier)) {
            case 1 -> 12;
            case 2 -> 10;
            case 3 -> 8;
            case 4 -> 6;
            default -> 4;
        };
    }

    public static int homeRadius(int tier) {
        return 5 + clampTier(tier) * 2;
    }

    public static double rallyRadius(int tier) {
        return 7.0 + clampTier(tier) * 2.0;
    }

    private static int clampTier(int tier) {
        return Math.max(1, Math.min(5, tier));
    }

    private CreatureDispositionPolicy() {
    }
}
