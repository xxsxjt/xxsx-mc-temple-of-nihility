package com.templenihility.action;

import com.templenihility.action.CreatureActionPolicy.ActionType;

public final class GrandEnvoyActionPolicy {
    public static final int SHIELD_CHARGES = 3;
    public static final float SHIELD_DAMAGE_MULTIPLIER = 0.45F;
    public static final int DOMAIN_DURATION_TICKS = 12 * 20;
    public static final double DOMAIN_RADIUS = 9.0;
    public static final int DOMAIN_TELEPORT_COST = 10;
    public static final double DOMAIN_SPRINT_DRAG = 0.88;
    public static final float DOMAIN_SPRINT_EXHAUSTION = 0.08F;
    public static final int MAX_RECALLED_ALLIES = 3;

    public static ActionType select(double healthRatio, int shieldCharges, boolean hasRecallCandidate,
                                    double targetDistanceSquared) {
        double health = Double.isFinite(healthRatio) ? Math.max(0.0, Math.min(1.0, healthRatio)) : 1.0;
        if (shieldCharges <= 0 && health <= 0.65) {
            return ActionType.GRAND_ENVOY_SHIELD;
        }
        if (hasRecallCandidate) {
            return ActionType.GRAND_ENVOY_RECALL;
        }
        if (targetDistanceSquared >= 3.0 * 3.0 && targetDistanceSquared <= 14.0 * 14.0) {
            return ActionType.GRAND_ENVOY_DOMAIN;
        }
        return ActionType.NONE;
    }

    public static float shieldedDamage(float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            return 0.0F;
        }
        return amount * SHIELD_DAMAGE_MULTIPLIER;
    }

    public static boolean taxesTeleport(boolean sourceInside, boolean targetInside) {
        return sourceInside || targetInside;
    }

    private GrandEnvoyActionPolicy() {
    }
}
