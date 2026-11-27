package com.templenihility.effect;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure policy for non-spam combat HUD notifications, phase-dodge audio throttling,
 * and energy HUD opacity modulation. Kept Minecraft-free for direct unit testing.
 */
public final class CombatFeedbackPolicy {
    public static final int MAX_VISIBLE_TOASTS = 3;
    public static final long DODGE_AUDIO_MIN_INTERVAL_TICKS = 10L;
    public static final int IDLE_DIM_AFTER_TICKS = 100;

    public enum Kind {
        PHASE_DODGE(0, 0xFFE2E8F0, 34, true, "相位闪避"),
        ZERO_COUNTER(1, 0xFFFDE68A, 34, false, "归零反击"),
        WARD_MEMBRANE(2, 0xFFC4B5FD, 30, false, "归零薄膜"),
        AEGIS_SHIELD(3, 0xFFA78BFA, 44, true, "相位庇护"),
        GAUNTLET_BURST(4, 0xFFF59E0B, 36, false, "五连崩灭"),
        FATE_CRIT(5, 0xFFFBBF24, 28, false, "命定暴击"),
        ECLIPSE_AMBUSH(6, 0xFFE2E8F0, 32, false, "蚀影伏击"),
        SENTINEL_DEFLECT(7, 0xFF38BDF8, 30, false, "哨兵偏折"),
        ABYSS_SLAM(8, 0xFFEF4444, 34, false, "深渊重压"),
        CHAOS_CROWN(9, 0xFFF59E0B, 64, true, "死境过载"),
        SOUL_ANCHOR(10, 0xFFDC2626, 64, true, "魂锚免死"),
        ZERO_EXECUTE(11, 0xFFE2E8F0, 34, false, "归零斩杀"),
        DAGGER_MARK(12, 0xFFC4B5FD, 30, false, "缺口引爆"),
        AXE_SHATTER(13, 0xFFE2E8F0, 30, false, "破界碎甲"),
        RECOVERY_ORB(14, 0xFFF87171, 44, true, "复苏护主");

        public final int id;
        public final int accentArgb;
        public final int durationTicks;
        public final boolean edgeFlash;
        public final String labelZh;

        Kind(int id, int accentArgb, int durationTicks, boolean edgeFlash, String labelZh) {
            this.id = id;
            this.accentArgb = accentArgb;
            this.durationTicks = durationTicks;
            this.edgeFlash = edgeFlash;
            this.labelZh = labelZh;
        }

        public static Kind byId(int id) {
            for (Kind k : values()) {
                if (k.id == id) {
                    return k;
                }
            }
            return PHASE_DODGE;
        }
    }

    public record ToastEntry(
        Kind kind,
        String detail,
        int count,
        int energyDelta,
        int ageTicks,
        int maxTicks
    ) {
        public String formattedLabel() {
            String base = (detail != null && !detail.isEmpty()) ? detail : kind.labelZh;
            return count > 1 ? base + " ×" + count : base;
        }

        public float alpha() {
            int remaining = maxTicks - ageTicks;
            if (remaining <= 0) {
                return 0.0F;
            }
            if (remaining < 8) {
                return remaining / 8.0F;
            }
            return 1.0F;
        }
    }

    public static List<ToastEntry> mergeOrInsert(List<ToastEntry> existing, Kind kind,
                                                 String detail, int energyDelta, int maxVisible) {
        List<ToastEntry> next = new ArrayList<>(existing.size() + 1);
        boolean merged = false;
        for (ToastEntry entry : existing) {
            if (entry.kind() == kind && !merged) {
                next.add(new ToastEntry(
                    kind,
                    (detail != null && !detail.isEmpty()) ? detail : entry.detail(),
                    entry.count() + 1,
                    entry.energyDelta() + energyDelta,
                    0,
                    kind.durationTicks
                ));
                merged = true;
            } else {
                next.add(entry);
            }
        }
        if (!merged) {
            next.add(0, new ToastEntry(kind, detail == null ? "" : detail, 1, energyDelta, 0, kind.durationTicks));
        }
        while (next.size() > Math.max(1, maxVisible)) {
            next.remove(next.size() - 1);
        }
        return next;
    }

    public static List<ToastEntry> tickToasts(List<ToastEntry> existing) {
        List<ToastEntry> next = new ArrayList<>(existing.size());
        for (ToastEntry entry : existing) {
            int nextAge = entry.ageTicks() + 1;
            if (nextAge < entry.maxTicks()) {
                next.add(new ToastEntry(
                    entry.kind(), entry.detail(), entry.count(), entry.energyDelta(), nextAge, entry.maxTicks()
                ));
            }
        }
        return next;
    }

    public static boolean shouldPlayDodgeAudio(long nowTick, long lastAudioTick) {
        return lastAudioTick < 0L || (nowTick - lastAudioTick) >= DODGE_AUDIO_MIN_INTERVAL_TICKS;
    }

    public static float computeHudAlpha(int power, int maxPower, boolean hasActiveBuff, int ticksSinceChange) {
        if (hasActiveBuff || power < maxPower || ticksSinceChange < IDLE_DIM_AFTER_TICKS) {
            return 1.0F;
        }
        int over = ticksSinceChange - IDLE_DIM_AFTER_TICKS;
        return Math.max(0.48F, 1.0F - over * 0.025F);
    }

    private CombatFeedbackPolicy() {
    }
}
