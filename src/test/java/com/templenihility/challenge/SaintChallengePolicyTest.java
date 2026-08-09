package com.templenihility.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.templenihility.challenge.SaintChallengePolicy.EndReason;
import com.templenihility.challenge.SaintChallengePolicy.StartStatus;
import org.junit.jupiter.api.Test;

class SaintChallengePolicyTest {
    @Test
    void activeParticipantCanAlwaysUseTheAltarToWithdraw() {
        assertEquals(StartStatus.OWN_CHALLENGE,
            SaintChallengePolicy.evaluateStart(0, false, 0, true, false));
    }

    @Test
    void tierThreeAltarIsRequiredToStart() {
        assertEquals(StartStatus.TIER_TOO_LOW,
            SaintChallengePolicy.evaluateStart(2, true, 1, false, true));
    }

    @Test
    void missingAndBusySaintsHaveDifferentFeedback() {
        assertEquals(StartStatus.NO_SAINT,
            SaintChallengePolicy.evaluateStart(3, true, 0, false, false));
        assertEquals(StartStatus.SAINT_BUSY,
            SaintChallengePolicy.evaluateStart(3, true, 1, false, false));
    }

    @Test
    void validPlayerAndAvailableSaintCanStart() {
        assertEquals(StartStatus.READY,
            SaintChallengePolicy.evaluateStart(3, true, 1, false, true));
    }

    @Test
    void leavingTheArenaEndsTheChallenge() {
        double outside = SaintChallengePolicy.ARENA_RADIUS + 0.1;
        assertEquals(EndReason.LEFT_ARENA,
            SaintChallengePolicy.evaluateEnd(true, true, true, true,
                outside * outside, 100L, 1000L));
    }

    @Test
    void challengeTimesOutAtItsDeadline() {
        assertEquals(EndReason.TIMEOUT,
            SaintChallengePolicy.evaluateEnd(true, true, true, true,
                0.0, 1000L, 1000L));
    }

    @Test
    void deadOrDisconnectedPlayersCannotLeaveAStuckChallenge() {
        assertEquals(EndReason.PLAYER_DEFEATED,
            SaintChallengePolicy.evaluateEnd(true, false, true, true,
                0.0, 100L, 1000L));
        assertEquals(EndReason.PLAYER_GONE,
            SaintChallengePolicy.evaluateEnd(false, false, false, false,
                Double.POSITIVE_INFINITY, 100L, 1000L));
    }
}
