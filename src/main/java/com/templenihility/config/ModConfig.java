package com.templenihility.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    public static final ModConfigSpec COMMON_SPEC;

    // 圣殿生成配置
    public static final ModConfigSpec.IntValue SHRINE_WEIGHT;
    public static final ModConfigSpec.IntValue TEMPLE_WEIGHT;
    public static final ModConfigSpec.IntValue COMPLEX_WEIGHT;
    public static final ModConfigSpec.IntValue MIN_SPAWN_DISTANCE;
    public static final ModConfigSpec.IntValue MAX_SPAWN_DISTANCE;

    // 生物配置
    public static final ModConfigSpec.IntValue BELIEVER_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue FOLLOWER_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue ENVOY_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue GRAND_ENVOY_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue SAINT_SPAWN_CHANCE;

    // 交易配置
    public static final ModConfigSpec.DoubleValue TRADE_PRICE_MULTIPLIER;
    public static final ModConfigSpec.IntValue TRADE_COOLDOWN;

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

        MIN_SPAWN_DISTANCE = builder
            .comment("最小生成距离 (方块)")
            .defineInRange("min_spawn_distance", 500, 100, 5000);

        MAX_SPAWN_DISTANCE = builder
            .comment("最大生成距离 (方块)")
            .defineInRange("max_spawn_distance", 2000, 500, 10000);

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

        COMMON_SPEC = builder.build();
    }
}
