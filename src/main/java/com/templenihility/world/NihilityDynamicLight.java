package com.templenihility.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class NihilityDynamicLight {
    private static final int LIGHT_LEVEL = 14;
    private static final Map<UUID, TrackedLight> LIGHTS = new HashMap<>();

    public static void update(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        TrackedLight old = LIGHTS.get(player.getUUID());
        BlockPos target = findTarget(level, player, old);
        if (target == null) {
            clear(player);
            return;
        }

        TrackedLight next = new TrackedLight(level.dimension(), target.immutable());
        if (next.equals(old)) {
            return;
        }

        clear(player);
        if (canPlace(level, target)) {
            BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL);
            level.setBlock(target, light, 3);
            LIGHTS.put(player.getUUID(), next);
        }
    }

    public static void clear(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        TrackedLight light = LIGHTS.remove(player.getUUID());
        if (light != null) {
            clear(level.getServer(), light);
        }
    }

    private static BlockPos findTarget(ServerLevel level, Player player, TrackedLight old) {
        BlockPos base = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        BlockPos[] candidates = {base, player.blockPosition().above(), player.blockPosition(), base.above()};
        for (BlockPos candidate : candidates) {
            if (canUseCandidate(level, candidate, old)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean canUseCandidate(ServerLevel level, BlockPos pos, TrackedLight old) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir()
            || old != null && old.dimension().equals(level.dimension()) && old.pos().equals(pos) && state.is(Blocks.LIGHT);
    }

    private static boolean canPlace(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).isAir();
    }

    private static void clear(MinecraftServer server, TrackedLight light) {
        ServerLevel level = server.getLevel(light.dimension());
        if (level == null || !level.isLoaded(light.pos()) || isTrackedByAnotherPlayer(light)) {
            return;
        }
        if (level.getBlockState(light.pos()).is(Blocks.LIGHT)) {
            level.setBlock(light.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static boolean isTrackedByAnotherPlayer(TrackedLight light) {
        for (TrackedLight other : LIGHTS.values()) {
            if (light.equals(other)) {
                return true;
            }
        }
        return false;
    }

    private record TrackedLight(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private NihilityDynamicLight() {
    }
}
