package com.templenihility.ritual;

public final class RitualExecutionPolicy {
    public enum Status {
        READY,
        NO_INPUT,
        NO_RECIPE,
        NOT_ENOUGH_INPUT,
        TIER_TOO_LOW,
        OUTPUT_BLOCKED,
        NOT_ENOUGH_ENERGY;

        public static Status byId(int id) {
            Status[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public static Status evaluate(
        boolean hasInput,
        boolean hasRecipe,
        int inputCount,
        int requiredInput,
        int structureTier,
        int requiredTier,
        boolean outputFits,
        int availableEnergy,
        int requiredEnergy
    ) {
        if (!hasInput) {
            return Status.NO_INPUT;
        }
        if (!hasRecipe) {
            return Status.NO_RECIPE;
        }
        if (inputCount < requiredInput) {
            return Status.NOT_ENOUGH_INPUT;
        }
        if (structureTier < requiredTier) {
            return Status.TIER_TOO_LOW;
        }
        if (!outputFits) {
            return Status.OUTPUT_BLOCKED;
        }
        if (availableEnergy < requiredEnergy) {
            return Status.NOT_ENOUGH_ENERGY;
        }
        return Status.READY;
    }

    private RitualExecutionPolicy() {
    }
}
