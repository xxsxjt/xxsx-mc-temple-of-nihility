package com.templenihility;

import com.templenihility.config.ModConfig;
import com.templenihility.entity.NihilityCreature;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.init.ModBlocks;
import com.templenihility.init.ModBlockEntities;
import com.templenihility.init.ModEntities;
import com.templenihility.init.ModCreativeTab;
import com.templenihility.init.ModItems;
import com.templenihility.init.ModMenus;
import com.templenihility.init.ModStructures;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

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
        ModMenus.register(modEventBus);
        // ModStructures.register(modEventBus);  // 结构系统暂时关闭，待修复
        ModCreativeTab.register(modEventBus);

        // 属性只在 MOD 总线
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onAttributeCreate);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ModClientEvents.register(modEventBus);
        }
        // 服务启动在 NeoForge 总线
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(NihilityArmorEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(NihilityArmorEvents::incomingDamage);
        NeoForge.EVENT_BUS.addListener(GravityFieldManager::levelTick);

        LOGGER.info("Temple of Nihility loaded");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("curios")) {
            event.enqueueWork(CuriosCompat::register);
        }
    }

    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        TempleCommand.register(event.getServer().getCommands().getDispatcher());
    }

    public void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NIHILITY_BELIEVER.get(), NihilityCreature.createAttributes(1).build());
        event.put(ModEntities.NIHILITY_FOLLOWER.get(), NihilityCreature.createAttributes(2).build());
        event.put(ModEntities.NIHILITY_ENVOY.get(), NihilityCreature.createAttributes(3).build());
        event.put(ModEntities.NIHILITY_GRAND_ENVOY.get(), NihilityCreature.createAttributes(4).build());
        event.put(ModEntities.NIHILITY_SAINT.get(), NihilityCreature.createAttributes(5).build());
    }
}
