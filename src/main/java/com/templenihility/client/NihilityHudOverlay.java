package com.templenihility.client;

import com.templenihility.effect.CombatFeedbackPolicy.ToastEntry;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

public final class NihilityHudOverlay {
    private static final int BAR_WIDTH = 94;
    private static final int BAR_HEIGHT = 21;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) {
            return;
        }
        Player player = mc.player;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        int edgeTicks = NihilityHudState.edgeFlashTicks();
        if (edgeTicks > 0) {
            float ea = Math.min(1.0F, edgeTicks / 9.0F);
            int rgb = NihilityHudState.edgeFlashColor() & 0x00FFFFFF;
            int outer = withAlpha(rgb, (int) (ea * 135.0F));
            int inner = withAlpha(rgb, (int) (ea * 48.0F));
            int marginY = Math.max(12, h / 8);
            graphics.fill(0, marginY, 2, h - marginY, outer);
            graphics.fill(2, marginY + 8, 4, h - marginY - 8, inner);
            graphics.fill(w - 2, marginY, w, h - marginY, outer);
            graphics.fill(w - 4, marginY + 8, w - 2, h - marginY - 8, inner);
        }

        if (!NihilityHudState.shouldRenderBar(player)) {
            return;
        }

        Font font = mc.font;
        float alpha = NihilityHudState.hudAlpha();

        int x = Math.min(w - BAR_WIDTH - 6, w / 2 + 99);
        int y = h - BAR_HEIGHT - 2;

        int bgColor = withAlpha(0x090B0E, (int) (alpha * 212.0F));
        int borderRgb = NihilityHudState.overdriveTicks() > 0
            ? 0xF59E0B
            : (NihilityHudState.counterTicks() > 0 ? 0xFDE68A : 0x475569);
        int borderColor = withAlpha(borderRgb, (int) (alpha * 225.0F));
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, bgColor);
        graphics.outline(x, y, BAR_WIDTH, BAR_HEIGHT, borderColor);

        if (NihilityHudState.counterTicks() > 0) {
            int cw = Math.max(1, (int) ((BAR_WIDTH - 2) * Math.min(1.0F, NihilityHudState.counterTicks() / 80.0F)));
            graphics.fill(x + 1, y + 1, x + 1 + cw, y + 2, withAlpha(0xFDE68A, (int) (alpha * 245.0F)));
        } else if (NihilityHudState.overdriveTicks() > 0) {
            int ow = Math.max(1, (int) ((BAR_WIDTH - 2) * Math.min(1.0F, NihilityHudState.overdriveTicks() / 160.0F)));
            graphics.fill(x + 1, y + 1, x + 1 + ow, y + 2, withAlpha(0xF59E0B, (int) (alpha * 245.0F)));
        }

        int power = NihilityHudState.power();
        int maxPower = NihilityHudState.maxPower();
        String energyText = power + "/" + maxPower;
        int labelCol = withAlpha(NihilityHudState.energyFlashTicks() > 0 ? 0xFFFFFF : 0xE2E8F0,
            (int) (alpha * 245.0F));
        graphics.text(font, energyText, x + 5, y + 4, labelCol, false);

        if (NihilityHudState.counterTicks() > 0) {
            String tag = "反击";
            int tw = font.width(tag);
            graphics.text(font, tag, x + BAR_WIDTH - tw - 4, y + 4,
                withAlpha(0xFDE68A, (int) (alpha * 255.0F)), false);
        } else if (NihilityHudState.overdriveTicks() > 0) {
            String tag = "过载";
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
            String tag = "盾" + sh;
            int tw = font.width(tag);
            graphics.text(font, tag, x + BAR_WIDTH - tw - 4, y + 4,
                withAlpha(0xFCA5A5, (int) (alpha * 240.0F)), false);
        } else if (NihilityHudState.wardCooldownTicks() == 0) {
            graphics.fill(x + BAR_WIDTH - 8, y + 6, x + BAR_WIDTH - 5, y + 9,
                withAlpha(0xC4B5FD, (int) (alpha * 230.0F)));
        }

        int trackX = x + 4;
        int trackY = y + BAR_HEIGHT - 6;
        int trackW = BAR_WIDTH - 8;
        graphics.fill(trackX, trackY, trackX + trackW, trackY + 3, withAlpha(0x1E293B, (int) (alpha * 220.0F)));
        float ratio = Math.max(0.0F, Math.min(1.0F, power / (float) maxPower));
        int fillW = (int) Math.round(trackW * ratio);
        if (fillW > 0) {
            int fillCore = NihilityHudState.energyFlashTicks() > 0 ? 0xDDD6FE : 0x8B5CF6;
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 3, withAlpha(fillCore, (int) (alpha * 245.0F)));
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 1, withAlpha(0xF8FAFC, (int) (alpha * 230.0F)));
        }

        List<ToastEntry> toasts = NihilityHudState.toasts();
        int toastY = y - 16;
        for (ToastEntry entry : toasts) {
            float ta = entry.alpha();
            if (ta <= 0.04F) {
                continue;
            }
            String label = entry.formattedLabel();
            String delta = entry.energyDelta() > 0
                ? "+" + entry.energyDelta()
                : (entry.energyDelta() < 0 ? String.valueOf(entry.energyDelta()) : "");
            int accentRgb = entry.kind().accentArgb & 0x00FFFFFF;

            graphics.fill(x, toastY, x + BAR_WIDTH, toastY + 14, withAlpha(0x090B0E, (int) (ta * 215.0F)));
            graphics.fill(x, toastY, x + 2, toastY + 14, withAlpha(accentRgb, (int) (ta * 255.0F)));
            graphics.outline(x, toastY, BAR_WIDTH, 14, withAlpha(0x334155, (int) (ta * 170.0F)));
            graphics.text(font, label, x + 5, toastY + 3, withAlpha(0xF1F5F9, (int) (ta * 250.0F)), false);
            if (!delta.isEmpty()) {
                int dw = font.width(delta);
                int dRgb = entry.energyDelta() > 0 ? 0xC4B5FD : 0x94A3B8;
                graphics.text(font, delta, x + BAR_WIDTH - dw - 4, toastY + 3,
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
