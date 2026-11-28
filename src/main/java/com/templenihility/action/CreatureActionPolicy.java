package com.templenihility.action;

public final class CreatureActionPolicy {

    public enum ActionType {
        NONE(0, 0, 0, 0),
        FOLLOWER_INTERCEPT(6, 4, 6, 48),
        ENVOY_BACKSTEP(4, 1, 6, 45),
        ENVOY_RIFT_DIRECTION(8, 1, 8, 65),
        GRAND_ENVOY_DOMAIN(14, 1, 10, 180),
        GRAND_ENVOY_RECALL(10, 1, 8, 130),
        GRAND_ENVOY_SHIELD(12, 1, 10, 140),
        SAINT_ERASURE_LINE(14, 1, 8, 52),
        SAINT_MISSING_ZONE(12, 1, 8, 62),
        SAINT_VOID_TERMINUS(12, 1, 8, 140);

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

        public int recoveryTicks() {
            return recoveryTicks;
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
