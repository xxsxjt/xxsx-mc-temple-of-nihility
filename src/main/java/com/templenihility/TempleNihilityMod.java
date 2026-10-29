package com.templenihility;

import com.templenihility.action.PlayerWeaponActions;
import com.templenihility.action.NihilityActionService;
import com.templenihility.action.GrandEnvoyActionPolicy;
import com.templenihility.action.GrandEnvoyDomainService;
import com.templenihility.action.CreatureProjectileDefenseService;
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
import com.templenihility.init.ModParticles;
import com.templenihility.init.ModRecipes;
import com.templenihility.init.ModStructures;
import com.templenihility.effect.VoidPhaseEvents;
import com.templenihility.effect.NihilityRelicEffects;
import com.templenihility.energy.VoidPower;
import com.templenihility.item.NihilityArmorEvents;
import com.templenihility.network.NihilityNetwork;
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
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

@Mod(TempleNihilityMod.MOD_ID)
public class TempleNihilityMod {
    public static final String MOD_ID = "templenihility";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final TicketController VAULT_CHUNK_TICKETS = new TicketController(
        Identifier.fromNamespaceAndPath(MOD_ID, "vault_chunks"));

    public TempleNihilityMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModMenus.register(modEventBus);
        ModParticles.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModStructures.register(modEventBus);
        ModCreativeTab.register(modEventBus);

        // 属性只在 MOD 总线
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onAttributeCreate);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerTicketControllers);
        modEventBus.addListener(NihilityNetwork::registerPayloads);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ModClientEvents.register(modEventBus);
        }
        // 服务启动在 NeoForge 总线
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(NihilityArmorEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(NihilityArmorEvents::incomingDamage);
        NeoForge.EVENT_BUS.addListener(PlayerWeaponActions::playerTick);
        NeoForge.EVENT_BUS.addListener(PlayerWeaponActions::incomingDamage);
        NeoForge.EVENT_BUS.addListener(PlayerWeaponActions::livingHeal);
        NeoForge.EVENT_BUS.addListener(PlayerWeaponActions::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(PlayerWeaponActions::playerCloned);
        NeoForge.EVENT_BUS.addListener(NihilityActionService::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(GravityFieldManager::levelTick);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::incomingDamage);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::invulnerabilityCheck);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::targetChange);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::attackEntity);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::entityInteract);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::projectileImpact);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerRespawn);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerChangedDimension);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerChangeGameMode);
        NeoForge.EVENT_BUS.addListener(VoidPhaseEvents::playerCloned);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::playerTick);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::entityTick);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::levelTick);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::incomingDamage);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::effectApplicable);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::projectileImpact);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(NihilityRelicEffects::playerCloned);

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

    private void registerTicketControllers(RegisterTicketControllersEvent event) {
        event.register(VAULT_CHUNK_TICKETS);
    }

    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        TempleCommand.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.NIHILITY_RITUAL_TYPE.get());
    }

    @SubscribeEvent
    public void onGuardedContainerOpen(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
            || !(event.getEntity() instanceof ServerPlayer player)
            || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RandomizableContainerBlockEntity)) {
            return;
        }

        AABB alertArea = player.getBoundingBox().inflate(16.0, 8.0, 16.0);
        boolean alerted = false;
        for (NihilityCreature creature : level.getEntitiesOfClass(NihilityCreature.class, alertArea)) {
            if (creature.isAlive()) {
                alerted |= creature.becomeHostileTo(player);
            }
        }
        if (alerted) {
            player.sendSystemMessage(Component.translatable("message.templenihility.creatures_alerted"));
        }
    }

    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || player.isSpectator()
            || player.isCreative()
            || !(player.level() instanceof ServerLevel sourceLevel)) {
            return;
        }

        boolean sourceInside = GrandEnvoyDomainService.findContainingDomain(sourceLevel, event.getPrev()) != null;
        boolean targetInside = GrandEnvoyDomainService.findContainingDomain(
            event.getTargetLevel(), event.getTarget()) != null;
        if (!GrandEnvoyActionPolicy.taxesTeleport(sourceInside, targetInside)) {
            return;
        }

        if (!VoidPower.tryConsume(player, GrandEnvoyActionPolicy.DOMAIN_TELEPORT_COST)) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable(
                "message.templenihility.grand_envoy_domain_teleport_blocked",
                GrandEnvoyActionPolicy.DOMAIN_TELEPORT_COST));
            return;
        }
        player.sendOverlayMessage(Component.translatable(
            "message.templenihility.grand_envoy_domain_teleport_cost",
            GrandEnvoyActionPolicy.DOMAIN_TELEPORT_COST,
            VoidPower.get(player)));
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        CreatureProjectileDefenseService.handle(event);
    }

    @SubscribeEvent
    public void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof Level level)
            || !event.getState().is(ModBlocks.NIHILITY_VAULT.get())
            || !(level.getBlockEntity(event.getPos()) instanceof NihilityVaultBlockEntity vault)) {
            return;
        }

        if (!com.templenihility.storage.NihilityVaultNetwork.canAccess(java.util.List.of(vault), event.getPlayer())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            event.getPlayer().sendSystemMessage(Component.translatable("message.templenihility.vault_access_denied"));
            return;
        }
        if (vault.isBreakProtected()) {
            event.setCanceled(true);
            if (!level.isClientSide()) {
                event.setNotifyClient(true);
                event.getPlayer().sendSystemMessage(
                    Component.translatable("message.templenihility.vault_break_protected"));
            }
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
