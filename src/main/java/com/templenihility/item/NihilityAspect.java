package com.templenihility.item;

import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum NihilityAspect {
    NIHILITY("tooltip.templenihility.aspect.nihility", ChatFormatting.WHITE),
    VOID("tooltip.templenihility.aspect.void", ChatFormatting.DARK_AQUA),
    CHAOS("tooltip.templenihility.aspect.chaos", ChatFormatting.LIGHT_PURPLE),
    ABYSS("tooltip.templenihility.aspect.abyss", ChatFormatting.DARK_PURPLE);

    private static final Map<String, NihilityAspect> BY_TOOLTIP = Map.ofEntries(
        Map.entry("tooltip.templenihility.nihility_vault_expansion", VOID),
        Map.entry("tooltip.templenihility.nihility_lantern", VOID),
        Map.entry("tooltip.templenihility.nihility_recovery_orb", ABYSS),
        Map.entry("tooltip.templenihility.nihility_shadow_sigil", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_purifying_bell", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_echo_lens", VOID),
        Map.entry("tooltip.templenihility.nihility_barrier_core", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_phase_feather", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_gravity_sigil", VOID),
        Map.entry("tooltip.templenihility.nihility_war_horn", ABYSS),
        Map.entry("tooltip.templenihility.nihility_soul_flask", ABYSS),
        Map.entry("tooltip.templenihility.nihility_null_scroll", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_stasis_watch", VOID),
        Map.entry("tooltip.templenihility.nihility_rift_snare", VOID),
        Map.entry("tooltip.templenihility.nihility_abyssal_drum", ABYSS),
        Map.entry("tooltip.templenihility.nihility_void_beacon", VOID),
        Map.entry("tooltip.templenihility.nihility_terminal", VOID),
        Map.entry("tooltip.templenihility.nihility_ring", CHAOS),
        Map.entry("tooltip.templenihility.nihility_amulet", VOID),
        Map.entry("tooltip.templenihility.nihility_belt", VOID),
        Map.entry("tooltip.templenihility.nihility_cloak", VOID),
        Map.entry("tooltip.templenihility.nihility_charm", CHAOS),
        Map.entry("tooltip.templenihility.nihility_magnet", VOID),
        Map.entry("tooltip.templenihility.nihility_regenerator", ABYSS),
        Map.entry("tooltip.templenihility.nihility_miner_charm", VOID),
        Map.entry("tooltip.templenihility.nihility_ward", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_gauntlet", CHAOS),
        Map.entry("tooltip.templenihility.nihility_hourglass", VOID),
        Map.entry("tooltip.templenihility.nihility_soul_anchor", ABYSS),
        Map.entry("tooltip.templenihility.nihility_rift_ring", VOID),
        Map.entry("tooltip.templenihility.nihility_eclipse_amulet", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_aegis_charm", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_wayfinder", VOID),
        Map.entry("tooltip.templenihility.nihility_star_compass", VOID),
        Map.entry("tooltip.templenihility.nihility_crown", CHAOS),
        Map.entry("tooltip.templenihility.nihility_abyss_mantle", ABYSS),
        Map.entry("tooltip.templenihility.nihility_siphon_ring", ABYSS),
        Map.entry("tooltip.templenihility.nihility_pearl_belt", VOID),
        Map.entry("tooltip.templenihility.nihility_sentinel_eye", VOID),
        Map.entry("tooltip.templenihility.nihility_trader_seal", CHAOS),
        Map.entry("tooltip.templenihility.nihility_waystone_missing", VOID),
        Map.entry("tooltip.templenihility.nihility_power_charm", NIHILITY),
        Map.entry("tooltip.templenihility.nihility_conduit_charm", NIHILITY)
    );

    private final String translationKey;
    private final ChatFormatting style;

    NihilityAspect(String translationKey, ChatFormatting style) {
        this.translationKey = translationKey;
        this.style = style;
    }

    public Component line() {
        return Component.translatable(translationKey).withStyle(style);
    }

    public static NihilityAspect fromTooltip(String tooltipKey) {
        return BY_TOOLTIP.getOrDefault(tooltipKey, NIHILITY);
    }
}
