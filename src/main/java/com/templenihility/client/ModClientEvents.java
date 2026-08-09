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
import com.templenihility.init.ModMenus;
import com.templenihility.init.ModParticles;
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
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
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
        NeoForge.EVENT_BUS.addListener(ModClientEvents::clientTick);
        NeoForge.EVENT_BUS.addListener(NihilityWeaponAnimation::renderHand);
        NeoForge.EVENT_BUS.addListener(NihilityClientRecipes::recipesReceived);
        NeoForge.EVENT_BUS.addListener(NihilityClientRecipes::loggingOut);
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
        while (OPEN_TERMINAL.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null) {
                ClientPacketDistributor.sendToServer(OpenTerminalPayload.INSTANCE);
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
