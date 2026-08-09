package com.templenihility.client.screen;

import com.templenihility.menu.NihilityTransportCoreMenu;
import com.templenihility.menu.NihilityTransportLayout;
import com.templenihility.transport.TransportPolicy.BlockedReason;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class NihilityTransportCoreScreen extends AbstractContainerScreen<NihilityTransportCoreMenu> {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace(
        "textures/gui/container/dispenser.png");

    private Button inputButton;
    private Button outputButton;
    private Button modeButton;
    private Button priorityButton;
    private Button redstoneButton;
    private Button filterButton;
    private Button clearButton;

    public NihilityTransportCoreScreen(NihilityTransportCoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, NihilityTransportLayout.IMAGE_WIDTH, NihilityTransportLayout.IMAGE_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = -100;
    }

    @Override
    protected void init() {
        super.init();
        inputButton = addConfigButton(NihilityTransportLayout.LEFT_BUTTON_X,
            NihilityTransportLayout.BUTTON_ROW_1_Y, NihilityTransportCoreMenu.BUTTON_INPUT_DIRECTION,
            "screen.templenihility.transport.input_tip");
        modeButton = addConfigButton(NihilityTransportLayout.LEFT_BUTTON_X,
            NihilityTransportLayout.BUTTON_ROW_2_Y, NihilityTransportCoreMenu.BUTTON_RESOURCE_MODE,
            "screen.templenihility.transport.mode_tip");
        redstoneButton = addConfigButton(NihilityTransportLayout.LEFT_BUTTON_X,
            NihilityTransportLayout.BUTTON_ROW_3_Y, NihilityTransportCoreMenu.BUTTON_REDSTONE_MODE,
            "screen.templenihility.transport.redstone_tip");

        outputButton = addConfigButton(NihilityTransportLayout.RIGHT_BUTTON_X,
            NihilityTransportLayout.BUTTON_ROW_1_Y, NihilityTransportCoreMenu.BUTTON_OUTPUT_DIRECTION,
            "screen.templenihility.transport.output_tip");
        priorityButton = addConfigButton(NihilityTransportLayout.RIGHT_BUTTON_X,
            NihilityTransportLayout.BUTTON_ROW_2_Y, NihilityTransportCoreMenu.BUTTON_PRIORITY,
            "screen.templenihility.transport.priority_tip");
        filterButton = addConfigButton(NihilityTransportLayout.RIGHT_BUTTON_X,
            NihilityTransportLayout.BUTTON_ROW_3_Y, NihilityTransportCoreMenu.BUTTON_FILTER_MODE,
            "screen.templenihility.transport.filter_mode_tip");

        clearButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.transport.clear_filters"),
                button -> sendButton(NihilityTransportCoreMenu.BUTTON_CLEAR_FILTERS))
            .bounds(leftPos + NihilityTransportLayout.CLEAR_BUTTON_X,
                topPos + NihilityTransportLayout.CLEAR_BUTTON_Y,
                NihilityTransportLayout.CLEAR_BUTTON_WIDTH,
                NihilityTransportLayout.CLEAR_BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.transport.clear_filters_tip")))
            .build());
        refreshButtons();
    }

    private Button addConfigButton(int x, int y, int id, String tooltipKey) {
        return addRenderableWidget(Button.builder(Component.empty(), button -> sendButton(id))
            .bounds(leftPos + x, topPos + y,
                NihilityTransportLayout.BUTTON_WIDTH, NihilityTransportLayout.BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
            .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (inputButton == null) {
            return;
        }
        inputButton.setMessage(Component.translatable("screen.templenihility.transport.input_button",
            directionName(menu.getInputDirection())));
        outputButton.setMessage(Component.translatable("screen.templenihility.transport.output_button",
            directionName(menu.getOutputDirection())));
        modeButton.setMessage(Component.translatable(resourceModeKey()));
        priorityButton.setMessage(Component.translatable(priorityKey()));
        redstoneButton.setMessage(Component.translatable(redstoneKey()));
        filterButton.setMessage(Component.translatable(filterKey()));
        priorityButton.active = menu.getResourceMode() == com.templenihility.transport.TransportPolicy.ResourceMode.ALL;
        clearButton.active = menu.hasFilterEntries();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component status = currentStatus();
        int statusColor = menu.getBlockedReason() == BlockedReason.RUNNING ? 0xFF2D7770 : 0xFF8A623E;
        graphics.text(font, compact(Component.translatable(
            "screen.templenihility.transport.title_status", title, status), 160), 8, 6, statusColor, false);

        if (isTopLineHovered(mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                Component.translatable("screen.templenihility.transport.route_detail",
                    directionName(menu.getInputDirection()), directionName(menu.getOutputDirection())),
                Component.translatable("screen.templenihility.transport.throughput_detail",
                    menu.getLastItemMoved(), menu.getLastFluidMoved(), menu.getLastEnergyMoved()),
                menu.isLegacyRouting()
                    ? Component.translatable("screen.templenihility.transport.legacy_tip")
                    : Component.translatable(menu.getBlockedReason().translationKey())
            ), mouseX, mouseY);
        }

        for (int index = 0; index < NihilityTransportCoreMenu.FILTER_SLOT_COUNT; index++) {
            Slot slot = menu.getSlot(index);
            if (slot.getItem().isEmpty() && isSlotHovered(slot, mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.templenihility.transport.filter_slot_tip_1"),
                    Component.translatable("screen.templenihility.transport.filter_slot_tip_2")
                ), mouseX, mouseY);
                break;
            }
        }
    }

    private Component currentStatus() {
        if (menu.isLegacyRouting()) {
            return Component.translatable("screen.templenihility.transport.status_legacy");
        }
        return Component.translatable(menu.getBlockedReason().translationKey());
    }

    private boolean isTopLineHovered(int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= 6 && localX < 170 && localY >= 3 && localY < 16;
    }

    private boolean isSlotHovered(Slot slot, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= slot.x && localX < slot.x + 16 && localY >= slot.y && localY < slot.y + 16;
    }

    private Component directionName(Direction direction) {
        return Component.translatable("screen.templenihility.transport.direction."
            + direction.getName());
    }

    private String resourceModeKey() {
        return "screen.templenihility.transport.mode." + menu.getResourceMode().name().toLowerCase(java.util.Locale.ROOT);
    }

    private String priorityKey() {
        return "screen.templenihility.transport.priority." + menu.getPriority().name().toLowerCase(java.util.Locale.ROOT);
    }

    private String redstoneKey() {
        return "screen.templenihility.transport.redstone." + menu.getRedstoneMode().name().toLowerCase(java.util.Locale.ROOT);
    }

    private String filterKey() {
        return "screen.templenihility.transport.filter." + menu.getFilterMode().name().toLowerCase(java.util.Locale.ROOT);
    }

    private Component compact(Component component, int maxWidth) {
        String text = component.getString();
        if (font.width(text) <= maxWidth) {
            return component;
        }
        String suffix = "...";
        return Component.literal(font.plainSubstrByWidth(text, maxWidth - font.width(suffix)) + suffix);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
