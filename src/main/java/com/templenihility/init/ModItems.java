package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TempleNihilityMod.MOD_ID);

    // 虚无碎片 - 基础货币
    public static final DeferredItem<Item> NIHILITY_SHARD = ITEMS.register("nihility_shard",
        id -> new NihilityShard(itemProperties(id).stacksTo(64)));

    // 虚无结晶 - 高级货币
    public static final DeferredItem<Item> NIHILITY_CRYSTAL = ITEMS.register("nihility_crystal",
        id -> new Item(itemProperties(id).stacksTo(64).rarity(Rarity.UNCOMMON)));

    // 虚无粉尘 - 基础加工材料
    public static final DeferredItem<Item> NIHILITY_DUST = ITEMS.register("nihility_dust",
        id -> new Item(itemProperties(id).stacksTo(64)));

    // 虚无符文 - 建筑与祭坛材料
    public static final DeferredItem<Item> NIHILITY_RUNE = ITEMS.register("nihility_rune",
        id -> new Item(itemProperties(id).stacksTo(64).rarity(Rarity.UNCOMMON)));

    // 圣殿封印 - 高级仪式材料
    public static final DeferredItem<Item> TEMPLE_SEAL = ITEMS.register("temple_seal",
        id -> new Item(itemProperties(id).stacksTo(64).rarity(Rarity.UNCOMMON)));

    // 虚无核心 - 核心材料
    public static final DeferredItem<Item> NIHILITY_CORE = ITEMS.register("nihility_core",
        id -> new Item(itemProperties(id).stacksTo(16).rarity(Rarity.RARE)));

    // 宝库升级件
    public static final DeferredItem<Item> NIHILITY_VAULT_EXPANSION = ITEMS.register("nihility_vault_expansion",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(16).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_vault_expansion"));

    // 武器
    public static final DeferredItem<Item> NIHILITY_SWORD = ITEMS.register("nihility_sword",
        id -> new NihilitySword(itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_DAGGER = ITEMS.register("nihility_dagger",
        id -> new NihilityDagger(itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_GREATSWORD = ITEMS.register("nihility_greatsword",
        id -> new NihilityGreatsword(itemProperties(id).rarity(Rarity.RARE)));

    // 工具
    public static final DeferredItem<Item> NIHILITY_PICKAXE = ITEMS.register("nihility_pickaxe",
        id -> new NihilityPickaxe(itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_AXE = ITEMS.register("nihility_axe",
        id -> new NihilityAxe(itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_SHOVEL = ITEMS.register("nihility_shovel",
        id -> new NihilityShovel(itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_HOE = ITEMS.register("nihility_hoe",
        id -> new NihilityHoe(itemProperties(id).rarity(Rarity.UNCOMMON)));

    // 功能道具
    public static final DeferredItem<Item> NIHILITY_LANTERN = ITEMS.register("nihility_lantern",
        id -> new NihilityTooltipItem(
            itemProperties(id).stacksTo(1).rarity(Rarity.UNCOMMON),
            true,
            "tooltip.templenihility.nihility_lantern"));

    public static final DeferredItem<Item> NIHILITY_RECOVERY_ORB = ITEMS.register("nihility_recovery_orb",
        id -> new NihilityTooltipItem(
            itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true,
            "tooltip.templenihility.nihility_recovery_orb"));

    public static final DeferredItem<Item> NIHILITY_SHADOW_SIGIL = ITEMS.register("nihility_shadow_sigil",
        id -> new NihilityEffectItem(NihilityEffectItem.Kind.SHADOW_SIGIL,
            itemProperties(id).stacksTo(16).rarity(Rarity.UNCOMMON),
            "tooltip.templenihility.nihility_shadow_sigil"));

    public static final DeferredItem<Item> NIHILITY_PURIFYING_BELL = ITEMS.register("nihility_purifying_bell",
        id -> new NihilityEffectItem(NihilityEffectItem.Kind.PURIFYING_BELL,
            itemProperties(id).stacksTo(1).durability(96).rarity(Rarity.RARE),
            "tooltip.templenihility.nihility_purifying_bell"));

    public static final DeferredItem<Item> NIHILITY_ECHO_LENS = ITEMS.register("nihility_echo_lens",
        id -> new NihilityEffectItem(NihilityEffectItem.Kind.ECHO_LENS,
            itemProperties(id).stacksTo(1).durability(128).rarity(Rarity.UNCOMMON),
            "tooltip.templenihility.nihility_echo_lens"));

    public static final DeferredItem<Item> NIHILITY_BARRIER_CORE = ITEMS.register("nihility_barrier_core",
        id -> new NihilityEffectItem(NihilityEffectItem.Kind.BARRIER_CORE,
            itemProperties(id).stacksTo(1).durability(80).rarity(Rarity.RARE),
            "tooltip.templenihility.nihility_barrier_core"));

    public static final DeferredItem<Item> NIHILITY_PHASE_FEATHER = ITEMS.register("nihility_phase_feather",
        id -> new NihilityEffectItem(NihilityEffectItem.Kind.PHASE_FEATHER,
            itemProperties(id).stacksTo(1).durability(96).rarity(Rarity.UNCOMMON),
            "tooltip.templenihility.nihility_phase_feather"));

    public static final DeferredItem<Item> NIHILITY_GRAVITY_SIGIL = ITEMS.register("nihility_gravity_sigil",
        id -> new NihilityEffectItem(NihilityEffectItem.Kind.GRAVITY_SIGIL,
            itemProperties(id).stacksTo(1).durability(80).rarity(Rarity.RARE),
            "tooltip.templenihility.nihility_gravity_sigil"));

    public static final DeferredItem<Item> NIHILITY_WAR_HORN = ITEMS.register("nihility_war_horn",
        id -> new NihilityEffectItem(NihilityEffectItem.Kind.WAR_HORN,
            itemProperties(id).stacksTo(1).durability(64).rarity(Rarity.RARE),
            "tooltip.templenihility.nihility_war_horn"));

    // Curios 饰品；未安装 Curios 时仍作为普通物品存在
    public static final DeferredItem<Item> NIHILITY_RING = ITEMS.register("nihility_ring",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.UNCOMMON),
            true, "tooltip.templenihility.nihility_ring"));

    public static final DeferredItem<Item> NIHILITY_AMULET = ITEMS.register("nihility_amulet",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.UNCOMMON),
            true, "tooltip.templenihility.nihility_amulet"));

    public static final DeferredItem<Item> NIHILITY_BELT = ITEMS.register("nihility_belt",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.UNCOMMON),
            true, "tooltip.templenihility.nihility_belt"));

    public static final DeferredItem<Item> NIHILITY_CLOAK = ITEMS.register("nihility_cloak",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_cloak"));

    public static final DeferredItem<Item> NIHILITY_CHARM = ITEMS.register("nihility_charm",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.UNCOMMON),
            true, "tooltip.templenihility.nihility_charm"));

    public static final DeferredItem<Item> NIHILITY_MAGNET = ITEMS.register("nihility_magnet",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_magnet"));

    public static final DeferredItem<Item> NIHILITY_REGENERATOR = ITEMS.register("nihility_regenerator",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_regenerator"));

    public static final DeferredItem<Item> NIHILITY_MINER_CHARM = ITEMS.register("nihility_miner_charm",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.UNCOMMON),
            true, "tooltip.templenihility.nihility_miner_charm"));

    public static final DeferredItem<Item> NIHILITY_WARD = ITEMS.register("nihility_ward",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_ward"));

    public static final DeferredItem<Item> NIHILITY_GAUNTLET = ITEMS.register("nihility_gauntlet",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_gauntlet"));

    public static final DeferredItem<Item> NIHILITY_HOURGLASS = ITEMS.register("nihility_hourglass",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_hourglass"));

    public static final DeferredItem<Item> NIHILITY_SOUL_ANCHOR = ITEMS.register("nihility_soul_anchor",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_soul_anchor"));

    public static final DeferredItem<Item> NIHILITY_RIFT_RING = ITEMS.register("nihility_rift_ring",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_rift_ring"));

    public static final DeferredItem<Item> NIHILITY_ECLIPSE_AMULET = ITEMS.register("nihility_eclipse_amulet",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_eclipse_amulet"));

    public static final DeferredItem<Item> NIHILITY_AEGIS_CHARM = ITEMS.register("nihility_aegis_charm",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_aegis_charm"));

    public static final DeferredItem<Item> NIHILITY_WAYFINDER = ITEMS.register("nihility_wayfinder",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.UNCOMMON),
            true, "tooltip.templenihility.nihility_wayfinder"));

    public static final DeferredItem<Item> NIHILITY_STAR_COMPASS = ITEMS.register("nihility_star_compass",
        id -> new NihilityTooltipItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE),
            true, "tooltip.templenihility.nihility_star_compass"));

    public static final DeferredItem<Item> NIHILITY_TERMINAL = ITEMS.register("nihility_terminal",
        id -> new NihilityTerminalItem(itemProperties(id).stacksTo(1).rarity(Rarity.RARE)));

    // 装备
    public static final DeferredItem<Item> NIHILITY_HELMET = ITEMS.register("nihility_helmet",
        id -> new NihilityArmor(ArmorType.HELMET, itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_CHESTPLATE = ITEMS.register("nihility_chestplate",
        id -> new NihilityArmor(ArmorType.CHESTPLATE, itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_LEGGINGS = ITEMS.register("nihility_leggings",
        id -> new NihilityArmor(ArmorType.LEGGINGS, itemProperties(id).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NIHILITY_BOOTS = ITEMS.register("nihility_boots",
        id -> new NihilityArmor(ArmorType.BOOTS, itemProperties(id).rarity(Rarity.UNCOMMON)));

    static Item.Properties itemProperties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
