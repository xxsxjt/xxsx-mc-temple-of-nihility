package com.templenihility.effect;

public final class VoidPhaseStateResolver {
    public record Snapshot(boolean mayFly, boolean flying, boolean noGravity,
                           boolean noPhysics, boolean invisible) {
    }

    public record RestoredState(boolean mayFly, boolean flying, boolean noGravity,
                                boolean noPhysics, boolean invisible) {
    }

    public static RestoredState resolve(Snapshot snapshot, boolean spectator,
                                        boolean currentMayFly, boolean currentFlying,
                                        boolean hasVanillaInvisibility) {
        boolean mayFly = spectator ? currentMayFly : snapshot.mayFly();
        boolean flying = spectator ? currentFlying : snapshot.mayFly() && snapshot.flying();
        return new RestoredState(
            mayFly,
            flying,
            snapshot.noGravity(),
            spectator || snapshot.noPhysics(),
            snapshot.invisible() || hasVanillaInvisibility
        );
    }

    private VoidPhaseStateResolver() {
    }
}
