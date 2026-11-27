package com.templenihility.effect;

import static org.junit.jupiter.api.Assertions.*;

import com.templenihility.effect.CombatFeedbackPolicy.Kind;
import com.templenihility.effect.CombatFeedbackPolicy.ToastEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

class CombatFeedbackPolicyTest {
    @Test
    void coalescesRapidPhaseDodgesInsteadOfSpammingDuplicateToasts() {
        List<ToastEntry> list = List.of();
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);
        assertEquals(1, list.size());
        assertEquals(3, list.get(0).count());
        assertEquals(33, list.get(0).energyDelta());
        assertEquals("相位闪避 ×3", list.get(0).formattedLabel());
    }

    @Test
    void capsVisibleToastsAtThreeAndExpiresCleanly() {
        List<ToastEntry> list = List.of();
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.FATE_CRIT, "", 6, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.WARD_MEMBRANE, "", 4, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.GAUNTLET_BURST, "", 0, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);
        assertEquals(3, list.size());
        assertEquals(Kind.PHASE_DODGE, list.get(0).kind());

        for (int i = 0; i < 40; i++) {
            list = CombatFeedbackPolicy.tickToasts(list);
        }
        assertTrue(list.isEmpty());
    }

    @Test
    void throttlesRapidDodgeAudioAndDimsIdleFullHud() {
        assertTrue(CombatFeedbackPolicy.shouldPlayDodgeAudio(100L, -1L));
        assertFalse(CombatFeedbackPolicy.shouldPlayDodgeAudio(105L, 100L));
        assertTrue(CombatFeedbackPolicy.shouldPlayDodgeAudio(110L, 100L));

        assertEquals(1.0F, CombatFeedbackPolicy.computeHudAlpha(80, 150, false, 500));
        assertEquals(1.0F, CombatFeedbackPolicy.computeHudAlpha(150, 150, true, 500));
        assertTrue(CombatFeedbackPolicy.computeHudAlpha(150, 150, false, 300) < 0.55F);
    }
}
