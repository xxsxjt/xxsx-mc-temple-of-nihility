package com.templenihility.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import org.junit.jupiter.api.Test;

class SaintActionPolicyTest {
    @Test
    void saintAlternatesReadableAttackFamilies() {
        assertEquals(ActionType.SAINT_ERASURE_LINE, SaintActionPolicy.select(0, 64.0));
        assertEquals(ActionType.SAINT_MISSING_ZONE, SaintActionPolicy.select(1, 64.0));
        assertEquals(ActionType.SAINT_ERASURE_LINE, SaintActionPolicy.select(2, 64.0));
    }

    @Test
    void saintDoesNotStartArenaAttacksOutsideTheirRange() {
        assertEquals(ActionType.NONE,
            SaintActionPolicy.select(0, SaintActionPolicy.ERASURE_LINE_LENGTH
                * SaintActionPolicy.ERASURE_LINE_LENGTH + 1.0));
        assertEquals(ActionType.NONE, SaintActionPolicy.select(0, Double.NaN));
    }

    @Test
    void erasureLineHitsOnlyForwardCorridor() {
        assertTrue(SaintActionPolicy.lineHits(8.0, 0.0, 0.0, 0.0, 1.0, 0.0));
        assertFalse(SaintActionPolicy.lineHits(8.0, 2.0, 0.0, 0.0, 1.0, 0.0));
        assertFalse(SaintActionPolicy.lineHits(-1.0, 0.0, 0.0, 0.0, 1.0, 0.0));
        assertFalse(SaintActionPolicy.lineHits(20.0, 0.0, 0.0, 0.0, 1.0, 0.0));
    }

    @Test
    void missingZoneUsesItsVisibleRadius() {
        assertTrue(SaintActionPolicy.zoneContains(2.0, 0.0, 0.0, 0.0));
        assertFalse(SaintActionPolicy.zoneContains(2.5, 0.0, 0.0, 0.0));
    }

    @Test
    void lowHealthPeriodicallyTriggersVoidTerminus() {
        assertEquals(ActionType.SAINT_VOID_TERMINUS,
            SaintActionPolicy.select(0, 64.0, 0.2));
        assertEquals(ActionType.SAINT_MISSING_ZONE,
            SaintActionPolicy.select(1, 64.0, 0.2));
        assertEquals(ActionType.SAINT_ERASURE_LINE,
            SaintActionPolicy.select(2, 64.0, 0.2));
        assertEquals(ActionType.SAINT_VOID_TERMINUS,
            SaintActionPolicy.select(3, 64.0, 0.2));
    }

    @Test
    void healthySaintNeverUsesVoidTerminus() {
        assertEquals(ActionType.SAINT_ERASURE_LINE,
            SaintActionPolicy.select(0, 64.0, 0.8));
        assertEquals(ActionType.SAINT_MISSING_ZONE,
            SaintActionPolicy.select(1, 64.0, 0.8));
    }

    @Test
    void terminusSafeZoneShrinksMonotonically() {
        double start = SaintActionPolicy.terminusSafeRadius(0.0);
        double middle = SaintActionPolicy.terminusSafeRadius(0.5);
        double end = SaintActionPolicy.terminusSafeRadius(1.0);
        assertTrue(start > middle);
        assertTrue(middle > end);
        assertEquals(SaintActionPolicy.TERMINUS_START_RADIUS, start, 1.0E-6);
        assertEquals(SaintActionPolicy.TERMINUS_END_RADIUS, end, 1.0E-6);
    }

    @Test
    void terminusSafeZoneMembershipFollowsCurrentRadius() {
        double startRadius = SaintActionPolicy.terminusSafeRadius(0.0);
        assertTrue(SaintActionPolicy.isInTerminusSafeZone(
            (startRadius * 0.5) * (startRadius * 0.5), 0.0));
        double endRadius = SaintActionPolicy.terminusSafeRadius(1.0);
        assertFalse(SaintActionPolicy.isInTerminusSafeZone(
            (endRadius + 1.0) * (endRadius + 1.0), 1.0));
    }

    @Test
    void terminusDamageRisesAsZoneShrinks() {
        float early = SaintActionPolicy.terminusDamage(0.0);
        float late = SaintActionPolicy.terminusDamage(1.0);
        assertTrue(late > early);
        assertEquals(SaintActionPolicy.TERMINUS_MAX_DAMAGE, late, 1.0E-3F);
    }
}
