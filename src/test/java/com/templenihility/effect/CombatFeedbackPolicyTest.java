package com.templenihility.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.templenihility.effect.CombatFeedbackPolicy.Kind;
import com.templenihility.effect.CombatFeedbackPolicy.ToastEntry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CombatFeedbackPolicyTest {
    @Test
    void repeatedPhaseDodgesCoalesceInPlaceAndSumEnergyDelta() {
        List<ToastEntry> list = List.of();
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);

        assertEquals(1, list.size());
        assertEquals(3, list.get(0).count());
        assertEquals(33, list.get(0).energyDelta());
        assertEquals("相位闪避 ×3", list.get(0).formattedLabel());
        assertEquals("Phase Dodge ×3", list.get(0).formatWithLabel("Phase Dodge"));
    }

    @Test
    void daggerBackstabAndLocalizedDetailFormatCleanly() {
        List<ToastEntry> list = List.of();
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.DAGGER_BACKSTAB, "", 4, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.DAGGER_BACKSTAB, "", 4, 3);
        assertEquals(1, list.size());
        assertEquals("Backstab Crit ×2", list.get(0).formatWithLabel("Backstab Crit"));

        List<ToastEntry> axe = CombatFeedbackPolicy.mergeOrInsert(List.of(), Kind.AXE_SHATTER, "×3", -10, 3);
        assertEquals("Shatter Cleave ×3", axe.get(0).formatWithLabel("Shatter Cleave"));
    }

    @Test
    void toastStackCapsAtMaxVisibleAndEvictsOldest() {
        List<ToastEntry> list = List.of();
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.PHASE_DODGE, "", 11, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.WARD_MEMBRANE, "", 4, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.FATE_CRIT, "", 6, 3);
        list = CombatFeedbackPolicy.mergeOrInsert(list, Kind.GAUNTLET_BURST, "", 0, 3);

        assertEquals(3, list.size());
        assertEquals(Kind.GAUNTLET_BURST, list.get(0).kind());
        assertEquals(Kind.FATE_CRIT, list.get(1).kind());
        assertEquals(Kind.WARD_MEMBRANE, list.get(2).kind());
    }

    @Test
    void dodgeAudioIsThrottledWithinTenTicks() {
        assertTrue(CombatFeedbackPolicy.shouldPlayDodgeAudio(100L, -100L));
        assertFalse(CombatFeedbackPolicy.shouldPlayDodgeAudio(105L, 100L));
        assertFalse(CombatFeedbackPolicy.shouldPlayDodgeAudio(109L, 100L));
        assertTrue(CombatFeedbackPolicy.shouldPlayDodgeAudio(110L, 100L));
    }

    @Test
    void idleFullEnergyHudAutoDimsWithoutVanishing() {
        assertEquals(1.0F, CombatFeedbackPolicy.computeHudAlpha(60, 100, false, 200));
        assertEquals(1.0F, CombatFeedbackPolicy.computeHudAlpha(100, 100, true, 200));
        assertEquals(1.0F, CombatFeedbackPolicy.computeHudAlpha(100, 100, false, 40));
        assertEquals(0.48F, CombatFeedbackPolicy.computeHudAlpha(100, 100, false, 300), 0.001F);
    }

    @Test
    void hudAnchorAvoidsHotbarAttackIndicatorAndClampsNarrowScreens() {
        assertEquals(418, CombatFeedbackPolicy.computeHudAnchorX(640, 94, false));
        assertEquals(438, CombatFeedbackPolicy.computeHudAnchorX(640, 94, true));
        assertEquals(222, CombatFeedbackPolicy.computeHudAnchorX(320, 94, true));
    }

    @Test
    void clonePreservesFullPowerOnPortalAndHalfOnDeath() {
        assertEquals(420, CombatFeedbackPolicy.retainedVoidPowerOnClone(420, false));
        assertEquals(210, CombatFeedbackPolicy.retainedVoidPowerOnClone(420, true));
        assertEquals(0, CombatFeedbackPolicy.retainedVoidPowerOnClone(-10, true));
        assertTrue(CombatFeedbackPolicy.isLowEnergy(11, 100));
        assertFalse(CombatFeedbackPolicy.isLowEnergy(12, 100));
    }

    @Test
    void everyFeedbackKindHasUniqueIdAndTranslationKey() {
        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (Kind k : Kind.values()) {
            assertTrue(ids.add(k.id), "Duplicate kind id: " + k);
            assertTrue(keys.add(k.translationKey), "Duplicate translation key: " + k);
            assertTrue(k.translationKey.startsWith("hud.templenihility.feedback."));
        }
    }
}
