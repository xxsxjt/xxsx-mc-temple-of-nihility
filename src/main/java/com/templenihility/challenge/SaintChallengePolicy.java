package com.templenihility.challenge;

public final class SaintChallengePolicy {
    public static final int REQUIRED_ALTAR_TIER = 3;
    public static final int DECLARATION_TICKS = 3 * 20;
    public static final int DURATION_TICKS = 4 * 60 * 20;
    public static final int RETRY_COOLDOWN_TICKS = 15 * 20;
    public static final double START_SEARCH_RADIUS = 24.0;
    public static final double ACTIVE_SEARCH_RADIUS = 72.0;
    public static final double ARENA_RADIUS = 28.0;

    public enum StartStatus {
        READY,
        OWN_CHALLENGE,
        TIER_TOO_LOW,
        PLAYER_INVALID,
        NO_SAINT,
        SAINT_BUSY;

        public static StartStatus byId(int id) {
            StartStatus[] values = values();
            return id >= 0 && id < values.length ? values[id] : PLAYER_INVALID;
        }

        public boolean canClick() {
            return this == READY || this == OWN_CHALLENGE;
        }
    }

    public enum EndReason {
        NONE,
        PLAYER_GONE,
        PLAYER_DEFEATED,
        INVALID_MODE,
        LEFT_ARENA,
        TIMEOUT,
        VOLUNTARY
    }

    public static StartStatus evaluateStart(int altarTier, boolean playerEligible, int nearbySaints,
                                            boolean ownChallenge, boolean availableSaint) {
        if (ownChallenge) {
            return StartStatus.OWN_CHALLENGE;
        }
        if (!playerEligible) {
            return StartStatus.PLAYER_INVALID;
        }
        if (altarTier < REQUIRED_ALTAR_TIER) {
            return StartStatus.TIER_TOO_LOW;
        }
        if (nearbySaints <= 0) {
            return StartStatus.NO_SAINT;
        }
        return availableSaint ? StartStatus.READY : StartStatus.SAINT_BUSY;
    }

    public static EndReason evaluateEnd(boolean playerPresent, boolean playerAlive, boolean playerEligible,
                                        boolean sameLevel, double distanceSquared, long gameTime,
                                        long expiresGameTime) {
        if (!playerPresent) {
            return EndReason.PLAYER_GONE;
        }
        if (!playerAlive) {
            return EndReason.PLAYER_DEFEATED;
        }
        if (!playerEligible) {
            return EndReason.INVALID_MODE;
        }
        if (!sameLevel || distanceSquared > ARENA_RADIUS * ARENA_RADIUS) {
            return EndReason.LEFT_ARENA;
        }
        return gameTime >= expiresGameTime ? EndReason.TIMEOUT : EndReason.NONE;
    }

    private SaintChallengePolicy() {
    }
}
