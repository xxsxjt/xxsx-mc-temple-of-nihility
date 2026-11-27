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

    @Test
    void tieredArmorProgressionAndMixedSetCarryover() {
        var none = RelicBuildPolicy.tieredArmorSetBonus(1, 0, 0);
        assertEquals(0, none.vpMaxBonus());
        assertEquals(0.0, none.dodgeChance(), 0.0001);

        var t1Full = RelicBuildPolicy.tieredArmorSetBonus(4, 0, 0);
        assertEquals(1, t1Full.fourPieceTier());
        assertEquals(30, t1Full.vpMaxBonus());
        assertEquals(0.10, t1Full.dodgeChance(), 0.0001);
        assertFalse(t1Full.hasZeroCounter());

        var t2Full = RelicBuildPolicy.tieredArmorSetBonus(0, 4, 0);
        assertEquals(2, t2Full.fourPieceTier());
        assertEquals(55, t2Full.vpMaxBonus());
        assertEquals(0.16, t2Full.dodgeChance(), 0.0001);
        assertEquals(2, t2Full.autoRepairIntervalSeconds());

        var t3Full = RelicBuildPolicy.tieredArmorSetBonus(0, 0, 4);
        assertEquals(3, t3Full.fourPieceTier());
        assertEquals(80, t3Full.vpMaxBonus());
        assertEquals(0.25, t3Full.dodgeChance(), 0.0001);
        assertEquals(1, t3Full.autoRepairIntervalSeconds());
        assertTrue(t3Full.hasZeroCounter());
        assertTrue(t3Full.hasDebuffDecay());

        // Upgrading piece-by-piece: 2 Shade + 2 Nihility -> 2pc Tier 3 regen & debuff decay, 4pc Tier 2 dodge!
        var mixed22 = RelicBuildPolicy.tieredArmorSetBonus(0, 2, 2);
        assertEquals(3, mixed22.twoPieceTier());
        assertEquals(2, mixed22.fourPieceTier());
        assertEquals(55, mixed22.vpMaxBonus());
        assertTrue(mixed22.hasDebuffDecay());
        assertEquals(0.16, mixed22.dodgeChance(), 0.0001);

        // 3 Stillstone + 1 Shade preserves full 4pc Tier 1 set bonus while upgrading!
        var mixed31 = RelicBuildPolicy.tieredArmorSetBonus(3, 1, 0);
        assertEquals(1, mixed31.twoPieceTier());
        assertEquals(1, mixed31.fourPieceTier());
        assertEquals(30, mixed31.vpMaxBonus());
        assertEquals(0.10, mixed31.dodgeChance(), 0.0001);
    }

    @Test
    void lethalZeroExecuteHonorsBossImmunityAndStacks() {
        assertTrue(RelicBuildPolicy.shouldLethalZeroExecute(24.0F, 100.0F, 0, false));
        assertFalse(RelicBuildPolicy.shouldLethalZeroExecute(30.0F, 100.0F, 0, false));
        assertTrue(RelicBuildPolicy.shouldLethalZeroExecute(34.0F, 100.0F, 3, false));
        assertFalse(RelicBuildPolicy.shouldLethalZeroExecute(10.0F, 100.0F, 3, true));
        assertFalse(RelicBuildPolicy.shouldLethalZeroExecute(0.0F, 100.0F, 3, false));
    }

    @Test
    void boundaryWaveDamageEscalatesAcrossAllThreeWaves() {
        float w0 = RelicBuildPolicy.boundaryWaveDamageMultiplier(0);
        float w1 = RelicBuildPolicy.boundaryWaveDamageMultiplier(1);
        float w2 = RelicBuildPolicy.boundaryWaveDamageMultiplier(2);
        assertTrue(w0 > 0.0F && w1 > w0 && w2 > w1);
        assertTrue(RelicBuildPolicy.boundaryWaveFlatBonus(2) > RelicBuildPolicy.boundaryWaveFlatBonus(1));
        assertTrue(RelicBuildPolicy.boundaryWaveFlatBonus(1) > RelicBuildPolicy.boundaryWaveFlatBonus(0));
    }
}
