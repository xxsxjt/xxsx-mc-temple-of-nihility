package com.templenihility.client.screen;

import com.templenihility.TempleNihilityMod;
import com.templenihility.menu.NihilityAltarMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class NihilityAltarScreen extends AbstractContainerScreen<NihilityAltarMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/gui/container/nihility_altar.png");

    public NihilityAltarScreen(NihilityAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.altar_button"),
                button -> sendButton(NihilityAltarMenu.BUTTON_PERFORM_RITUAL))
            .bounds(leftPos + 75, topPos + 55, 48, 18)
            .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE9D8FF, false);
        graphics.text(font, Component.translatable("screen.templenihility.altar_tier",
            Component.translatable(menu.getTierNameKey())), 8, 18, 0xFFB9A8D6, false);
        graphics.text(font, Component.translatable("screen.templenihility.altar_parts",
            menu.getRuneCount(), menu.getCrystalCount(), menu.getPillarCount(), menu.getChiseledCount()),
            8, 62, 0xFF9ADCE8, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE9D8FF, false);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
