package com.templenihility.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    public static final ModConfigSpec COMMON_SPEC;

    // 圣殿生成配置
    public static final ModConfigSpec.IntValue SHRINE_WEIGHT;
    public static final ModConfigSpec.IntValue TEMPLE_WEIGHT;
    public static final ModConfigSpec.IntValue COMPLEX_WEIGHT;

    // 生物配置
    public static final ModConfigSpec.IntValue BELIEVER_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue FOLLOWER_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue ENVOY_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue GRAND_ENVOY_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue SAINT_SPAWN_CHANCE;

    // 交易配置
    public static final ModConfigSpec.DoubleValue TRADE_PRICE_MULTIPLIER;
    public static final ModConfigSpec.IntValue TRADE_COOLDOWN;

    // 宝库配置
    public static final ModConfigSpec.IntValue VAULT_MAX_NETWORK_BLOCKS;
    public static final ModConfigSpec.IntValue VAULT_MAX_FORCED_CHUNKS;

    // 传输配置
    public static final ModConfigSpec.IntValue TRANSPORT_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue TRANSPORT_ITEMS_PER_OPERATION;
    public static final ModConfigSpec.IntValue TRANSPORT_FLUID_PER_OPERATION;
    public static final ModConfigSpec.IntValue TRANSPORT_ENERGY_PER_OPERATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("圣殿生成配置").push("structure");

        SHRINE_WEIGHT = builder
            .comment("神龛生成权重 (越大越常见)")
            .defineInRange("shrine_weight", 60, 1, 100);

        TEMPLE_WEIGHT = builder
            .comment("圣殿生成权重")
            .defineInRange("temple_weight", 30, 1, 100);

        COMPLEX_WEIGHT = builder
            .comment("圣殿群生成权重")
            .defineInRange("complex_weight", 10, 1, 100);

        builder.pop();

        builder.comment("生物生成配置").push("entity");

        BELIEVER_SPAWN_CHANCE = builder
            .comment("信徒生成概率 (%)")
            .defineInRange("believer_spawn_chance", 30, 1, 100);

        FOLLOWER_SPAWN_CHANCE = builder
            .comment("从者生成概率 (%)")
            .defineInRange("follower_spawn_chance", 25, 1, 100);

        ENVOY_SPAWN_CHANCE = builder
            .comment("令使生成概率 (%)")
            .defineInRange("envoy_spawn_chance", 20, 1, 100);

        GRAND_ENVOY_SPAWN_CHANCE = builder
            .comment("大令使生成概率 (%)")
            .defineInRange("grand_envoy_spawn_chance", 15, 1, 100);

        SAINT_SPAWN_CHANCE = builder
            .comment("圣灵生成概率 (%)")
            .defineInRange("saint_spawn_chance", 10, 1, 100);

        builder.pop();

        builder.comment("交易配置").push("trade");

        TRADE_PRICE_MULTIPLIER = builder
            .comment("交易价格倍率")
            .defineInRange("trade_price_multiplier", 1.0, 0.1, 10.0);

        TRADE_COOLDOWN = builder
            .comment("交易冷却时间 (tick)")
            .defineInRange("trade_cooldown", 20, 0, 200);

        builder.pop();

        builder.comment("虚空宝库安全配置").push("vault");

        VAULT_MAX_NETWORK_BLOCKS = builder
            .comment("单个相连宝库网络允许扫描的最大宝库方块数")
            .defineInRange("max_network_blocks", 256, 1, 4096);

        VAULT_MAX_FORCED_CHUNKS = builder
            .comment("单个宝库网络允许常加载的最大区块数，设为 0 可禁用常加载")
            .defineInRange("max_forced_chunks", 9, 0, 64);

        builder.pop();

        builder.comment("虚空传输核心性能与吞吐配置").push("transport");

        TRANSPORT_INTERVAL_TICKS = builder
            .comment("传输核心执行一次搬运的间隔 tick；20 tick 约为 1 秒")
            .defineInRange("interval_ticks", 20, 5, 200);

        TRANSPORT_ITEMS_PER_OPERATION = builder
            .comment("每次搬运最多传输的物品数量")
            .defineInRange("items_per_operation", 16, 1, 1024);

        TRANSPORT_FLUID_PER_OPERATION = builder
            .comment("每次搬运最多传输的流体量，单位 mB")
            .defineInRange("fluid_per_operation", 250, 1, 64000);

        TRANSPORT_ENERGY_PER_OPERATION = builder
            .comment("每次搬运最多传输的 NeoForge 能量")
            .defineInRange("energy_per_operation", 256, 1, 1000000);

        builder.pop();

        COMMON_SPEC = builder.build();
    }
}
