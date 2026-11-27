package com.templenihility.client;

import com.templenihility.TempleNihilityMod;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.client.model.NihilityFlowModel;
import com.templenihility.client.particle.NihilityParticle;
import com.templenihility.client.renderer.NihilityCreatureRenderer;
import com.templenihility.client.screen.NihilityAltarScreen;
import com.templenihility.client.screen.NihilityTerminalScreen;
import com.templenihility.client.screen.NihilityTransportCoreScreen;
import com.templenihility.client.screen.NihilityTradeScreen;
import com.templenihility.init.ModEntities;
import com.templenihility.init.ModItems;
import com.templenihility.init.ModMenus;
import com.templenihility.init.ModParticles;
import com.templenihility.item.NihilityAxe;
import com.templenihility.item.NihilityDagger;
import com.templenihility.item.NihilityGreatsword;
import com.templenihility.item.NihilitySword;
import com.templenihility.network.NihilityNetwork;
import com.templenihility.network.OpenTerminalPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public class ModClientEvents {
    private static final KeyMapping.Category CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "controls"));
    private static final KeyMapping OPEN_TERMINAL = new KeyMapping(
        "key.templenihility.open_terminal",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        CATEGORY
    );

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModClientEvents::registerLayerDefinitions);
        modEventBus.addListener(ModClientEvents::registerRenderers);
        modEventBus.addListener(ModClientEvents::registerKeyMappings);
        modEventBus.addListener(ModClientEvents::registerMenuScreens);
        modEventBus.addListener(ModClientEvents::registerParticleProviders);
        modEventBus.addListener(ModClientEvents::registerClientExtensions);
        modEventBus.addListener(ModClientEvents::registerGuiLayers);
        NihilityNetwork.setClientHudHandler(NihilityHudState::applyHudSync);
        NihilityNetwork.setClientFeedbackHandler(NihilityHudState::applyCombatFeedback);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::clientTick);
        NeoForge.EVENT_BUS.addListener(NihilityWeaponAnimation::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(NihilityWeaponAnimation::onUseItemStop);
        NeoForge.EVENT_BUS.addListener(NihilityWeaponAnimation::renderPlayerPre);
        com.templenihility.action.PlayerWeaponActions.setClientSkillListener(NihilityWeaponAnimation::handleClientSkillCallback);
        NeoForge.EVENT_BUS.addListener(NihilityClientRecipes::recipesReceived);
        NeoForge.EVENT_BUS.addListener(NihilityClientRecipes::loggingOut);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onLoggingOut);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(
            NihilityWeaponAnimation.WEAPON_EXTENSIONS,
            ModItems.NIHILITY_SWORD.get(),
            ModItems.NIHILITY_GREATSWORD.get(),
            ModItems.NIHILITY_DAGGER.get(),
            ModItems.NIHILITY_AXE.get()
        );
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_hud"),
            NihilityHudOverlay::render
        );
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        NihilityHudState.reset();
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_TERMINAL);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.NIHILITY_TERMINAL.get(), NihilityTerminalScreen::new);
        event.register(ModMenus.NIHILITY_TRADE.get(), NihilityTradeScreen::new);
        event.register(ModMenus.NIHILITY_ALTAR.get(), NihilityAltarScreen::new);
        event.register(ModMenus.NIHILITY_TRANSPORT_CORE.get(), NihilityTransportCoreScreen::new);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.NIHILITY_MOTE.get(),
            sprites -> new NihilityParticle.Provider(sprites, NihilityParticle.Style.MOTE));
        event.registerSpriteSet(ModParticles.NIHILITY_RIPPLE.get(),
            sprites -> new NihilityParticle.Provider(sprites, NihilityParticle.Style.RIPPLE));
        event.registerSpriteSet(ModParticles.NIHILITY_SHARD.get(),
            sprites -> new NihilityParticle.Provider(sprites, NihilityParticle.Style.SHARD));
        event.registerSpriteSet(ModParticles.ABYSS_EMBER.get(),
            sprites -> new NihilityParticle.Provider(sprites, NihilityParticle.Style.ABYSS));
        event.registerSpriteSet(ModParticles.CHAOS_GLINT.get(),
            sprites -> new NihilityParticle.Provider(sprites, NihilityParticle.Style.CHAOS));
    }

    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_TERMINAL.consumeClick()) {
            if (minecraft.getConnection() != null) {
                ClientPacketDistributor.sendToServer(OpenTerminalPayload.INSTANCE);
            }
        }
        // Subtle held-weapon Nihility erasure motes (restrained: 1 crisp mote flaking off weapon tip every 5 ticks)
        if (!minecraft.isPaused() && minecraft.player != null && minecraft.level != null) {
            NihilityHudState.clientTick();
            var player = minecraft.player;
            var mainItem = player.getMainHandItem().getItem();
            boolean holdingNihilityWeapon = mainItem instanceof NihilitySword
                || mainItem instanceof NihilityGreatsword
                || mainItem instanceof NihilityDagger
                || mainItem instanceof NihilityAxe;
            long tick = minecraft.level.getGameTime();
            if (holdingNihilityWeapon && tick % 5L == 0L) {
                var look = player.getLookAngle();
                double rightX = -look.z;
                double rightZ = look.x;
                double len = Math.max(0.001, Math.hypot(rightX, rightZ));
                rightX /= len;
                rightZ /= len;
                double fwd = (mainItem instanceof NihilityGreatsword) ? 0.95 : 0.72;
                double px = player.getX() + look.x * fwd + rightX * 0.42 + (minecraft.level.getRandom().nextDouble() - 0.5) * 0.14;
                double py = player.getY() + 1.05 + (minecraft.level.getRandom().nextDouble() - 0.35) * 0.28;
                double pz = player.getZ() + look.z * fwd + rightZ * 0.42 + (minecraft.level.getRandom().nextDouble() - 0.5) * 0.14;
                var particle = (tick % 15L == 0L) ? ModParticles.NIHILITY_SHARD.get() : ModParticles.NIHILITY_MOTE.get();
                minecraft.level.addParticle(particle, px, py, pz, 0.0, 0.018, 0.0);
            }
        }
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NihilityCreatureModel.LAYER_LOCATION, NihilityCreatureModel::createBodyLayer);
        event.registerLayerDefinition(NihilityFlowModel.LAYER_LOCATION, NihilityFlowModel::createBodyLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        Identifier[] textures = {
            Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "textures/entity/nihility_believer.png"),
            Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "textures/entity/nihility_follower.png"),
            Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "textures/entity/nihility_envoy.png"),
            Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "textures/entity/nihility_grand_envoy.png"),
            Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "textures/entity/nihility_saint.png"),
        };

        event.registerEntityRenderer(ModEntities.NIHILITY_BELIEVER.get(), ctx -> new NihilityCreatureRenderer<>(ctx, textures[0]));
        event.registerEntityRenderer(ModEntities.NIHILITY_FOLLOWER.get(), ctx -> new NihilityCreatureRenderer<>(ctx, textures[1]));
        event.registerEntityRenderer(ModEntities.NIHILITY_ENVOY.get(), ctx -> new NihilityCreatureRenderer<>(ctx, textures[2]));
        event.registerEntityRenderer(ModEntities.NIHILITY_GRAND_ENVOY.get(), ctx -> new NihilityCreatureRenderer<>(ctx, textures[3]));
        event.registerEntityRenderer(ModEntities.NIHILITY_SAINT.get(), ctx -> new NihilityCreatureRenderer<>(ctx, textures[4]));
    }
}
