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
        this.inventoryLabelY = 154;
        this.titleLabelY = 7;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;

        searchBox = new EditBox(font, leftPos + 8, topPos + 20, 62, 18,
            Component.translatable("screen.templenihility.search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::syncSearch);
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("<"), button -> sendButton(NihilityTerminalMenu.BUTTON_PREV_PAGE))
            .bounds(leftPos + 74, topPos + 20, 18, 18)
            .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> sendButton(NihilityTerminalMenu.BUTTON_NEXT_PAGE))
            .bounds(leftPos + 94, topPos + 20, 18, 18)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.sort_short"),
                button -> sendButton(NihilityTerminalMenu.BUTTON_CYCLE_SORT))
            .bounds(leftPos + 116, topPos + 20, 34, 18)
            .build());
        addRenderableWidget(Button.builder(Component.literal("X"), button -> {
                searchBox.setValue("");
                sendButton(NihilityTerminalMenu.BUTTON_CLEAR_SEARCH);
            })
            .bounds(leftPos + 152, topPos + 20, 16, 18)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.chunkload_button"),
                button -> sendButton(NihilityTerminalMenu.BUTTON_TOGGLE_CHUNKLOAD))
            .bounds(leftPos + 122, topPos + 153, 46, 12)
            .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        graphics.fill(RenderPipelines.GUI, x, y, x + imageWidth, y + imageHeight, 0xFF12091F);
        graphics.fill(RenderPipelines.GUI, x + 4, y + 15, x + imageWidth - 4, y + 42, 0xFF1F1033);
        graphics.fill(RenderPipelines.GUI, x + 4, y + 42, x + imageWidth - 4, y + 153, 0xFF25133B);
        graphics.fill(RenderPipelines.GUI, x + 4, y + 162, x + imageWidth - 4, y + 244, 0xFF1A1028);
        graphics.outline(x, y, imageWidth, imageHeight, 0xFF7B3CC5);
        graphics.outline(x + 4, y + 42, imageWidth - 8, 111, 0xFF42E6F5);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE9D8FF, false);
        graphics.text(font, Component.translatable("screen.templenihility.vault_status_line",
            menu.getUsedSlots(), menu.getTotalSlots(), menu.getFilteredSlotCount(),
            Component.translatable(menu.isNetworkChunkLoaded()
                ? "screen.templenihility.chunkload_on"
                : "screen.templenihility.chunkload_off")),
            8, 34, 0xFFB9A8D6, false);
        graphics.text(font, Component.literal((menu.getPage() + 1) + " / " + (menu.getMaxPage() + 1)), 140, 34, 0xFF7EEBFF, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE9D8FF, false);
    }

    public Rect2i getSearchAreaForJei() {
        return new Rect2i(leftPos + 8, topPos + 20, 62, 18);
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
