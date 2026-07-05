package com.templenihility.world;

import com.templenihility.TempleNihilityMod;
import com.templenihility.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.HashSet;
import java.util.Set;
import net.neoforged.neoforge.event.level.ChunkEvent;

public class ModWorldEvents {

    // 防重复生成：记录已处理区块
    private static final Set<Long> GENERATED_CHUNKS = new HashSet<>();

    // TODO: 改用 Structure 系统或 BiomeModifier 代替 ChunkEvent.Load
    // ChunkEvent.Load 会因 setBlock 触发相邻区块加载导致无限递归
    // @SubscribeEvent
    public static void onChunkLoad_Disabled(ChunkEvent.Load event) {
        // 只处理服务器端、已完全加载的区块
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        // 防重复：同一区块只处理一次
        long chunkKey = chunk.getPos().pack();
        if (!GENERATED_CHUNKS.add(chunkKey)) return;

        // 生成概率 0.5%
        if (level.getRandom().nextInt(1000) >= 5) return;

        // 确定类型
        TempleType type;
        int roll = level.getRandom().nextInt(100);
        int complexW = ModConfig.COMPLEX_WEIGHT.get();
        int templeW = ModConfig.TEMPLE_WEIGHT.get();
        if (roll < complexW) type = TempleType.TEMPLE_COMPLEX;
        else if (roll < complexW + templeW) type = TempleType.TEMPLE;
        else type = TempleType.SHRINE;

        // 地表高度
        int cx = chunk.getPos().getMiddleBlockX();
        int cz = chunk.getPos().getMiddleBlockZ();
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        if (y < 63) return; // 水下跳过

        try {
            TempleGenerator.generateTemple(level, new BlockPos(cx, y, cz), type);
        } catch (Exception e) {
            TempleNihilityMod.LOGGER.error("圣殿生成失败 at ({},{},{}): {}", cx, y, cz, e.getMessage());
        }
    }
}
