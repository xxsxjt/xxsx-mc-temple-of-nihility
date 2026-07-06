package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.block.NihilityAltarBlock;
import com.templenihility.block.NihilityVaultBlock;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Function;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TempleNihilityMod.MOD_ID);

    // 虚无石
    public static final DeferredBlock<Block> NIHILITY_STONE = registerBlock("nihility_stone",
        id -> new Block(blockProperties(id)
            .strength(3.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()));

    // 虚无水晶块
    public static final DeferredBlock<Block> NIHILITY_CRYSTAL_BLOCK = registerBlock("nihility_crystal_block",
        id -> new Block(blockProperties(id)
            .strength(2.0f, 3.0f)
            .sound(SoundType.AMETHYST)
            .lightLevel(state -> 10)
            .requiresCorrectToolForDrops()));

    // 虚无砖块
    public static final DeferredBlock<Block> NIHILITY_BRICKS = registerBlock("nihility_bricks",
        id -> new Block(blockProperties(id)
            .strength(3.5f, 7.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()));

    // 虚无符文砖
    public static final DeferredBlock<Block> NIHILITY_RUNE_BRICKS = registerBlock("nihility_rune_bricks",
        id -> new Block(blockProperties(id)
            .strength(3.5f, 7.0f)
            .sound(SoundType.STONE)
            .lightLevel(state -> 4)
            .requiresCorrectToolForDrops()));

    // 裂纹虚无砖
    public static final DeferredBlock<Block> CRACKED_NIHILITY_BRICKS = registerBlock("cracked_nihility_bricks",
        id -> new Block(blockProperties(id)
            .strength(3.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()));

    // 錾制虚无石
    public static final DeferredBlock<Block> CHISELED_NIHILITY_STONE = registerBlock("chiseled_nihility_stone",
        id -> new Block(blockProperties(id)
            .strength(3.5f, 7.0f)
            .sound(SoundType.STONE)
            .lightLevel(state -> 2)
            .requiresCorrectToolForDrops()));

    // 虚无柱
    public static final DeferredBlock<Block> NIHILITY_PILLAR = registerBlock("nihility_pillar",
        id -> new Block(blockProperties(id)
            .strength(3.5f, 7.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()));

    // 虚无瓦
    public static final DeferredBlock<Block> NIHILITY_TILES = registerBlock("nihility_tiles",
        id -> new Block(blockProperties(id)
            .strength(3.5f, 7.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()));

    // 虚无灯
    public static final DeferredBlock<Block> NIHILITY_LAMP = registerBlock("nihility_lamp",
        id -> new Block(blockProperties(id)
            .strength(1.5f, 3.0f)
            .sound(SoundType.AMETHYST)
            .lightLevel(state -> 15)
            .requiresCorrectToolForDrops()));

    // 虚无玻璃
    public static final DeferredBlock<Block> NIHILITY_GLASS = registerBlock("nihility_glass",
        id -> new TransparentBlock(blockProperties(id)
            .strength(0.4f, 0.6f)
            .sound(SoundType.GLASS)
            .noOcclusion()));

    // 虚无祭坛
    public static final DeferredBlock<Block> NIHILITY_ALTAR = registerBlock("nihility_altar",
        id -> new NihilityAltarBlock(blockProperties(id)
            .strength(5.0f, 1200.0f)
            .sound(SoundType.STONE)
            .lightLevel(state -> 15)));

    // 虚无宝库 - 多方块网络仓储核心
    public static final DeferredBlock<Block> NIHILITY_VAULT = registerBlock("nihility_vault",
        id -> new NihilityVaultBlock(blockProperties(id)
            .strength(6.0f, 1200.0f)
            .sound(SoundType.AMETHYST)
            .lightLevel(state -> 8)
            .requiresCorrectToolForDrops()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<Identifier, T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        if ("nihility_altar".equals(name)) {
            ModItems.ITEMS.register(name, id -> new BlockItem(block.get(), ModItems.itemProperties(id).useBlockDescriptionPrefix()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                            Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("tooltip.templenihility.nihility_altar_1").withStyle(ChatFormatting.DARK_AQUA));
                    tooltip.accept(Component.translatable("tooltip.templenihility.nihility_altar_2").withStyle(ChatFormatting.AQUA));
                    tooltip.accept(Component.translatable("tooltip.templenihility.nihility_altar_3").withStyle(ChatFormatting.GRAY));
                }
            });
            return;
        }
        if ("nihility_vault".equals(name)) {
            ModItems.ITEMS.register(name, id -> new BlockItem(block.get(), ModItems.itemProperties(id).useBlockDescriptionPrefix()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                            Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("tooltip.templenihility.nihility_vault_1").withStyle(ChatFormatting.DARK_AQUA));
                    tooltip.accept(Component.translatable("tooltip.templenihility.nihility_vault_2").withStyle(ChatFormatting.AQUA));
                    tooltip.accept(Component.translatable("tooltip.templenihility.nihility_vault_3").withStyle(ChatFormatting.GRAY));
                }
            });
            return;
        }
        ModItems.ITEMS.register(name, id -> new BlockItem(block.get(), ModItems.itemProperties(id).useBlockDescriptionPrefix()));
    }

    private static BlockBehaviour.Properties blockProperties(Identifier id) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
