package com.templenihility.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.templenihility.ritual.RitualExecutionPolicy.Status;
import org.junit.jupiter.api.Test;

class RitualExecutionPolicyTest {
    @Test
    void reportsMissingInputBeforeAnyRecipeState() {
        assertEquals(Status.NO_INPUT, evaluate(false, false, 0, 4, 0, 1, false, 0, 5));
    }

    @Test
    void reportsUnknownMaterials() {
        assertEquals(Status.NO_RECIPE, evaluate(true, false, 64, 1, 3, 0, true, 100, 0));
    }

    @Test
    void materialShortageHasOneConsistentPriority() {
        assertEquals(Status.NOT_ENOUGH_INPUT, evaluate(true, true, 3, 4, 0, 2, false, 0, 50));
    }

    @Test
    void structureIsCheckedBeforeOutputAndEnergy() {
        assertEquals(Status.TIER_TOO_LOW, evaluate(true, true, 4, 4, 1, 2, false, 0, 50));
    }

    @Test
    void blockedOutputDoesNotReachEnergyCharging() {
        assertEquals(Status.OUTPUT_BLOCKED, evaluate(true, true, 4, 4, 2, 2, false, 100, 50));
    }

    @Test
    void insufficientEnergyIsTheLastBlockedState() {
        assertEquals(Status.NOT_ENOUGH_ENERGY, evaluate(true, true, 4, 4, 2, 2, true, 49, 50));
    }

    @Test
    void readyRequiresEveryPredicate() {
        assertEquals(Status.READY, evaluate(true, true, 4, 4, 2, 2, true, 50, 50));
    }

    private static Status evaluate(boolean hasInput, boolean hasRecipe, int inputCount, int requiredInput,
                                   int tier, int requiredTier, boolean outputFits, int energy, int requiredEnergy) {
        return RitualExecutionPolicy.evaluate(hasInput, hasRecipe, inputCount, requiredInput,
            tier, requiredTier, outputFits, energy, requiredEnergy);
    }
}
