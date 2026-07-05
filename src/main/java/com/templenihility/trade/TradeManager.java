package com.templenihility.trade;

import com.templenihility.init.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TradeManager {
    private static final List<TradeOffer> TIER_1_OFFERS = new ArrayList<>();
    private static final List<TradeOffer> TIER_2_OFFERS = new ArrayList<>();
    private static final List<TradeOffer> TIER_3_OFFERS = new ArrayList<>();
    private static final List<TradeOffer> TIER_4_OFFERS = new ArrayList<>();
    private static final List<TradeOffer> TIER_5_OFFERS = new ArrayList<>();

    private static final Random RANDOM = new Random();

    static {
        // 1级交易 - 基础物资
        TIER_1_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_SHARD.get(), 5),
            new ItemStack(Items.BREAD, 8), 1));
        TIER_1_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_SHARD.get(), 3),
            new ItemStack(Items.APPLE, 6), 1));
        TIER_1_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_SHARD.get(), 10),
            new ItemStack(Items.IRON_INGOT, 3), 1));

        // 2级交易 - 中级物资
        TIER_2_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_SHARD.get(), 15),
            new ItemStack(Items.DIAMOND, 1), 2));
        TIER_2_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_SHARD.get(), 20),
            new ItemStack(Items.EMERALD, 4), 2));
        TIER_2_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_SHARD.get(), 25),
            new ItemStack(Items.GOLDEN_APPLE, 2), 2));

        // 3级交易 - 高级物资
        TIER_3_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 1),
            new ItemStack(Items.ENDER_PEARL, 4), 3));
        TIER_3_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 2),
            new ItemStack(Items.BLAZE_POWDER, 8), 3));
        TIER_3_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 3),
            new ItemStack(Items.NETHERITE_SCRAP, 1), 3));

        // 4级交易 - 顶级物资
        TIER_4_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 5),
            new ItemStack(Items.NETHERITE_INGOT, 1), 4));
        TIER_4_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 8),
            new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1), 4));
        TIER_4_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 10),
            new ItemStack(Items.SHULKER_SHELL, 4), 4));

        // 5级交易 - 传说物资
        TIER_5_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 20),
            new ItemStack(Items.NETHER_STAR, 1), 5));
        TIER_5_OFFERS.add(new TradeOffer(
            new ItemStack(ModItems.NIHILITY_CRYSTAL.get(), 15),
            new ItemStack(Items.TOTEM_OF_UNDYING, 1), 5));
    }

    public static TradeOffer getRandomOffer(int tier) {
        List<TradeOffer> offers = getOffersForTier(tier);
        if (offers.isEmpty()) return null;
        return offers.get(RANDOM.nextInt(offers.size()));
    }

    private static List<TradeOffer> getOffersForTier(int tier) {
        switch (tier) {
            case 1: return TIER_1_OFFERS;
            case 2: return TIER_2_OFFERS;
            case 3: return TIER_3_OFFERS;
            case 4: return TIER_4_OFFERS;
            case 5: return TIER_5_OFFERS;
            default: return new ArrayList<>();
        }
    }
}
