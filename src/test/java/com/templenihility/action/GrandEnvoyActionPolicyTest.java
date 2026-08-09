package com.templenihility.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import org.junit.jupiter.api.Test;

class GrandEnvoyActionPolicyTest {
    @Test
    void lowHealthWithoutShieldPrioritizesShield() {
        assertEquals(ActionType.GRAND_ENVOY_SHIELD,
            GrandEnvoyActionPolicy.select(0.6, 0, true, 64.0));
    }

    @Test
    void distantEngagedAlliesTriggerRecallBeforeDomain() {
        assertEquals(ActionType.GRAND_ENVOY_RECALL,
            GrandEnvoyActionPolicy.select(1.0, 0, true, 64.0));
    }

    @Test
    void readableMidRangeTriggersDomain() {
        assertEquals(ActionType.GRAND_ENVOY_DOMAIN,
            GrandEnvoyActionPolicy.select(1.0, 0, false, 8.0 * 8.0));
        assertEquals(ActionType.NONE,
            GrandEnvoyActionPolicy.select(1.0, 0, false, 2.0 * 2.0));
    }

    @Test
    void shieldReducesRatherThanDeletesDamage() {
        float reduced = GrandEnvoyActionPolicy.shieldedDamage(20.0F);
        assertTrue(reduced > 0.0F);
        assertTrue(reduced < 20.0F);
        assertEquals(0.0F, GrandEnvoyActionPolicy.shieldedDamage(Float.NaN));
    }

    @Test
    void eitherEndOfATeleportCanEnterTheDomainCost() {
        assertTrue(GrandEnvoyActionPolicy.taxesTeleport(true, false));
        assertTrue(GrandEnvoyActionPolicy.taxesTeleport(false, true));
        assertFalse(GrandEnvoyActionPolicy.taxesTeleport(false, false));
    }
}
