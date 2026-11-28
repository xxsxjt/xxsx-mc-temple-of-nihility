package com.templenihility.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreatureCombatPolicyTest {

    @Test
    void saintCannotBeOneShotByExtremeModdedDamage() {
        float capped = CreatureCombatPolicy.mitigatedDamage(5, 9999.0F, 960.0F, 1.0);
        assertTrue(capped <= 960.0F * 0.06F + 0.01F);
        assertTrue(capped > 0.0F);
    }

    @Test
    void highTierEntitiesHaveUninterruptibleSuperArmorDuringWindup() {
        assertFalse(CreatureCombatPolicy.canInterruptWindup(3, 100.0F, 280.0F));
        assertFalse(CreatureCombatPolicy.canInterruptWindup(4, 200.0F, 520.0F));
        assertFalse(CreatureCombatPolicy.canInterruptWindup(5, 500.0F, 960.0F));
        assertFalse(CreatureCombatPolicy.canInterruptWindup(2, 10.0F, 160.0F));
        assertTrue(CreatureCombatPolicy.canInterruptWindup(2, 60.0F, 160.0F));
    }

    @Test
    void voidErosionScalesAgainstHighHpBossesWhileStayingCappedForPlayers() {
        float vsBoss = CreatureCombatPolicy.voidErosionBonus(5, 2000.0F, false, 1.5F);
        float vsPlayer = CreatureCombatPolicy.voidErosionBonus(5, 40.0F, true, 1.5F);
        assertTrue(vsBoss >= 100.0F);
        assertTrue(vsBoss <= CreatureCombatPolicy.MAX_MOB_VOID_EROSION_PER_HIT);
        assertTrue(vsPlayer <= CreatureCombatPolicy.MAX_PLAYER_VOID_EROSION_PER_HIT);
        assertEquals(0.0F, CreatureCombatPolicy.mitigatedDamage(5, -5.0F, 960.0F, 1.0));
    }

    @Test
    void awakenedSaintReceivesStrongerMitigationBelowHalfHealth() {
        float fullHp = CreatureCombatPolicy.mitigatedDamage(5, 60.0F, 960.0F, 1.0);
        float awakened = CreatureCombatPolicy.mitigatedDamage(5, 60.0F, 960.0F, 0.4);
        assertTrue(awakened < fullHp);
    }
}
