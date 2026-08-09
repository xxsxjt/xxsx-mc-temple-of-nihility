package com.templenihility.action;

import com.templenihility.action.CreatureActionPolicy.ActionType;

public final class SaintActionPolicy {
    public static final double ERASURE_LINE_LENGTH = 18.0;
    public static final double ERASURE_LINE_HALF_WIDTH = 1.15;
    public static final float ERASURE_LINE_DAMAGE = 14.0F;
    public static final double MISSING_ZONE_RADIUS = 2.2;
    public static final int MISSING_ZONE_DURATION_TICKS = 6 * 20;
    public static final float MISSING_ZONE_DAMAGE = 4.0F;

    // 归零终式：低生命阶段招式。多段安全区逐步缩小，玩家必须随安全区收缩逃向中心。
    // 安全区规律可读（半径线性收缩），不做全屏随机秒杀。
    public static final double LOW_HEALTH_FRACTION = 0.30;
    public static final double TERMINUS_START_RADIUS = 12.0;
    public static final double TERMINUS_END_RADIUS = 2.0;
    public static final int TERMINUS_DURATION_TICKS = 6 * 20;
    public static final float TERMINUS_MAX_DAMAGE = 6.0F;
    public static final int TERMINUS_ACTIVE_EVERY = 3;

    public static ActionType select(int sequence, double targetDistanceSquared) {
        return select(sequence, targetDistanceSquared, 1.0);
    }

    public static ActionType select(int sequence, double targetDistanceSquared, double healthFraction) {
        if (!Double.isFinite(targetDistanceSquared)
            || targetDistanceSquared > ERASURE_LINE_LENGTH * ERASURE_LINE_LENGTH) {
            return ActionType.NONE;
        }
        boolean lowHealth = Double.isFinite(healthFraction)
            && healthFraction <= LOW_HEALTH_FRACTION;
        if (lowHealth && Math.floorMod(sequence, TERMINUS_ACTIVE_EVERY) == 0) {
            return ActionType.SAINT_VOID_TERMINUS;
        }
        return Math.floorMod(sequence, 2) == 0
            ? ActionType.SAINT_ERASURE_LINE
            : ActionType.SAINT_MISSING_ZONE;
    }

    /** 归零终式当前安全半径：随进度从起始半径线性收缩到终态半径。 */
    public static double terminusSafeRadius(double progress) {
        if (!Double.isFinite(progress)) {
            return TERMINUS_END_RADIUS;
        }
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return TERMINUS_START_RADIUS + (TERMINUS_END_RADIUS - TERMINUS_START_RADIUS) * clamped;
    }

    /** 归零终式当前伤害：安全区收缩越紧，对区外伤害越高。 */
    public static float terminusDamage(double progress) {
        if (!Double.isFinite(progress)) {
            return TERMINUS_MAX_DAMAGE;
        }
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return (float) (1.5 + (TERMINUS_MAX_DAMAGE - 1.5) * clamped);
    }

    /** 给定进度与到中心距离，判断玩家是否处于安全区内。 */
    public static boolean isInTerminusSafeZone(double distanceSquared, double progress) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0.0) {
            return false;
        }
        double radius = terminusSafeRadius(progress);
        return distanceSquared <= radius * radius;
    }

    public static boolean lineHits(double pointX, double pointZ, double originX, double originZ,
                                   double directionX, double directionZ) {
        double directionLengthSquared = directionX * directionX + directionZ * directionZ;
        if (!Double.isFinite(directionLengthSquared) || directionLengthSquared <= 1.0E-8) {
            return false;
        }
        double inverseLength = 1.0 / Math.sqrt(directionLengthSquared);
        double dx = directionX * inverseLength;
        double dz = directionZ * inverseLength;
        double relativeX = pointX - originX;
        double relativeZ = pointZ - originZ;
        double projection = relativeX * dx + relativeZ * dz;
        if (projection < 0.0 || projection > ERASURE_LINE_LENGTH) {
            return false;
        }
        double perpendicularX = relativeX - projection * dx;
        double perpendicularZ = relativeZ - projection * dz;
        return perpendicularX * perpendicularX + perpendicularZ * perpendicularZ
            <= ERASURE_LINE_HALF_WIDTH * ERASURE_LINE_HALF_WIDTH;
    }

    public static boolean zoneContains(double pointX, double pointZ, double centerX, double centerZ) {
        double dx = pointX - centerX;
        double dz = pointZ - centerZ;
        return dx * dx + dz * dz <= MISSING_ZONE_RADIUS * MISSING_ZONE_RADIUS;
    }

    private SaintActionPolicy() {
    }
}
