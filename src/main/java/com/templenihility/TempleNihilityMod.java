package com.templenihility;

import com.templenihility.config.ModConfig;
import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.entity.NihilityCreature;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.compat.WaystonesCompat;
import com.templenihility.init.ModBlocks;
import com.templenihility.init.ModBlockEntities;
import com.templenihility.init.ModEntities;
import com.templenihility.init.ModCreativeTab;
import com.templenihility.init.ModEffects;
import com.templenihility.init.ModItems;
import com.templenihility.init.ModMenus;
import com.templenihility.init.ModStructures;
import com.templenihility.effect.VoidPhaseEvents;
import com.templenihility.item.NihilityArmorEvents;
import com.templenihility.client.ModClientEvents;
import com.templenihility.world.GravityFieldManager;
import com.templenihility.world.TempleCommand;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

@Mod(TempleNihilityMod.MOD_ID)
public class TempleNihilityMod {
    public static final String MOD_ID = "templenihility";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TempleNihilityMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModMenus.register(modEventBus);
        ModStructures.register(modEventBus);
        ModCreativeTab.register(modEventBus);

        // 属性只在 MOD 总线
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onAttributeCreate);
        modEventBus.addListener(this::registerCapabilities);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ModClientEvents.register(modEventBus);
        }
        // 服务启动在 NeoForge 总线
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(NihilityArmorEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(NihilityArmorEvents::incomingDamage);
        NeoForge.EVENT_BUS.addListener(GravityFieldManager::levelTick);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::incomingDamage);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::invulnerabilityCheck);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::targetChange);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::attackEntity);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::entityInteract);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::entityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::projectileImpact);

        LOGGER.info("Temple of Nihility loaded");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("curios")) {
            event.enqueueWork(CuriosCompat::register);
        }
        if (ModList.get().isLoaded("waystones")) {
            event.enqueueWork(WaystonesCompat::register);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntities.NIHILITY_ENERGY_CELL.get(),
            (cell, side) -> cell.getEnergyHandler());
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntities.NIHILITY_ENERGY_PRISM.get(),
            (prism, side) -> prism.getEnergyHandler());
    }

    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        TempleCommand.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof Level level)
            || !event.getState().is(ModBlocks.NIHILITY_VAULT.get())
            || !(level.getBlockEntity(event.getPos()) instanceof NihilityVaultBlockEntity vault)
            || !vault.isBreakProtected()) {
            return;
        }

        event.setCanceled(true);
        if (!level.isClientSide()) {
            event.setNotifyClient(true);
            event.getPlayer().sendSystemMessage(
                Component.translatable("message.templenihility.vault_break_protected"));
        }
    }

    public void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NIHILITY_BELIEVER.get(), NihilityCreature.createAttributes(1).build());
        event.put(ModEntities.NIHILITY_FOLLOWER.get(), NihilityCreature.createAttributes(2).build());
        event.put(ModEntities.NIHILITY_ENVOY.get(), NihilityCreature.createAttributes(3).build());
        event.put(ModEntities.NIHILITY_GRAND_ENVOY.get(), NihilityCreature.createAttributes(4).build());
        event.put(ModEntities.NIHILITY_SAINT.get(), NihilityCreature.createAttributes(5).build());
    }
}
