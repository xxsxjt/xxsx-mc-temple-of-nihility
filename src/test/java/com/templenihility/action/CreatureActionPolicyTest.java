package com.templenihility.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import org.junit.jupiter.api.Test;

class CreatureActionPolicyTest {
    @Test
    void actionPhasesFollowWindupActiveRecoveryOrder() {
        ActionType action = ActionType.FOLLOWER_INTERCEPT;
        assertEquals(Phase.WINDUP, CreatureActionPolicy.phase(action, 0));
        assertEquals(Phase.ACTIVE, CreatureActionPolicy.phase(action, action.windupTicks()));
        assertEquals(Phase.RECOVERY,
            CreatureActionPolicy.phase(action, action.windupTicks() + action.activeTicks()));
        assertEquals(Phase.IDLE, CreatureActionPolicy.phase(action, action.totalTicks()));
    }

    @Test
    void noActionIsAlwaysIdleAndFinished() {
        assertEquals(Phase.IDLE, CreatureActionPolicy.phase(ActionType.NONE, 0));
        assertTrue(CreatureActionPolicy.isFinished(ActionType.NONE, 0));
    }

    @Test
    void negativeElapsedTimeCannotEnterWindup() {
        assertEquals(Phase.IDLE, CreatureActionPolicy.phase(ActionType.ENVOY_BACKSTEP, -1));
    }

    @Test
    void cooldownAndHostilityBothGateActionStart() {
        assertFalse(CreatureActionPolicy.canStart(100, 101, true));
        assertFalse(CreatureActionPolicy.canStart(100, 100, false));
        assertTrue(CreatureActionPolicy.canStart(100, 100, true));
    }

    @Test
    void actionIdsFailClosed() {
        assertEquals(ActionType.NONE, ActionType.byId(-1));
        assertEquals(ActionType.NONE, ActionType.byId(99));
    }

    @Test
    void actionTimingsRemainBounded() {
        for (ActionType action : ActionType.values()) {
            assertTrue(action.totalTicks() >= 0);
            assertTrue(action.totalTicks() <= 40);
            assertTrue(action.cooldownTicks() >= action.totalTicks());
        }
    }
}
