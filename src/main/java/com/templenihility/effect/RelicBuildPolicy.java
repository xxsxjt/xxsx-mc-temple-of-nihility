package com.templenihility.effect;

/**
 * Pure gameplay calculation policy for Four-Aspect weapons, Nihility Armor set tiers,
 * and Curios accessory build synergies. Kept free of Minecraft runtime dependencies so
 * every build formula is verified by unit tests.
 */
public final class RelicBuildPolicy {

    public enum DeathSaveChoice {
        NONE,
        CHAOS_CROWN,
        SOUL_ANCHOR
    }

    public static final int CHAOS_CROWN_ENERGY_COST = 60;
    public static final int CHAOS_GAUNTLET_MAX_STACKS = 5;
    public static final long CHAOS_GAUNTLET_WINDOW_TICKS = 100L;
    public static final float SIPHON_SHIELD_CAP = 12.0F;

    /**
     * Resolves lethal-save priority so the shorter-cooldown Chaos Crown (120s + 60 VoidPower)
     * always triggers before the 10-minute Soul Anchor, preventing double consumption.
     */
    public static DeathSaveChoice resolveDeathSave(boolean hasCrown, boolean crownReady, int voidPower,
                                                   boolean hasSoulAnchor, boolean soulAnchorReady) {
        if (hasCrown && crownReady && voidPower >= CHAOS_CROWN_ENERGY_COST) {
            return DeathSaveChoice.CHAOS_CROWN;
        }
        if (hasSoulAnchor && soulAnchorReady) {
            return DeathSaveChoice.SOUL_ANCHOR;
        }
        return DeathSaveChoice.NONE;
    }

    /**
     * Chaos Blood Ring: +8% damage per 10% missing HP, capped at +56% (1.56x).
     */
    public static float chaosBloodDamageMultiplier(float currentHealth, float maxHealth) {
        if (maxHealth <= 0.0F) {
            return 1.0F;
        }
        float clampedFraction = Math.max(0.0F, Math.min(1.0F, currentHealth / maxHealth));
        float missingFraction = 1.0F - clampedFraction;
        int steps = Math.min(7, (int) Math.floor(missingFraction * 10.0F));
        return 1.0F + steps * 0.08F;
    }

    /**
     * Chaos Gauntlet: builds 1..5 consecutive combo stacks within 5s (100 ticks).
     */
    public static int nextChaosGauntletStacks(int previousStacks, long ticksSinceLastHit) {
        if (ticksSinceLastHit > CHAOS_GAUNTLET_WINDOW_TICKS || ticksSinceLastHit < 0L) {
            return 1;
        }
        int clamped = Math.max(0, previousStacks);
        return clamped >= CHAOS_GAUNTLET_MAX_STACKS ? 1 : clamped + 1;
    }

    /**
     * Siphon Ring: 22% of real health damage healed (max 4.0); any overheal above maxHealth
     * converts 100% into Abyssal Soul Shield up to cap 12.0.
     */
    public static float[] computeSiphonHealAndShield(float realHealthDamage, float currentHealth,
                                                     float maxHealth, float currentShield) {
        if (realHealthDamage <= 0.0F || maxHealth <= 0.0F) {
            return new float[] { 0.0F, 0.0F };
        }
        float rawHeal = Math.min(4.0F, realHealthDamage * 0.22F);
        float missingHp = Math.max(0.0F, maxHealth - currentHealth);
        float directHeal = Math.min(rawHeal, missingHp);
        float overflow = Math.max(0.0F, rawHeal - directHeal);
        float shieldGain = Math.max(0.0F, Math.min(SIPHON_SHIELD_CAP - Math.max(0.0F, currentShield), overflow));
        return new float[] { directHeal, shieldGain };
    }

    /**
     * Rift Dagger: rear 120-degree sector check (dot >= 0.5 between target look and attack vector).
     */
    public static boolean isBackstab(double attackerX, double attackerZ,
                                     double targetX, double targetZ,
                                     double targetLookX, double targetLookZ) {
        double toTargetX = targetX - attackerX;
        double toTargetZ = targetZ - attackerZ;
        double attackLenSq = toTargetX * toTargetX + toTargetZ * toTargetZ;
        double lookLenSq = targetLookX * targetLookX + targetLookZ * targetLookZ;
        if (attackLenSq <= 1.0E-6 || lookLenSq <= 1.0E-6) {
            return false;
        }
        double invAttack = 1.0 / Math.sqrt(attackLenSq);
        double invLook = 1.0 / Math.sqrt(lookLenSq);
        double dot = (toTargetX * invAttack) * (targetLookX * invLook)
            + (toTargetZ * invAttack) * (targetLookZ * invLook);
        return dot >= 0.5;
    }

    /**
     * Blade of Zero: Execute bonus triggers when target has 3 Existence stacks OR <= 35% HP.
     */
    public static float zeroSlashExecuteBonus(float currentHealth, float maxHealth, int existenceStacks) {
        if (maxHealth <= 0.0F) {
            return 0.0F;
        }
        boolean thresholdMet = existenceStacks >= 3 || (currentHealth / maxHealth) <= 0.35F;
        if (!thresholdMet) {
            return 0.0F;
        }
        float missing = Math.max(0.0F, maxHealth - currentHealth);
        return Math.min(24.0F, 4.0F + missing * 0.15F);
    }

    /**
     * Rift Dagger Missing Mark detonation bonus: 6.0 + 8% target max HP (capped at +18 bonus).
     */
    public static float missingMarkExecuteBonus(float targetMaxHealth) {
        float pct = Math.min(18.0F, Math.max(0.0F, targetMaxHealth) * 0.08F);
        return 6.0F + pct;
    }

    /**
     * Nihility Armor set VoidPower capacity bonus: +25 for 2-piece, +50 for 4-piece.
     */
    public static int armorSetVoidPowerBonus(int equippedPieces) {
        if (equippedPieces >= 4) {
            return 50;
        }
        if (equippedPieces >= 2) {
            return 25;
        }
        return 0;
    }

    private RelicBuildPolicy() {
    }
}
