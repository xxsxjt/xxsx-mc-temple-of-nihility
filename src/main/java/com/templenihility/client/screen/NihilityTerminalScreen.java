package com.templenihility.client.screen;

import com.templenihility.menu.NihilityTerminalMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class NihilityTerminalScreen extends AbstractContainerScreen<NihilityTerminalMenu> {
    private EditBox searchBox;
    private boolean syncingSearch;

    public NihilityTerminalScreen(NihilityTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 248);
        this.inventoryLabelY = 156;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;

        searchBox = new EditBox(font, leftPos + 8, topPos + 25, 66, 16,
            Component.translatable("screen.templenihility.search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::syncSearch);
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("<"), button -> sendButton(NihilityTerminalMenu.BUTTON_PREV_PAGE))
            .bounds(leftPos + 78, topPos + 25, 17, 16)
            .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> sendButton(NihilityTerminalMenu.BUTTON_NEXT_PAGE))
            .bounds(leftPos + 97, topPos + 25, 17, 16)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.sort_short"),
                button -> sendButton(NihilityTerminalMenu.BUTTON_CYCLE_SORT))
            .bounds(leftPos + 118, topPos + 25, 34, 16)
            .build());
        addRenderableWidget(Button.builder(Component.literal("X"), button -> {
                searchBox.setValue("");
                sendButton(NihilityTerminalMenu.BUTTON_CLEAR_SEARCH);
            })
            .bounds(leftPos + 154, topPos + 25, 14, 16)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.chunkload_button"),
                button -> sendButton(NihilityTerminalMenu.BUTTON_TOGGLE_CHUNKLOAD))
            .bounds(leftPos + 118, topPos + 151, 50, 14)
            .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        graphics.fill(RenderPipelines.GUI, x, y, x + imageWidth, y + imageHeight, 0xFF08040F);
        graphics.fill(RenderPipelines.GUI, x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF130821);
        graphics.fill(RenderPipelines.GUI, x + 5, y + 16, x + imageWidth - 5, y + 43, 0xFF211036);
        graphics.fill(RenderPipelines.GUI, x + 8, y + 25, x + 74, y + 41, 0xAA08040F);
        graphics.fill(RenderPipelines.GUI, x + 5, y + 43, x + imageWidth - 5, y + 151, 0xFF180D28);
        graphics.fill(RenderPipelines.GUI, x + 5, y + 166, x + imageWidth - 5, y + 244, 0xFF12091D);
        graphics.fill(RenderPipelines.GUI, x + 7, y + 45, x + 169, y + 151, 0x551EEBFF);
        graphics.fill(RenderPipelines.GUI, x + 7, y + 168, x + 169, y + 242, 0x331B8AFF);
        drawSlotGrid(graphics, x + 8, y + 45, 9, 6, 0xAA10081C, 0x6634F5FF);
        drawSlotGrid(graphics, x + 8, y + 166, 9, 3, 0xAA0C0615, 0x445A3C9A);
        drawSlotGrid(graphics, x + 8, y + 224, 9, 1, 0xBB160A24, 0x667B3CC5);
        graphics.fill(RenderPipelines.GUI, x + 11, y + 114, x + 165, y + 116, 0x5534F5FF);
        graphics.fill(RenderPipelines.GUI, x + 5, y + 43, x + imageWidth - 5, y + 45, 0xAA42E6F5);
        graphics.fill(RenderPipelines.GUI, x + 5, y + 166, x + imageWidth - 5, y + 168, 0x777B3CC5);
        graphics.fill(RenderPipelines.GUI, x + 10, y + 7, x + 18, y + 9, 0xFF42E6F5);
        graphics.fill(RenderPipelines.GUI, x + 158, y + 7, x + 166, y + 9, 0xFFE06BFF);
        graphics.outline(x, y, imageWidth, imageHeight, 0xFF9F5CFF);
        graphics.outline(x + 3, y + 3, imageWidth - 6, imageHeight - 6, 0x7734F5FF);
        graphics.outline(x + 5, y + 43, imageWidth - 10, 108, 0xFF42E6F5);
        graphics.outline(x + 5, y + 166, imageWidth - 10, 78, 0xAA7B3CC5);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE9D8FF, false);
        graphics.text(font, Component.translatable("screen.templenihility.vault_status_line",
            menu.getUsedSlots(), menu.getTotalSlots(), menu.getFilteredSlotCount(),
            Component.translatable(menu.isNetworkChunkLoaded()
                ? "screen.templenihility.chunkload_on"
                : "screen.templenihility.chunkload_off")),
            8, 16, 0xFFB9A8D6, false);
        graphics.text(font, Component.translatable("screen.templenihility.vault_stats_compact",
            menu.getVaultCount(), menu.getStoredItemCount(), menu.getEmptySlots()),
            8, 154, 0xFFD2C3F2, false);
        graphics.text(font, Component.translatable(menu.getSortLabelKey()), 82, 154, 0xFF7EEBFF, false);
        graphics.text(font, Component.literal((menu.getPage() + 1) + " / " + (menu.getMaxPage() + 1)), 140, 16, 0xFF7EEBFF, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE9D8FF, false);
    }

    private static void drawSlotGrid(GuiGraphicsExtractor graphics, int x, int y, int columns, int rows,
                                     int fillColor, int borderColor) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotX = x + col * 18;
                int slotY = y + row * 18;
                graphics.fill(RenderPipelines.GUI, slotX, slotY, slotX + 16, slotY + 16, fillColor);
                graphics.outline(slotX, slotY, 16, 16, borderColor);
            }
        }
    }

    public Rect2i getSearchAreaForJei() {
        return new Rect2i(leftPos + 8, topPos + 25, 66, 16);
    }

    public Optional<ItemStack> getJeiIngredientUnderMouse(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localY < 45 || localY >= 45 + 6 * 18 || localX < 8 || localX >= 8 + 9 * 18) {
            return Optional.empty();
        }

        int col = (localX - 8) / 18;
        int row = (localY - 45) / 18;
        int slotIndex = col + row * 9;
        if ((localX - 8) % 18 >= 16 || (localY - 45) % 18 >= 16 || slotIndex < 0 || slotIndex >= NihilityTerminalMenu.VISIBLE_SLOTS) {
            return Optional.empty();
        }

        ItemStack stack = menu.slots.get(slotIndex).getItem();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy());
    }

    public Rect2i getJeiSlotArea(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        int col = Math.max(0, Math.min(8, (localX - 8) / 18));
        int row = Math.max(0, Math.min(5, (localY - 45) / 18));
        return new Rect2i(leftPos + 8 + col * 18, topPos + 45 + row * 18, 16, 16);
    }

    public void searchFromJei(ItemStack stack) {
        if (searchBox == null || stack.isEmpty()) {
            return;
        }
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        String name = stack.getHoverName().getString();
        searchBox.setValue(name.isBlank() ? namespace : name);
    }

    private void syncSearch(String value) {
        if (syncingSearch || minecraft == null || minecraft.gameMode == null) {
            return;
        }
        syncingSearch = true;
        sendButton(NihilityTerminalMenu.BUTTON_CLEAR_SEARCH);
        for (int i = 0; i < value.length(); i++) {
            sendButton(NihilityTerminalMenu.BUTTON_CHAR_BASE + value.charAt(i));
        }
        syncingSearch = false;
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
