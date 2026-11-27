package com.templenihility.client.screen;

import com.templenihility.TempleNihilityMod;
import com.templenihility.menu.NihilityTradeMenu;
import com.templenihility.trade.TradeOffer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 虚无黑曜石风格圣殿契约交易界面。 */
public class NihilityTradeScreen extends AbstractContainerScreen<NihilityTradeMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/gui/nihility_trade.png");

    private static final int OFFER_X = 7;
    private static final int OFFER_Y = 21;
    private static final int OFFER_WIDTH = 85;
    private static final int OFFER_HEIGHT = 19;
    private static final int SCROLL_X = 95;
    private static final int SCROLL_Y = 21;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_HEIGHT = 133;

    private Button tradeButton;
    private int scrollOffset;
    private boolean draggingScroller;

    public NihilityTradeScreen(NihilityTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 276, 166);
        this.inventoryLabelX = 108;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        tradeButton = addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.trade_button"),
                button -> sendButton(NihilityTradeMenu.BUTTON_TRADE))
            .bounds(leftPos + 198, topPos + 55, 70, 18)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.trade_button_tip")))
            .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (tradeButton != null) {
            tradeButton.active = menu.canTradeSelected();
        }
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Left header centered over contracts well
        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_contracts_label"),
            53, 8, 0xFFD8EEF2);

        // Right header: trader title on left, tier badge flush right (zero overlap)
        Component tierLabel = Component.translatable("screen.templenihility.trade_tier", menu.getTier());
        int tierWidth = font.width(tierLabel);
        int maxTitleWidth = Math.max(40, 154 - tierWidth);
        graphics.text(font, compactText(title, maxTitleWidth), 108, 8, 0xFFE6F6F8, false);
        graphics.text(font, tierLabel, 268 - tierWidth, 8, 0xFF56E0E8, false);

        // Centered slot headers cleanly above slots at y=34
        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_payment"),
            NihilityTradeMenu.PAYMENT_SLOT_X + 8, 23, 0xFF8FB2BC);
        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_required"),
            NihilityTradeMenu.REQUIRED_SLOT_X + 8, 23, 0xFF8FB2BC);
        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_result"),
            NihilityTradeMenu.RESULT_SLOT_X + 8, 23, 0xFF8FB2BC);

        TradeOffer selectedOffer = menu.getSelectedOffer();
        if (selectedOffer != null) {
            Component selText = Component.translatable("screen.templenihility.trade_selected",
                compactOfferName(selectedOffer.getResult(), 64));
            graphics.text(font, compactText(selText, 86), 108, 59, 0xFFA9CCD2, false);
        }
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF8FB2BC, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        for (int row = 0; row < NihilityTradeMenu.OFFERS_PER_PAGE; row++) {
            int index = scrollOffset + row;
            if (index < menu.getOfferCount()) {
                drawOffer(graphics, index, row, offerRowAt(mouseX, mouseY) == row);
            }
        }
        drawScroller(graphics);
        addOfferTooltip(graphics, mouseX, mouseY);
    }

    private void drawOffer(GuiGraphicsExtractor graphics, int index, int row, boolean hovered) {
        TradeOffer offer = menu.getOffer(index);
        int x = leftPos + OFFER_X;
        int y = topPos + OFFER_Y + row * OFFER_HEIGHT;
        boolean selected = index == menu.getSelectedIndex();

        if (selected) {
            graphics.fill(x, y, x + OFFER_WIDTH, y + OFFER_HEIGHT, 0x3D24AEB7);
            graphics.outline(x, y, OFFER_WIDTH, OFFER_HEIGHT, 0xFF24AEB7);
        } else if (hovered) {
            graphics.fill(x, y, x + OFFER_WIDTH, y + OFFER_HEIGHT, 0x2424AEB7);
            graphics.outline(x, y, OFFER_WIDTH, OFFER_HEIGHT, 0xFF4B7A86);
        } else if (row % 2 == 1) {
            graphics.fill(x + 1, y, x + OFFER_WIDTH - 1, y + OFFER_HEIGHT, 0x12FFFFFF);
        }

        int itemY = y + 1;
        graphics.fakeItem(offer.getCost(), x + 5, itemY);
        graphics.itemDecorations(font, offer.getCost(), x + 5, itemY);

        // Crisp custom cyan mini-arrow between cost and result
        int arrowX = x + 36;
        int arrowY = y + 9;
        int shaftColor = selected ? 0xFF56E0E8 : 0xFF24AEB7;
        graphics.fill(arrowX, arrowY, arrowX + 11, arrowY + 2, shaftColor);
        graphics.fill(arrowX + 11, arrowY - 2, arrowX + 12, arrowY + 4, shaftColor);
        graphics.fill(arrowX + 12, arrowY - 1, arrowX + 13, arrowY + 3, 0xFF7FF0F5);
        graphics.fill(arrowX + 13, arrowY, arrowX + 14, arrowY + 2, 0xFF7FF0F5);

        graphics.fakeItem(offer.getResult(), x + 62, itemY);
        graphics.itemDecorations(font, offer.getResult(), x + 62, itemY);
    }

    private void drawScroller(GuiGraphicsExtractor graphics) {
        int sx = leftPos + SCROLL_X;
        int sy = topPos + SCROLL_Y;
        int maxScroll = maxScrollOffset();
        if (maxScroll <= 0) {
            graphics.fill(sx, sy, sx + SCROLL_WIDTH, sy + 24, 0xFF1E2B3D);
            graphics.outline(sx, sy, SCROLL_WIDTH, 24, 0xFF2C3E56);
            return;
        }

        int thumbHeight = Math.max(22, SCROLL_HEIGHT * NihilityTradeMenu.OFFERS_PER_PAGE / menu.getOfferCount());
        int trackSpan = Math.max(1, SCROLL_HEIGHT - thumbHeight);
        int offset = Math.round((float) scrollOffset / maxScroll * trackSpan);
        graphics.fill(sx, sy + offset, sx + SCROLL_WIDTH, sy + offset + thumbHeight, 0xFF24AEB7);
        graphics.outline(sx, sy + offset, SCROLL_WIDTH, thumbHeight, 0xFF56E0E8);
    }

    private void addOfferTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int row = offerRowAt(mouseX, mouseY);
        if (row < 0) {
            return;
        }
        int index = scrollOffset + row;
        if (index >= menu.getOfferCount()) {
            return;
        }
        TradeOffer offer = menu.getOffer(index);
        graphics.setComponentTooltipForNextFrame(font, java.util.List.of(
            Component.translatable("screen.templenihility.trade_offer_cost",
                offer.getCost().getCount(), offer.getCost().getHoverName()),
            Component.translatable("screen.templenihility.trade_offer_result",
                offer.getResult().getCount(), offer.getResult().getHoverName())
        ), mouseX, mouseY);
    }

    private int offerRowAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localX < OFFER_X || localX >= OFFER_X + OFFER_WIDTH
            || localY < OFFER_Y || localY >= OFFER_Y + OFFER_HEIGHT * NihilityTradeMenu.OFFERS_PER_PAGE) {
            return -1;
        }
        return (localY - OFFER_Y) / OFFER_HEIGHT;
    }

    private int maxScrollOffset() {
        return Math.max(0, menu.getOfferCount() - NihilityTradeMenu.OFFERS_PER_PAGE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (offerRowAt(mouseX, mouseY) >= 0 && maxScrollOffset() > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScrollOffset());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int localX = (int) event.x() - leftPos;
            int localY = (int) event.y() - topPos;
            if (maxScrollOffset() > 0 && localX >= SCROLL_X && localX <= SCROLL_X + SCROLL_WIDTH
                && localY >= SCROLL_Y && localY <= SCROLL_Y + SCROLL_HEIGHT) {
                draggingScroller = true;
                return true;
            }

            int row = offerRowAt(event.x(), event.y());
            int index = row < 0 ? -1 : scrollOffset + row;
            if (index >= 0 && index < menu.getOfferCount()) {
                sendButton(NihilityTradeMenu.BUTTON_SELECT_BASE + index);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScroller && maxScrollOffset() > 0) {
            int fullTop = topPos + SCROLL_Y;
            int fullBottom = fullTop + SCROLL_HEIGHT;
            float normalized = ((float) event.y() - fullTop - 12.0f) / Math.max(1.0f, fullBottom - fullTop - 24.0f);
            scrollOffset = Mth.clamp((int) (normalized * maxScrollOffset() + 0.5f), 0, maxScrollOffset());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroller = false;
        return super.mouseReleased(event);
    }

    private Component compactOfferName(ItemStack stack, int maxWidth) {
        return compactText(stack.getHoverName(), maxWidth);
    }

    private Component compactText(Component text, int maxWidth) {
        String raw = text.getString();
        if (font.width(raw) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int bodyWidth = Math.max(0, maxWidth - font.width(suffix));
        return Component.literal(font.plainSubstrByWidth(raw, bodyWidth) + suffix);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
