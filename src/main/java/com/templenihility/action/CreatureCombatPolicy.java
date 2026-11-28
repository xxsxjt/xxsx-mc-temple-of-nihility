package com.templenihility.action;

/**
 * Pure combat durability, super-armor poise, and Void Erosion scaling policy for Temple of Nihility
 * creatures across Tiers 1..5. Designed so high-tier guardians (Envoy / Grand Envoy / Saint) stand
 * their ground against modded bosses without getting one-shot or stunlock-cancelled, while keeping
 * player duels fair and readable.
 */
public final class CreatureCombatPolicy {

    public static final float MAX_MOB_VOID_EROSION_PER_HIT = 220.0F;
    public static final float MAX_PLAYER_VOID_EROSION_PER_HIT = 6.0F;

    private static int clampTier(int tier) {
        return Math.max(1, Math.min(5, tier));
    }

    /** Single-hit damage cap as a fraction of max HP (prevents modded one-shots). */
    public static float singleHitDamageCap(int tier, float maxHealth) {
        if (!Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
            return 1.0F;
        }
        float fraction = switch (clampTier(tier)) {
            case 1 -> 0.35F;
            case 2 -> 0.28F;
            case 3 -> 0.20F;
            case 4 -> 0.12F;
            default -> 0.06F;
        };
        return Math.max(4.0F, maxHealth * fraction);
    }

    /** Flat Void Phase damage reduction multiplier (lower = stronger defense). */
    public static float incomingDamageMultiplier(int tier, double healthFraction) {
        int t = clampTier(tier);
        double hp = Double.isFinite(healthFraction) ? Math.max(0.0, Math.min(1.0, healthFraction)) : 1.0;
        return switch (t) {
            case 1 -> 1.00F;
            case 2 -> 0.90F;
            case 3 -> 0.82F;
            case 4 -> hp <= 0.50 ? 0.62F : 0.72F;
            default -> hp <= 0.50 ? 0.45F : 0.62F;
        };
    }

    /** Computes final incoming damage after Void Phase mitigation and single-hit cap. */
    public static float mitigatedDamage(int tier, float incomingDamage, float maxHealth, double healthFraction) {
        if (!Float.isFinite(incomingDamage) || incomingDamage <= 0.0F) {
            return 0.0F;
        }
        float reduced = incomingDamage * incomingDamageMultiplier(tier, healthFraction);
        float cap = singleHitDamageCap(tier, maxHealth);
        return Math.min(reduced, cap);
    }

    /**
     * Super-armor poise check during skill windup:
     * Tier 3+ (Envoy / Grand Envoy / Saint) have full Void Super Armor and cannot be stunlock-cancelled.
     * Tier 1-2 require a massive poise-breaking blow (>= 35% max HP) to interrupt.
     */
    public static boolean canInterruptWindup(int tier, float mitigatedDamage, float maxHealth) {
        if (clampTier(tier) >= 3) {
            return false;
        }
        if (!Float.isFinite(mitigatedDamage) || !Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
            return false;
        }
        return mitigatedDamage >= maxHealth * 0.35F;
    }

    /**
     * Armor-piercing Void Erosion bonus damage. Scales with target max HP against high-HP modded
     * bosses while staying strictly bounded against players for fair duels.
     */
    public static float voidErosionBonus(int tier, float targetMaxHealth, boolean targetIsPlayer,
                                         float skillMultiplier) {
        int t = clampTier(tier);
        float mult = Float.isFinite(skillMultiplier) ? Math.max(0.25F, Math.min(3.0F, skillMultiplier)) : 1.0F;
        if (targetIsPlayer) {
            return Math.min(MAX_PLAYER_VOID_EROSION_PER_HIT, t * 0.75F * mult);
        }
        float maxHp = Float.isFinite(targetMaxHealth) ? Math.max(0.0F, targetMaxHealth) : 0.0F;
        float percent = switch (t) {
            case 1 -> 0.015F;
            case 2 -> 0.025F;
            case 3 -> 0.035F;
            case 4 -> 0.050F;
            default -> 0.065F;
        };
        float scaled = Math.max(t * 1.5F, maxHp * percent) * mult;
        return Math.min(MAX_MOB_VOID_EROSION_PER_HIT, scaled);
    }

    /** Automatic close-range Void Repulse counter damage when Tier 3+ takes rapid melee hits. */
    public static float retaliationBurstDamage(int tier, float attackDamage) {
        float atk = Float.isFinite(attackDamage) ? Math.max(4.0F, attackDamage) : 8.0F;
        return atk * (clampTier(tier) >= 5 ? 1.15F : 0.90F);
    }

    private CreatureCombatPolicy() {
    }
}
