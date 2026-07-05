package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TempleNihilityMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NIHILITY_TAB = TABS.register("nihility",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.templenihility"))
            .icon(() -> new ItemStack(ModItems.NIHILITY_SHARD.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.NIHILITY_SHARD.get());
                output.accept(ModItems.NIHILITY_CRYSTAL.get());
                output.accept(ModItems.NIHILITY_DUST.get());
                output.accept(ModItems.NIHILITY_RUNE.get());
                output.accept(ModItems.TEMPLE_SEAL.get());
                output.accept(ModItems.NIHILITY_CORE.get());
                output.accept(ModItems.NIHILITY_SWORD.get());
                output.accept(ModItems.NIHILITY_DAGGER.get());
                output.accept(ModItems.NIHILITY_GREATSWORD.get());
                output.accept(ModItems.NIHILITY_PICKAXE.get());
                output.accept(ModItems.NIHILITY_AXE.get());
                output.accept(ModItems.NIHILITY_SHOVEL.get());
                output.accept(ModItems.NIHILITY_HOE.get());
                output.accept(ModItems.NIHILITY_HELMET.get());
                output.accept(ModItems.NIHILITY_CHESTPLATE.get());
                output.accept(ModItems.NIHILITY_LEGGINGS.get());
                output.accept(ModItems.NIHILITY_BOOTS.get());
                output.accept(ModItems.NIHILITY_BAG.get());
                output.accept(ModBlocks.NIHILITY_VAULT.get());
                output.accept(ModItems.NIHILITY_LANTERN.get());
                output.accept(ModItems.NIHILITY_RECOVERY_ORB.get());
                output.accept(ModItems.NIHILITY_SHADOW_SIGIL.get());
                output.accept(ModItems.NIHILITY_PURIFYING_BELL.get());
                output.accept(ModItems.NIHILITY_ECHO_LENS.get());
                output.accept(ModItems.NIHILITY_BARRIER_CORE.get());
                output.accept(ModItems.NIHILITY_TERMINAL.get());
                output.accept(ModItems.NIHILITY_RING.get());
                output.accept(ModItems.NIHILITY_AMULET.get());
                output.accept(ModItems.NIHILITY_BELT.get());
                output.accept(ModItems.NIHILITY_CLOAK.get());
                output.accept(ModItems.NIHILITY_CHARM.get());
                output.accept(ModItems.NIHILITY_MAGNET.get());
                output.accept(ModItems.NIHILITY_REGENERATOR.get());
                output.accept(ModItems.NIHILITY_MINER_CHARM.get());
                output.accept(ModItems.NIHILITY_WARD.get());
                output.accept(ModBlocks.NIHILITY_STONE.get());
                output.accept(ModBlocks.NIHILITY_CRYSTAL_BLOCK.get());
                output.accept(ModBlocks.NIHILITY_BRICKS.get());
                output.accept(ModBlocks.NIHILITY_RUNE_BRICKS.get());
                output.accept(ModBlocks.CRACKED_NIHILITY_BRICKS.get());
                output.accept(ModBlocks.CHISELED_NIHILITY_STONE.get());
                output.accept(ModBlocks.NIHILITY_PILLAR.get());
                output.accept(ModBlocks.NIHILITY_TILES.get());
                output.accept(ModBlocks.NIHILITY_LAMP.get());
                output.accept(ModBlocks.NIHILITY_GLASS.get());
                output.accept(ModBlocks.NIHILITY_ALTAR.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
