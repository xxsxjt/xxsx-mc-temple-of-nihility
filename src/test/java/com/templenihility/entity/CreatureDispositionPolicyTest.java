package com.templenihility.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import org.junit.jupiter.api.Test;

class CreatureDispositionPolicyTest {
    @Test
    void hostileTargetHasHighestPriority() {
        assertEquals(Disposition.HOSTILE, CreatureDispositionPolicy.resolve(true, true, true));
    }

    @Test
    void provocationInterruptsTradingBeforeCombatStarts() {
        assertEquals(Disposition.ALERT, CreatureDispositionPolicy.resolve(false, true, true));
    }

    @Test
    void tradingIsVisibleOnlyWhileCalm() {
        assertEquals(Disposition.TRADING, CreatureDispositionPolicy.resolve(false, false, true));
        assertEquals(Disposition.CALM, CreatureDispositionPolicy.resolve(false, false, false));
    }

    @Test
    void higherTiersTelegraphFasterWithoutRemovingTheWarning() {
        assertTrue(CreatureDispositionPolicy.warningTicks(1) > CreatureDispositionPolicy.warningTicks(5));
        assertTrue(CreatureDispositionPolicy.warningTicks(5) > 0);
    }

    @Test
    void higherTiersGuardAndRallyAcrossLargerAreas() {
        assertTrue(CreatureDispositionPolicy.homeRadius(5) > CreatureDispositionPolicy.homeRadius(1));
        assertTrue(CreatureDispositionPolicy.rallyRadius(5) > CreatureDispositionPolicy.rallyRadius(1));
    }

    @Test
    void corruptDispositionIdsFallBackToCalm() {
        assertEquals(Disposition.CALM, Disposition.byId(-1));
        assertEquals(Disposition.CALM, Disposition.byId(99));
    }

    @Test
    void ritualChallengeIsCombatAndBlocksTrading() {
        assertTrue(Disposition.CHALLENGE.isCombat());
        assertTrue(Disposition.CHALLENGE.blocksTrading());
    }
}
