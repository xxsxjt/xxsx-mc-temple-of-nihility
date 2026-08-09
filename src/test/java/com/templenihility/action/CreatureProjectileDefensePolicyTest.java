package com.templenihility.action;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreatureProjectileDefensePolicyTest {
    @Test
    void followerGuardsOnlyLowTierWoundedAllies() {
        assertTrue(CreatureProjectileDefensePolicy.followerCanGuard(2, 0.5, true, false));
        assertFalse(CreatureProjectileDefensePolicy.followerCanGuard(3, 0.4, true, false));
        assertFalse(CreatureProjectileDefensePolicy.followerCanGuard(2, 0.8, true, false));
    }

    @Test
    void followerDoesNotConsumeCooldownForFriendlyOrInvalidShots() {
        assertFalse(CreatureProjectileDefensePolicy.followerCanGuard(1, 0.3, false, false));
        assertFalse(CreatureProjectileDefensePolicy.followerCanGuard(1, 0.3, true, true));
        assertFalse(CreatureProjectileDefensePolicy.followerCanGuard(1, Double.NaN, true, false));
    }

    @Test
    void envoyDeflectRequiresCooldownAndProjectileMotion() {
        assertTrue(CreatureProjectileDefensePolicy.envoyCanDeflect(true, 0.25, false));
        assertFalse(CreatureProjectileDefensePolicy.envoyCanDeflect(false, 0.25, false));
        assertFalse(CreatureProjectileDefensePolicy.envoyCanDeflect(true, 0.001, false));
        assertFalse(CreatureProjectileDefensePolicy.envoyCanDeflect(true, 0.25, true));
    }
}
