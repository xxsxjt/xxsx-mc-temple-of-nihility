package com.templenihility.client.screen;

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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

/** 原版村民交易布局的虚无风格交易界面。 */
public class NihilityTradeScreen extends AbstractContainerScreen<NihilityTradeMenu> {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/villager.png");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/villager/scroller_disabled");
    private static final Identifier TRADE_ARROW = Identifier.withDefaultNamespace("container/villager/trade_arrow");

    private static final int OFFER_X = 5;
    private static final int OFFER_Y = 17;
    private static final int OFFER_WIDTH = 88;
    private static final int OFFER_HEIGHT = 20;
    private static final int SCROLL_X = 94;
    private static final int SCROLL_Y = 18;
    private static final int SCROLL_HEIGHT = 139;

    private Button tradeButton;
    private int scrollOffset;
    private boolean draggingScroller;

    public NihilityTradeScreen(NihilityTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 276, 166);
        this.inventoryLabelX = 108;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        tradeButton = addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.trade_button"),
                button -> sendButton(NihilityTradeMenu.BUTTON_TRADE))
            .bounds(leftPos + 188, topPos + 58, 80, 20)
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

        // A restrained cyan accent keeps the panel identifiable without replacing the vanilla container language.
        graphics.fill(leftPos + 101, topPos + 17, leftPos + 268, topPos + 18, 0xFF24AEB7);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int rightPanelCenter = 187;
        int titleX = rightPanelCenter - font.width(title) / 2;
        graphics.text(font, title, titleX, 6, 0xFF3F3532, false);
        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_contracts_label"), 49, 6,
            0xFF3F3532);
        graphics.text(font, Component.translatable("screen.templenihility.trade_tier", menu.getTier()),
            108, 18, 0xFF6D7D7D, false);

        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_payment"),
            NihilityTradeMenu.PAYMENT_SLOT_X + 8, 25, 0xFF566064);
        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_required"),
            NihilityTradeMenu.REQUIRED_SLOT_X + 8, 25, 0xFF566064);
        graphics.centeredText(font, Component.translatable("screen.templenihility.trade_result"),
            NihilityTradeMenu.RESULT_SLOT_X + 8, 25, 0xFF566064);

        TradeOffer selectedOffer = menu.getSelectedOffer();
        if (selectedOffer != null) {
            graphics.text(font, Component.translatable("screen.templenihility.trade_selected",
                    compactOfferName(selectedOffer.getResult(), 76)), 108, 61, 0xFF7C5F54, false);
        }
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
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
        drawScroller(graphics, mouseX, mouseY);
        addOfferTooltip(graphics, mouseX, mouseY);
    }

    private void drawOffer(GuiGraphicsExtractor graphics, int index, int row, boolean hovered) {
        TradeOffer offer = menu.getOffer(index);
        int y = OFFER_Y + row * OFFER_HEIGHT;
        boolean selected = index == menu.getSelectedIndex();

        if (selected || hovered) {
            int fill = selected ? 0x3D24AEB7 : 0x2424AEB7;
            graphics.fill(leftPos + OFFER_X, topPos + y, leftPos + OFFER_X + OFFER_WIDTH,
                topPos + y + OFFER_HEIGHT, fill);
            graphics.outline(leftPos + OFFER_X, topPos + y, OFFER_WIDTH, OFFER_HEIGHT,
                selected ? 0xFF24AEB7 : 0xFF7BA7A9);
        }

        int itemY = y + 2;
        graphics.fakeItem(offer.getCost(), leftPos + OFFER_X + 5, topPos + itemY);
        graphics.itemDecorations(font, offer.getCost(), leftPos + OFFER_X + 5, topPos + itemY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW,
            leftPos + OFFER_X + 50, topPos + itemY + 3, 10, 9);
        graphics.fakeItem(offer.getResult(), leftPos + OFFER_X + 68, topPos + itemY);
        graphics.itemDecorations(font, offer.getResult(), leftPos + OFFER_X + 68, topPos + itemY);
    }

    private void drawScroller(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (maxScrollOffset() <= 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED,
                leftPos + SCROLL_X, topPos + SCROLL_Y, 6, 27);
            return;
        }

        int steps = menu.getOfferCount() + 1 - NihilityTradeMenu.OFFERS_PER_PAGE;
        int leftOver = SCROLL_HEIGHT - (27 + (steps - 1) * SCROLL_HEIGHT / steps);
        int stepHeight = 1 + leftOver / steps + SCROLL_HEIGHT / steps;
        int scrollerOffset = Math.min(113, scrollOffset * stepHeight);
        if (scrollOffset == maxScrollOffset()) {
            scrollerOffset = 113;
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER,
            leftPos + SCROLL_X, topPos + SCROLL_Y + scrollerOffset, 6, 27);
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
            if (maxScrollOffset() > 0 && localX >= SCROLL_X && localX <= SCROLL_X + 6
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
            float normalized = ((float) event.y() - fullTop - 13.5f) / (fullBottom - fullTop - 27.0f);
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
