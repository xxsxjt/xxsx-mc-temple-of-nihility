package com.templenihility.client;

import com.templenihility.effect.CombatFeedbackPolicy;
import com.templenihility.effect.CombatFeedbackPolicy.Kind;
import com.templenihility.effect.CombatFeedbackPolicy.ToastEntry;
import com.templenihility.item.NihilityArmor;
import com.templenihility.item.NihilityAxe;
import com.templenihility.item.NihilityDagger;
import com.templenihility.item.NihilityGreatsword;
import com.templenihility.item.NihilitySword;
import com.templenihility.network.CombatFeedbackPayload;
import com.templenihility.network.NihilityHudPayload;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class NihilityHudState {
    private static int power = 0;
    private static int maxPower = 100;
    private static int armorPieces = 0;
    private static int counterTicks = 0;
    private static int gauntletStacks = 0;
    private static int wardCooldownTicks = -1;
    private static int overdriveTicks = 0;
    private static int soulShieldTenths = 0;
    private static int ticksSinceChange = 999;
    private static int energyFlashTicks = 0;
    private static int edgeFlashTicks = 0;
    private static int edgeFlashColor = 0xFFE2E8F0;
    private static boolean hasSynced = false;
    private static List<ToastEntry> toasts = Collections.emptyList();

    public static void applyHudSync(NihilityHudPayload payload) {
        if (hasSynced && payload.power() != power) {
            ticksSinceChange = 0;
            energyFlashTicks = 8;
        }
        power = payload.power();
        maxPower = Math.max(1, payload.maxPower());
        armorPieces = payload.armorPieces();
        counterTicks = payload.counterTicks();
        gauntletStacks = payload.gauntletStacks();
        wardCooldownTicks = payload.wardCooldownTicks();
        overdriveTicks = payload.overdriveTicks();
        soulShieldTenths = payload.soulShieldTenths();
        if (counterTicks > 0 || gauntletStacks > 0 || overdriveTicks > 0 || soulShieldTenths > 0) {
            ticksSinceChange = 0;
        }
        hasSynced = true;
    }

    public static void applyCombatFeedback(CombatFeedbackPayload payload) {
        Kind kind = Kind.byId(payload.kindId());
        toasts = CombatFeedbackPolicy.mergeOrInsert(
            toasts, kind, payload.detail(), payload.energyDelta(), CombatFeedbackPolicy.MAX_VISIBLE_TOASTS);
        ticksSinceChange = 0;
        if (payload.energyDelta() != 0) {
            energyFlashTicks = 8;
        }
        if (kind.edgeFlash) {
            edgeFlashTicks = 9;
            edgeFlashColor = kind.accentArgb;
        }
    }

    public static void clientTick() {
        ticksSinceChange = Math.min(9999, ticksSinceChange + 1);
        if (energyFlashTicks > 0) energyFlashTicks--;
        if (edgeFlashTicks > 0) edgeFlashTicks--;
        if (counterTicks > 0) counterTicks--;
        if (overdriveTicks > 0) overdriveTicks--;
        if (wardCooldownTicks > 0) wardCooldownTicks--;
        if (!toasts.isEmpty()) {
            toasts = CombatFeedbackPolicy.tickToasts(toasts);
        }
    }

    public static void reset() {
        power = 0;
        maxPower = 100;
        armorPieces = 0;
        counterTicks = 0;
        gauntletStacks = 0;
        wardCooldownTicks = -1;
        overdriveTicks = 0;
        soulShieldTenths = 0;
        ticksSinceChange = 999;
        energyFlashTicks = 0;
        edgeFlashTicks = 0;
        hasSynced = false;
        toasts = Collections.emptyList();
    }

    public static boolean shouldRenderBar(Player player) {
        if (!toasts.isEmpty() || counterTicks > 0 || gauntletStacks > 0 || overdriveTicks > 0 || soulShieldTenths > 0) {
            return true;
        }
        if (armorPieces >= 2 || power > 0 || maxPower > 100) {
            return true;
        }
        var main = player.getMainHandItem().getItem();
        if (main instanceof NihilitySword || main instanceof NihilityGreatsword
            || main instanceof NihilityDagger || main instanceof NihilityAxe) {
            return true;
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).getItem() instanceof NihilityArmor) {
                return true;
            }
        }
        return false;
    }

    public static int power() { return power; }
    public static int maxPower() { return maxPower; }
    public static int armorPieces() { return armorPieces; }
    public static int counterTicks() { return counterTicks; }
    public static int gauntletStacks() { return gauntletStacks; }
    public static int wardCooldownTicks() { return wardCooldownTicks; }
    public static int overdriveTicks() { return overdriveTicks; }
    public static int soulShieldTenths() { return soulShieldTenths; }
    public static int energyFlashTicks() { return energyFlashTicks; }
    public static int edgeFlashTicks() { return edgeFlashTicks; }
    public static int edgeFlashColor() { return edgeFlashColor; }
    public static List<ToastEntry> toasts() { return toasts; }

    public static float hudAlpha() {
        boolean activeBuff = !toasts.isEmpty() || counterTicks > 0 || gauntletStacks > 0
            || overdriveTicks > 0 || soulShieldTenths > 0;
        return CombatFeedbackPolicy.computeHudAlpha(power, maxPower, activeBuff, ticksSinceChange);
    }

    private NihilityHudState() {
    }
}
