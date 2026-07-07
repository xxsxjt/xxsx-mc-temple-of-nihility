package com.templenihility.client.screen;

import com.templenihility.TempleNihilityMod;
import com.templenihility.menu.NihilityTradeMenu;
import com.templenihility.trade.TradeOffer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class NihilityTradeScreen extends AbstractContainerScreen<NihilityTradeMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/gui/container/nihility_trade.png");
    private Button tradeButton;
    private Button prevButton;
    private Button nextButton;

    public NihilityTradeScreen(NihilityTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 204, 202);
        this.titleLabelX = 10;
        this.titleLabelY = 8;
        this.inventoryLabelX = 25;
        this.inventoryLabelY = 108;
    }

    @Override
    protected void init() {
        super.init();
        int rows = Math.min(NihilityTradeMenu.OFFERS_PER_PAGE, menu.getOfferCount());
        for (int i = 0; i < rows; i++) {
            final int row = i;
            addRenderableWidget(Button.builder(Component.literal(String.valueOf(i + 1)),
                    button -> sendButton(NihilityTradeMenu.BUTTON_SELECT_BASE + row))
                .bounds(leftPos + 10, topPos + 27 + i * 16, 18, 14)
                .build());
        }
        prevButton = addRenderableWidget(Button.builder(Component.literal("<"),
                button -> sendButton(NihilityTradeMenu.BUTTON_PREV_PAGE))
            .bounds(leftPos + 31, topPos + 94, 18, 14)
            .build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"),
                button -> sendButton(NihilityTradeMenu.BUTTON_NEXT_PAGE))
            .bounds(leftPos + 90, topPos + 94, 18, 14)
            .build());
        tradeButton = addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.trade_button"),
                button -> sendButton(NihilityTradeMenu.BUTTON_TRADE))
            .bounds(leftPos + 132, topPos + 94, 58, 18)
            .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (tradeButton != null) {
            tradeButton.active = menu.canTradeSelected();
        }
        if (prevButton != null) {
            prevButton.active = menu.getPage() > 0;
        }
        if (nextButton != null) {
            nextButton.active = menu.getPage() < menu.getMaxPage();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE9D8FF, false);
        graphics.text(font, Component.translatable("screen.templenihility.trade_tier", menu.getTier()),
            122, 27, 0xFFB9A8D6, false);
        graphics.text(font, Component.translatable("screen.templenihility.trade_payment"),
            26, 60, 0xFFB9A8D6, false);
        graphics.text(font, Component.literal("→"), 93, 76, 0xFF7EEBFF, false);
        graphics.text(font, Component.translatable("screen.templenihility.trade_result"),
            147, 60, 0xFFB9A8D6, false);
        graphics.text(font, Component.literal((menu.getPage() + 1) + "/" + (menu.getMaxPage() + 1)),
            55, 97, 0xFF7EEBFF, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE9D8FF, false);

        for (int row = 0; row < NihilityTradeMenu.OFFERS_PER_PAGE; row++) {
            int index = menu.getVisibleOfferIndex(row);
            if (index < menu.getOfferCount()) {
                drawOffer(graphics, index, 31 + row * 16);
            }
        }
    }

    private void drawOffer(GuiGraphicsExtractor graphics, int index, int y) {
        TradeOffer offer = menu.getOffer(index);
        int color = index == menu.getSelectedIndex() ? 0xFF33204D : 0xFF170C24;
        graphics.fill(30, y - 2, 108, y + 14, color);
        graphics.outline(30, y - 2, 78, 16, index == menu.getSelectedIndex() ? 0xFF7EEBFF : 0xFF5A3778);
        drawMiniItem(graphics, offer.getCost(), 33, y - 1);
        graphics.text(font, ">", 53, y + 3, 0xFF7EEBFF, false);
        drawMiniItem(graphics, offer.getResult(), 64, y - 1);
        graphics.text(font, compactOfferName(offer.getResult(), 34), 83, y + 3, 0xFFE9D8FF, false);
    }

    private void drawMiniItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
    }

    private Component compactOfferName(ItemStack stack, int maxWidth) {
        Component hoverName = stack.getHoverName();
        String name = hoverName.getString();
        if (font.width(name) <= maxWidth) {
            return hoverName;
        }

        String suffix = "...";
        int bodyWidth = Math.max(0, maxWidth - font.width(suffix));
        return Component.literal(font.plainSubstrByWidth(name, bodyWidth) + suffix);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
