package com.templenihility.client;

import com.templenihility.TempleNihilityMod;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.client.renderer.NihilityCreatureRenderer;
import com.templenihility.client.screen.NihilityTerminalScreen;
import com.templenihility.init.ModEntities;
import com.templenihility.init.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
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
        NeoForge.EVENT_BUS.addListener(ModClientEvents::clientTick);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_TERMINAL);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.NIHILITY_TERMINAL.get(), NihilityTerminalScreen::new);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        while (OPEN_TERMINAL.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null) {
                minecraft.getConnection().sendCommand("nihility terminal");
            }
        }
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NihilityCreatureModel.LAYER_LOCATION, NihilityCreatureModel::createBodyLayer);
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
