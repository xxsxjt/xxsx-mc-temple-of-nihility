package com.templenihility.action;

public final class CreatureActionPolicy {
    public enum ActionType {
        NONE(0, 0, 0, 0),
        FOLLOWER_INTERCEPT(8, 3, 10, 80),
        ENVOY_BACKSTEP(5, 1, 8, 70),
        ENVOY_RIFT_DIRECTION(12, 1, 10, 120),
        GRAND_ENVOY_DOMAIN(20, 1, 14, 300),
        GRAND_ENVOY_RECALL(12, 1, 12, 220),
        GRAND_ENVOY_SHIELD(16, 1, 18, 260),
        SAINT_ERASURE_LINE(20, 1, 12, 100),
        SAINT_MISSING_ZONE(16, 1, 12, 120),
        SAINT_VOID_TERMINUS(14, 1, 10, 300);

        private final int windupTicks;
        private final int activeTicks;
        private final int recoveryTicks;
        private final int cooldownTicks;

        ActionType(int windupTicks, int activeTicks, int recoveryTicks, int cooldownTicks) {
            this.windupTicks = windupTicks;
            this.activeTicks = activeTicks;
            this.recoveryTicks = recoveryTicks;
            this.cooldownTicks = cooldownTicks;
        }

        public int windupTicks() {
            return windupTicks;
        }

        public int activeTicks() {
            return activeTicks;
        }

        public int totalTicks() {
            return windupTicks + activeTicks + recoveryTicks;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public static ActionType byId(int id) {
            ActionType[] values = values();
            return id >= 0 && id < values.length ? values[id] : NONE;
        }
    }

    public enum Phase {
        IDLE,
        WINDUP,
        ACTIVE,
        RECOVERY;

        public static Phase byId(int id) {
            Phase[] values = values();
            return id >= 0 && id < values.length ? values[id] : IDLE;
        }
    }

    public static Phase phase(ActionType action, long elapsedTicks) {
        if (action == ActionType.NONE || elapsedTicks < 0) {
            return Phase.IDLE;
        }
        if (elapsedTicks < action.windupTicks()) {
            return Phase.WINDUP;
        }
        if (elapsedTicks < action.windupTicks() + action.activeTicks()) {
            return Phase.ACTIVE;
        }
        return elapsedTicks < action.totalTicks() ? Phase.RECOVERY : Phase.IDLE;
    }

    public static boolean isFinished(ActionType action, long elapsedTicks) {
        return action == ActionType.NONE || elapsedTicks >= action.totalTicks();
    }

    public static boolean canStart(long gameTime, long nextAllowedGameTime, boolean hostileTarget) {
        return hostileTarget && gameTime >= nextAllowedGameTime;
    }

    private CreatureActionPolicy() {
    }
}
