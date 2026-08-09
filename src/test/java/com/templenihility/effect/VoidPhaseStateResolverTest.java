package com.templenihility.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VoidPhaseStateResolverTest {
    @Test
    void survivalRestoresOriginalAbilities() {
        var snapshot = new VoidPhaseStateResolver.Snapshot(false, false, false, false, false);

        var restored = VoidPhaseStateResolver.resolve(snapshot, false, true, true, false);

        assertEquals(new VoidPhaseStateResolver.RestoredState(false, false, false, false, false), restored);
    }

    @Test
    void externalFlightPermissionSurvivesPhase() {
        var snapshot = new VoidPhaseStateResolver.Snapshot(true, true, true, true, false);

        var restored = VoidPhaseStateResolver.resolve(snapshot, false, true, true, false);

        assertEquals(new VoidPhaseStateResolver.RestoredState(true, true, true, true, false), restored);
    }

    @Test
    void mayFlyDoesNotForceFlyingAfterRestore() {
        var snapshot = new VoidPhaseStateResolver.Snapshot(true, false, false, false, false);

        var restored = VoidPhaseStateResolver.resolve(snapshot, false, true, true, false);

        assertEquals(new VoidPhaseStateResolver.RestoredState(true, false, false, false, false), restored);
    }

    @Test
    void spectatorKeepsCurrentFlightAndCollision() {
        var snapshot = new VoidPhaseStateResolver.Snapshot(false, false, false, false, false);

        var restored = VoidPhaseStateResolver.resolve(snapshot, true, true, true, false);

        assertEquals(new VoidPhaseStateResolver.RestoredState(true, true, false, true, false), restored);
    }

    @Test
    void vanillaInvisibilityIsPreserved() {
        var snapshot = new VoidPhaseStateResolver.Snapshot(false, false, false, false, false);

        var restored = VoidPhaseStateResolver.resolve(snapshot, false, true, true, true);

        assertEquals(new VoidPhaseStateResolver.RestoredState(false, false, false, false, true), restored);
    }
}
