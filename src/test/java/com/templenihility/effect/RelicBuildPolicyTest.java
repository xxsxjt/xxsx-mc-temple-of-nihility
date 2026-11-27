package com.templenihility.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.templenihility.effect.RelicBuildPolicy.DeathSaveChoice;
import org.junit.jupiter.api.Test;

class RelicBuildPolicyTest {

    @Test
    void deathSavePrefersChaosCrownBeforeSoulAnchor() {
        assertEquals(DeathSaveChoice.CHAOS_CROWN,
            RelicBuildPolicy.resolveDeathSave(true, true, 60, true, true));
        assertEquals(DeathSaveChoice.SOUL_ANCHOR,
            RelicBuildPolicy.resolveDeathSave(true, true, 40, true, true));
        assertEquals(DeathSaveChoice.SOUL_ANCHOR,
            RelicBuildPolicy.resolveDeathSave(true, false, 100, true, true));
        assertEquals(DeathSaveChoice.NONE,
            RelicBuildPolicy.resolveDeathSave(true, false, 100, true, false));
    }

    @Test
    void chaosBloodScalesWithMissingHealthAndCaps() {
        assertEquals(1.0F, RelicBuildPolicy.chaosBloodDamageMultiplier(20.0F, 20.0F), 0.001F);
        assertEquals(1.40F, RelicBuildPolicy.chaosBloodDamageMultiplier(10.0F, 20.0F), 0.001F);
        assertEquals(1.56F, RelicBuildPolicy.chaosBloodDamageMultiplier(1.0F, 20.0F), 0.001F);
    }

    @Test
    void chaosGauntletBuildsAndResetsComboStacks() {
        assertEquals(1, RelicBuildPolicy.nextChaosGauntletStacks(0, 10L));
        assertEquals(5, RelicBuildPolicy.nextChaosGauntletStacks(4, 30L));
        assertEquals(1, RelicBuildPolicy.nextChaosGauntletStacks(5, 10L));
        assertEquals(1, RelicBuildPolicy.nextChaosGauntletStacks(4, 120L));
    }

    @Test
    void siphonConvertsOverhealIntoSoulShieldUpToCap() {
        float[] fullHp = RelicBuildPolicy.computeSiphonHealAndShield(10.0F, 20.0F, 20.0F, 10.5F);
        assertEquals(0.0F, fullHp[0], 0.001F);
        assertEquals(1.5F, fullHp[1], 0.001F);

        float[] partial = RelicBuildPolicy.computeSiphonHealAndShield(10.0F, 19.0F, 20.0F, 0.0F);
        assertEquals(1.0F, partial[0], 0.001F);
        assertEquals(1.2F, partial[1], 0.001F);
    }

    @Test
    void backstabDetectsRearConeAccurately() {
        // Target at (0, 10) facing +Z (0, 1); attacker behind at (0, 8)
        assertTrue(RelicBuildPolicy.isBackstab(0.0, 8.0, 0.0, 10.0, 0.0, 1.0));
        // Attacker in front at (0, 12)
        assertFalse(RelicBuildPolicy.isBackstab(0.0, 12.0, 0.0, 10.0, 0.0, 1.0));
    }

    @Test
    void zeroExecuteAndMissingMarkAndArmorBonusesMatchSpec() {
        assertEquals(0.0F, RelicBuildPolicy.zeroSlashExecuteBonus(80.0F, 100.0F, 2), 0.001F);
        assertEquals(7.0F, RelicBuildPolicy.zeroSlashExecuteBonus(80.0F, 100.0F, 3), 0.001F);
        assertEquals(14.5F, RelicBuildPolicy.zeroSlashExecuteBonus(30.0F, 100.0F, 0), 0.001F);

        assertEquals(14.0F, RelicBuildPolicy.missingMarkExecuteBonus(100.0F), 0.001F);
        assertEquals(24.0F, RelicBuildPolicy.missingMarkExecuteBonus(500.0F), 0.001F);

        assertEquals(0, RelicBuildPolicy.armorSetVoidPowerBonus(1));
        assertEquals(25, RelicBuildPolicy.armorSetVoidPowerBonus(2));
        assertEquals(50, RelicBuildPolicy.armorSetVoidPowerBonus(4));
    }
}
