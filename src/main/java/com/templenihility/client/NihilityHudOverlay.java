package com.templenihility.client;

import com.templenihility.effect.CombatFeedbackPolicy;
import com.templenihility.effect.CombatFeedbackPolicy.ToastEntry;
import java.util.List;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Compact Nihility Energy HUD anchored right of the hotbar + coalesced non-spam
 * combat micro-toasts and subtle peripheral edge shimmer for Phase Dodge / death saves.
 * Fully bilingual (en_us / zh_cn) and respects vanilla Hotbar Attack Indicator spacing.
 */
public final class NihilityHudOverlay {
    private static final int BAR_WIDTH = 94;
    private static final int BAR_HEIGHT = 21;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        // 1. Subtle 2px peripheral screen-edge shimmer on Phase Dodge / Aegis / Death Save
        int edgeTicks = NihilityHudState.edgeFlashTicks();
        if (edgeTicks > 0) {
            float ratio = edgeTicks / 9.0F;
            int edgeAlpha = (int) (ratio * 110.0F);
            int edgeCol = withAlpha(NihilityHudState.edgeFlashColor(), edgeAlpha);
            graphics.fill(0, 0, w, 2, edgeCol);
            graphics.fill(0, h - 2, w, h, edgeCol);
            graphics.fill(0, 2, 2, h - 2, edgeCol);
            graphics.fill(w - 2, 2, w, h - 2, edgeCol);
        }

        if (!NihilityHudState.shouldRenderBar(player)) {
            return;
        }

        Font font = mc.font;
        float alpha = NihilityHudState.hudAlpha();
        boolean hasHotbarIndicator = mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
        int x = CombatFeedbackPolicy.computeHudAnchorX(w, BAR_WIDTH, hasHotbarIndicator);
        int y = h - 23;

        int power = NihilityHudState.power();
        int maxPower = Math.max(1, NihilityHudState.maxPower());
        boolean lowEnergy = CombatFeedbackPolicy.isLowEnergy(power, maxPower);
        boolean flashing = NihilityHudState.energyFlashTicks() > 0;
        int bgCol = withAlpha(0x090B0E, (int) (alpha * 208.0F));
        int borderRgb = flashing ? 0xE2E8F0 : (lowEnergy ? 0xBE123C : 0x475569);
        int borderCol = withAlpha(borderRgb, (int) (alpha * 225.0F));

        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, bgCol);
        graphics.outline(x, y, BAR_WIDTH, BAR_HEIGHT, borderCol);

        // Energy numeric readout (left)
        String energyText = power + "/" + maxPower;
        int labelCol = withAlpha(flashing ? 0xFFFFFF : (lowEnergy ? 0xFDA4AF : 0xE2E8F0), (int) (alpha * 245.0F));
        graphics.text(font, energyText, x + 5, y + 4, labelCol, false);

        // Compact active state badge (right side of bar header)
        if (NihilityHudState.counterTicks() > 0) {
            int sec = Math.max(1, (NihilityHudState.counterTicks() + 19) / 20);
            String tag = Component.translatable("hud.templenihility.counter", sec).getString();
            int tw = font.width(tag);
            graphics.text(font, tag, x + BAR_WIDTH - tw - 4, y + 4,
                withAlpha(0xFDE68A, (int) (alpha * 255.0F)), false);
        } else if (NihilityHudState.overdriveTicks() > 0) {
            int sec = Math.max(1, (NihilityHudState.overdriveTicks() + 19) / 20);
            String tag = Component.translatable("hud.templenihility.overdrive", sec).getString();
            int tw = font.width(tag);
            graphics.text(font, tag, x + BAR_WIDTH - tw - 4, y + 4,
                withAlpha(0xF59E0B, (int) (alpha * 255.0F)), false);
        } else if (NihilityHudState.gauntletStacks() > 0) {
            int stacks = NihilityHudState.gauntletStacks();
            int pipBaseX = x + BAR_WIDTH - 24;
            for (int i = 0; i < 5; i++) {
                int px = pipBaseX + i * 4;
                int pc = i < stacks
                    ? withAlpha(stacks == 5 ? 0xFDE68A : 0xF59E0B, (int) (alpha * 255.0F))
                    : withAlpha(0x334155, (int) (alpha * 180.0F));
                graphics.fill(px, y + 6, px + 3, y + 9, pc);
            }
        } else if (NihilityHudState.soulShieldTenths() > 0) {
            int sh = Math.max(1, (NihilityHudState.soulShieldTenths() + 5) / 10);
            String tag = Component.translatable("hud.templenihility.shield", sh).getString();
            int tw = font.width(tag);
            graphics.text(font, tag, x + BAR_WIDTH - tw - 4, y + 4,
                withAlpha(0xFCA5A5, (int) (alpha * 240.0F)), false);
        } else if (NihilityHudState.wardCooldownTicks() == 0) {
            graphics.fill(x + BAR_WIDTH - 8, y + 6, x + BAR_WIDTH - 5, y + 9,
                withAlpha(0xC4B5FD, (int) (alpha * 230.0F)));
        }

        // Crisp 3px void-energy progress bar with cold-white sheen
        int trackX = x + 4;
        int trackY = y + BAR_HEIGHT - 6;
        int trackW = BAR_WIDTH - 8;
        graphics.fill(trackX, trackY, trackX + trackW, trackY + 3, withAlpha(0x1E293B, (int) (alpha * 220.0F)));
        float ratio = Math.max(0.0F, Math.min(1.0F, power / (float) maxPower));
        int fillW = (int) Math.round(trackW * ratio);
        if (fillW > 0) {
            int fillCore = flashing ? 0xDDD6FE : (lowEnergy ? 0xE11D48 : 0x8B5CF6);
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 3, withAlpha(fillCore, (int) (alpha * 245.0F)));
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 1, withAlpha(0xF8FAFC, (int) (alpha * 230.0F)));
        }

        // 2. Stacked coalesced combat micro-toasts directly above the energy bar (max 3)
        List<ToastEntry> toasts = NihilityHudState.toasts();
        int toastY = y - 16;
        for (ToastEntry entry : toasts) {
            float ta = entry.alpha();
            if (ta <= 0.04F) {
                continue;
            }
            String localizedBase = Component.translatable(entry.kind().translationKey).getString();
            String label = entry.formatWithLabel(localizedBase);
            String delta = entry.energyDelta() > 0
                ? "+" + entry.energyDelta()
                : (entry.energyDelta() < 0 ? String.valueOf(entry.energyDelta()) : "");
            int neededW = font.width(label) + (delta.isEmpty() ? 10 : font.width(delta) + 14);
            int toastW = Math.min(Math.max(BAR_WIDTH, neededW), Math.max(BAR_WIDTH, w - x - 4));
            int accentRgb = entry.kind().accentArgb & 0x00FFFFFF;
            graphics.fill(x, toastY, x + toastW, toastY + 14, withAlpha(0x090B0E, (int) (ta * 215.0F)));
            graphics.fill(x, toastY, x + 2, toastY + 14, withAlpha(accentRgb, (int) (ta * 255.0F)));
            graphics.outline(x, toastY, toastW, 14, withAlpha(0x334155, (int) (ta * 170.0F)));
            graphics.text(font, label, x + 5, toastY + 3, withAlpha(0xF1F5F9, (int) (ta * 250.0F)), false);
            if (!delta.isEmpty()) {
                int dw = font.width(delta);
                int dRgb = entry.energyDelta() > 0 ? 0xC4B5FD : 0x94A3B8;
                graphics.text(font, delta, x + toastW - dw - 4, toastY + 3,
                    withAlpha(dRgb, (int) (ta * 245.0F)), false);
            }
            toastY -= 16;
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | (rgb & 0x00FFFFFF);
    }

    private NihilityHudOverlay() {
    }
}
