package com.templenihility.action;

public final class CreatureProjectileDefensePolicy {
    public static final double FOLLOWER_GUARD_RADIUS = 6.0;
    public static final double FOLLOWER_GUARD_HEALTH_THRESHOLD = 0.5;
    public static final int FOLLOWER_GUARD_COOLDOWN_TICKS = 12 * 20;
    public static final int FOLLOWER_GUARD_REACTION_TICKS = 10;
    public static final int ENVOY_DEFLECT_COOLDOWN_TICKS = 6 * 20;
    public static final int ENVOY_DEFLECT_REACTION_TICKS = 8;
    public static final double MIN_PROJECTILE_SPEED_SQUARED = 0.01;

    public static boolean followerCanGuard(int protectedTier, double healthRatio,
                                           boolean guardReady, boolean friendlyProjectile) {
        double health = Double.isFinite(healthRatio) ? healthRatio : 1.0;
        return protectedTier <= 2
            && health <= FOLLOWER_GUARD_HEALTH_THRESHOLD
            && guardReady
            && !friendlyProjectile;
    }

    public static boolean envoyCanDeflect(boolean deflectReady, double projectileSpeedSquared,
                                          boolean friendlyProjectile) {
        return deflectReady
            && Double.isFinite(projectileSpeedSquared)
            && projectileSpeedSquared >= MIN_PROJECTILE_SPEED_SQUARED
            && !friendlyProjectile;
    }

    private CreatureProjectileDefensePolicy() {
    }
}
