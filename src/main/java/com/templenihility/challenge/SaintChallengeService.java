package com.templenihility.challenge;

import com.templenihility.challenge.SaintChallengePolicy.EndReason;
import com.templenihility.challenge.SaintChallengePolicy.StartStatus;
import com.templenihility.entity.NihilitySaint;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SaintChallengeService {
    public record Inspection(StartStatus status, NihilitySaint saint) {
    }

    public static Inspection inspect(ServerLevel level, BlockPos altarPos, ServerPlayer player, int altarTier) {
        NihilitySaint ownChallenge = nearest(level, player.blockPosition(), SaintChallengePolicy.ACTIVE_SEARCH_RADIUS,
            saint -> saint.isChallengeParticipant(player));
        if (ownChallenge != null) {
            return new Inspection(StartStatus.OWN_CHALLENGE, ownChallenge);
        }

        List<NihilitySaint> nearby = nearby(level, altarPos, SaintChallengePolicy.START_SEARCH_RADIUS);
        NihilitySaint available = nearby.stream()
            .filter(saint -> saint.canBeginRitualChallenge(player))
            .min(Comparator.comparingDouble(saint -> saint.distanceToSqr(Vec3.atCenterOf(altarPos))))
            .orElse(null);
        StartStatus status = SaintChallengePolicy.evaluateStart(
            altarTier,
            isEligible(player),
            nearby.size(),
            false,
            available != null);
        return new Inspection(status, available);
    }

    public static boolean toggle(ServerLevel level, BlockPos altarPos, ServerPlayer player, int altarTier) {
        Inspection inspection = inspect(level, altarPos, player, altarTier);
        if (inspection.status() == StartStatus.OWN_CHALLENGE && inspection.saint() != null) {
            inspection.saint().cancelRitualChallenge(player, EndReason.VOLUNTARY);
            return true;
        }
        if (inspection.status() == StartStatus.READY && inspection.saint() != null) {
            return inspection.saint().beginRitualChallenge(player);
        }
        player.sendSystemMessage(Component.translatable(messageKey(inspection.status())));
        return false;
    }

    public static String messageKey(StartStatus status) {
        return switch (status) {
            case READY -> "screen.templenihility.altar_challenge_ready_tip";
            case OWN_CHALLENGE -> "screen.templenihility.altar_challenge_quit_tip";
            case TIER_TOO_LOW -> "screen.templenihility.altar_challenge_tier_tip";
            case PLAYER_INVALID -> "screen.templenihility.altar_challenge_player_tip";
            case NO_SAINT -> "screen.templenihility.altar_challenge_missing_tip";
            case SAINT_BUSY -> "screen.templenihility.altar_challenge_busy_tip";
        };
    }

    public static boolean isEligible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private static List<NihilitySaint> nearby(ServerLevel level, BlockPos pos, double radius) {
        AABB bounds = new AABB(pos).inflate(radius, 16.0, radius);
        return level.getEntitiesOfClass(NihilitySaint.class, bounds, NihilitySaint::isAlive);
    }

    private static NihilitySaint nearest(ServerLevel level, BlockPos pos, double radius,
                                         java.util.function.Predicate<NihilitySaint> predicate) {
        return nearby(level, pos, radius).stream()
            .filter(predicate)
            .min(Comparator.comparingDouble(saint -> saint.distanceToSqr(Vec3.atCenterOf(pos))))
            .orElse(null);
    }

    private SaintChallengeService() {
    }
}
