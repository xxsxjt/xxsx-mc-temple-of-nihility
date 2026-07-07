package com.templenihility.client.screen;

import com.templenihility.TempleNihilityMod;
import com.templenihility.menu.NihilityTerminalLayout;
import com.templenihility.menu.NihilityTerminalMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NihilityTerminalScreen extends AbstractContainerScreen<NihilityTerminalMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/gui/container/nihility_terminal.png");
    private static final Identifier VOID_SHARDS = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/gui/effects/void_shards.png");
    private EditBox searchBox;
    private Button clearSearchButton;
    private Button chunkloadButton;
    private Button breakProtectionButton;
    private Button confirmDisableButton;
    private Button cancelDisableButton;
    private final List<Button> mainButtons = new ArrayList<>();
    private boolean syncingSearch;
    private boolean confirmingDisableChunkload;
    private int animationTicks;

    public NihilityTerminalScreen(NihilityTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, NihilityTerminalLayout.IMAGE_WIDTH, NihilityTerminalLayout.IMAGE_HEIGHT);
        this.inventoryLabelX = NihilityTerminalLayout.INVENTORY_LABEL_X;
        this.inventoryLabelY = NihilityTerminalLayout.INVENTORY_LABEL_Y;
        this.titleLabelY = NihilityTerminalLayout.TITLE_Y;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = NihilityTerminalLayout.TITLE_X;

        searchBox = new EditBox(font, leftPos + NihilityTerminalLayout.SEARCH_X,
            topPos + NihilityTerminalLayout.SEARCH_Y,
            NihilityTerminalLayout.SEARCH_WIDTH, NihilityTerminalLayout.SEARCH_HEIGHT,
            Component.translatable("screen.templenihility.search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.translatable("screen.templenihility.search_hint"));
        searchBox.setResponder(this::syncSearch);
        addRenderableWidget(searchBox);

        addMainButton(Button.builder(Component.translatable("screen.templenihility.sort_short"),
                button -> sendButton(NihilityTerminalMenu.BUTTON_CYCLE_SORT))
            .bounds(leftPos + NihilityTerminalLayout.SORT_BUTTON_X, topPos + NihilityTerminalLayout.SORT_BUTTON_Y,
                NihilityTerminalLayout.SORT_BUTTON_WIDTH, NihilityTerminalLayout.SORT_BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.sort_tip")))
            .build());
        clearSearchButton = addMainButton(Button.builder(Component.literal("X"), button -> {
                searchBox.setValue("");
                sendButton(NihilityTerminalMenu.BUTTON_CLEAR_SEARCH);
            })
            .bounds(leftPos + NihilityTerminalLayout.CLEAR_SEARCH_X, topPos + NihilityTerminalLayout.CLEAR_SEARCH_Y,
                NihilityTerminalLayout.CLEAR_SEARCH_SIZE, NihilityTerminalLayout.CLEAR_SEARCH_SIZE)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.clear_search_tip")))
            .build());
        chunkloadButton = addMainButton(Button.builder(chunkloadButtonText(), button -> onChunkloadPressed())
            .bounds(leftPos + NihilityTerminalLayout.CHUNKLOAD_BUTTON_X, topPos + NihilityTerminalLayout.CHUNKLOAD_BUTTON_Y,
                NihilityTerminalLayout.CHUNKLOAD_BUTTON_WIDTH, NihilityTerminalLayout.CHUNKLOAD_BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.chunkload_tip")))
            .build());
        breakProtectionButton = addMainButton(Button.builder(breakProtectionButtonText(),
                button -> sendButton(NihilityTerminalMenu.BUTTON_TOGGLE_BREAK_PROTECTION))
            .bounds(leftPos + NihilityTerminalLayout.PROTECTION_BUTTON_X, topPos + NihilityTerminalLayout.PROTECTION_BUTTON_Y,
                NihilityTerminalLayout.PROTECTION_BUTTON_WIDTH, NihilityTerminalLayout.PROTECTION_BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.protection_tip")))
            .build());
        addMainButton(Button.builder(Component.literal("?"), button -> {})
            .bounds(leftPos + NihilityTerminalLayout.HELP_BUTTON_X, topPos + NihilityTerminalLayout.HELP_BUTTON_Y,
                NihilityTerminalLayout.HELP_BUTTON_WIDTH, NihilityTerminalLayout.HELP_BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.terminal_help_tip")))
            .build());

        confirmDisableButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.confirm_disable_chunkload_yes"),
                button -> confirmDisableChunkload())
            .bounds(leftPos + NihilityTerminalLayout.CONFIRM_YES_X, topPos + NihilityTerminalLayout.CONFIRM_YES_Y,
                NihilityTerminalLayout.CONFIRM_BUTTON_WIDTH, NihilityTerminalLayout.CONFIRM_BUTTON_HEIGHT)
            .build());
        cancelDisableButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.confirm_disable_chunkload_no"),
                button -> setConfirmingDisableChunkload(false))
            .bounds(leftPos + NihilityTerminalLayout.CONFIRM_NO_X, topPos + NihilityTerminalLayout.CONFIRM_NO_Y,
                NihilityTerminalLayout.CONFIRM_BUTTON_WIDTH, NihilityTerminalLayout.CONFIRM_BUTTON_HEIGHT)
            .build());
        setConfirmingDisableChunkload(false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        extractVoidShards(graphics, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("screen.templenihility.terminal_title_short"),
            titleLabelX, titleLabelY, 0xFF3F314D, false);
        graphics.text(font, Component.translatable("screen.templenihility.vault_status_line",
            formatCount(menu.getVaultCount()), formatCount(menu.getFilteredSlotCount()), formatCount(menu.getStoredItemCount())),
            NihilityTerminalLayout.VAULT_STATUS_X, NihilityTerminalLayout.VAULT_STATUS_Y, 0xFF5A4B68, false);
        graphics.text(font, Component.translatable("screen.templenihility.vault_stats_compact",
            formatCount(menu.getUsedSlots()), formatCount(menu.getTotalSlots())),
            NihilityTerminalLayout.STATS_TEXT_X, NihilityTerminalLayout.STATS_TEXT_Y, 0xFF5A4B68, false);
        graphics.text(font, Component.translatable(menu.getSortLabelKey()),
            NihilityTerminalLayout.SORT_TEXT_X, NihilityTerminalLayout.SORT_TEXT_Y, 0xFF396E78, false);
        drawCapacityBar(graphics);
        drawStorageScrollbar(graphics);
        drawStorageEmptyHint(graphics);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF3F314D, false);
        addPanelTooltips(graphics, mouseX, mouseY);

        if (confirmingDisableChunkload) {
            extractDisableChunkloadConfirm(graphics);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        animationTicks++;
        if (chunkloadButton != null) {
            chunkloadButton.setMessage(chunkloadButtonText());
        }
        if (breakProtectionButton != null) {
            breakProtectionButton.setMessage(breakProtectionButtonText());
        }
        refreshButtonStates();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (confirmingDisableChunkload && event.isEscape()) {
            setConfirmingDisableChunkload(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (confirmingDisableChunkload) {
            return true;
        }
        if (isHoveringLocal(NihilityTerminalLayout.STORAGE_X, NihilityTerminalLayout.STORAGE_Y,
            NihilityTerminalMenu.STORAGE_COLUMNS * NihilityTerminalLayout.SLOT_STEP + 8,
            NihilityTerminalMenu.STORAGE_ROWS * NihilityTerminalLayout.SLOT_STEP, (int) mouseX, (int) mouseY)
            && menu.getMaxScrollOffset() > 0) {
            sendButton(scrollY > 0 ? NihilityTerminalMenu.BUTTON_SCROLL_UP : NihilityTerminalMenu.BUTTON_SCROLL_DOWN);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public Rect2i getSearchAreaForJei() {
        return new Rect2i(leftPos + NihilityTerminalLayout.SEARCH_X,
            topPos + NihilityTerminalLayout.SEARCH_Y,
            NihilityTerminalLayout.SEARCH_JEI_WIDTH, NihilityTerminalLayout.SEARCH_HEIGHT);
    }

    public Optional<ItemStack> getJeiIngredientUnderMouse(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localY < NihilityTerminalLayout.STORAGE_Y
            || localY >= NihilityTerminalLayout.STORAGE_Y + NihilityTerminalMenu.STORAGE_ROWS * NihilityTerminalLayout.SLOT_STEP
            || localX < NihilityTerminalLayout.STORAGE_X
            || localX >= NihilityTerminalLayout.STORAGE_X + NihilityTerminalMenu.STORAGE_COLUMNS * NihilityTerminalLayout.SLOT_STEP) {
            return Optional.empty();
        }

        int col = (localX - NihilityTerminalLayout.STORAGE_X) / NihilityTerminalLayout.SLOT_STEP;
        int row = (localY - NihilityTerminalLayout.STORAGE_Y) / NihilityTerminalLayout.SLOT_STEP;
        int slotIndex = col + row * NihilityTerminalMenu.STORAGE_COLUMNS;
        if ((localX - NihilityTerminalLayout.STORAGE_X) % NihilityTerminalLayout.SLOT_STEP >= NihilityTerminalLayout.SLOT_INNER_SIZE
            || (localY - NihilityTerminalLayout.STORAGE_Y) % NihilityTerminalLayout.SLOT_STEP >= NihilityTerminalLayout.SLOT_INNER_SIZE
            || slotIndex < 0 || slotIndex >= NihilityTerminalMenu.VISIBLE_SLOTS) {
            return Optional.empty();
        }

        ItemStack stack = menu.slots.get(slotIndex).getItem();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy());
    }

    public Rect2i getJeiSlotArea(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        int col = Math.max(0, Math.min(NihilityTerminalMenu.STORAGE_COLUMNS - 1,
            (localX - NihilityTerminalLayout.STORAGE_X) / NihilityTerminalLayout.SLOT_STEP));
        int row = Math.max(0, Math.min(NihilityTerminalMenu.STORAGE_ROWS - 1,
            (localY - NihilityTerminalLayout.STORAGE_Y) / NihilityTerminalLayout.SLOT_STEP));
        return new Rect2i(leftPos + NihilityTerminalLayout.STORAGE_X + col * NihilityTerminalLayout.SLOT_STEP,
            topPos + NihilityTerminalLayout.STORAGE_Y + row * NihilityTerminalLayout.SLOT_STEP,
            NihilityTerminalLayout.SLOT_INNER_SIZE, NihilityTerminalLayout.SLOT_INNER_SIZE);
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

    private Button addMainButton(Button button) {
        mainButtons.add(button);
        return addRenderableWidget(button);
    }

    private void onChunkloadPressed() {
        if (menu.isNetworkChunkLoaded()) {
            setConfirmingDisableChunkload(true);
            return;
        }
        sendButton(NihilityTerminalMenu.BUTTON_TOGGLE_CHUNKLOAD);
    }

    private void confirmDisableChunkload() {
        setConfirmingDisableChunkload(false);
        if (menu.isNetworkChunkLoaded()) {
            sendButton(NihilityTerminalMenu.BUTTON_TOGGLE_CHUNKLOAD);
        }
    }

    private void setConfirmingDisableChunkload(boolean value) {
        confirmingDisableChunkload = value;
        if (searchBox != null) {
            searchBox.active = !value;
        }
        if (confirmDisableButton != null) {
            confirmDisableButton.visible = value;
            confirmDisableButton.active = value;
        }
        if (cancelDisableButton != null) {
            cancelDisableButton.visible = value;
            cancelDisableButton.active = value;
        }
        refreshButtonStates();
    }

    private Component chunkloadButtonText() {
        return Component.translatable(menu.isNetworkChunkLoaded()
            ? "screen.templenihility.chunkload_state_on"
            : "screen.templenihility.chunkload_state_off");
    }

    private Component breakProtectionButtonText() {
        return Component.translatable(menu.isNetworkBreakProtected()
            ? "screen.templenihility.protection_state_on"
            : "screen.templenihility.protection_state_off");
    }

    private void refreshButtonStates() {
        boolean enabled = !confirmingDisableChunkload;
        for (Button button : mainButtons) {
            button.active = enabled;
        }
        if (clearSearchButton != null) {
            clearSearchButton.active = enabled && searchBox != null && !searchBox.getValue().isBlank();
        }
        if (chunkloadButton != null) {
            chunkloadButton.active = enabled && menu.getVaultCount() > 0;
        }
        if (breakProtectionButton != null) {
            breakProtectionButton.active = enabled && menu.getVaultCount() > 0;
        }
    }

    private void drawCapacityBar(GuiGraphicsExtractor graphics) {
        int x = NihilityTerminalLayout.CAPACITY_BAR_X;
        int y = NihilityTerminalLayout.CAPACITY_BAR_Y;
        int width = NihilityTerminalLayout.CAPACITY_BAR_WIDTH;
        int total = Math.max(1, menu.getTotalSlots());
        int used = Math.max(0, Math.min(menu.getUsedSlots(), total));
        int filled = Math.max(0, Math.min(width, used * width / total));
        graphics.fill(x, y, x + width, y + NihilityTerminalLayout.CAPACITY_BAR_HEIGHT, 0x55180F22);
        graphics.fill(x, y, x + filled, y + NihilityTerminalLayout.CAPACITY_BAR_HEIGHT, 0xAA4DB8C8);
        graphics.outline(x - 1, y - 1, width + 2, NihilityTerminalLayout.CAPACITY_BAR_HEIGHT + 2, 0x663F314D);
    }

    private void drawStorageScrollbar(GuiGraphicsExtractor graphics) {
        int maxScroll = menu.getMaxScrollOffset();
        if (maxScroll <= 0) {
            return;
        }
        int x = NihilityTerminalLayout.SCROLLBAR_X;
        int y = NihilityTerminalLayout.SCROLLBAR_Y;
        int height = NihilityTerminalMenu.STORAGE_ROWS * NihilityTerminalLayout.SLOT_STEP;
        int thumbHeight = Math.max(12, height * NihilityTerminalMenu.STORAGE_ROWS
            / (NihilityTerminalMenu.STORAGE_ROWS + maxScroll));
        int thumbY = y + (height - thumbHeight) * menu.getScrollOffset() / maxScroll;
        graphics.fill(x, y, x + NihilityTerminalLayout.SCROLLBAR_WIDTH, y + height, 0x663F314D);
        graphics.fill(x + 1, thumbY, x + 3, thumbY + thumbHeight, 0xCC4DB8C8);
    }

    private void drawStorageEmptyHint(GuiGraphicsExtractor graphics) {
        if (menu.getFilteredSlotCount() > 0) {
            return;
        }
        boolean searching = searchBox != null && !searchBox.getValue().isBlank();
        Component message = Component.translatable(searching
            ? "screen.templenihility.no_results"
            : "screen.templenihility.vault_empty");
        graphics.textWithWordWrap(font, message,
            NihilityTerminalLayout.STORAGE_X + 8,
            NihilityTerminalLayout.STORAGE_Y + 27,
            NihilityTerminalMenu.STORAGE_COLUMNS * NihilityTerminalLayout.SLOT_STEP - 18,
            0xFF7A6C85);
    }

    private void addPanelTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean hoveringFilledSlot = hoveredSlot != null && hoveredSlot.hasItem();
        if (!hoveringFilledSlot && isHoveringLocal(NihilityTerminalLayout.STORAGE_X, NihilityTerminalLayout.STORAGE_Y,
            NihilityTerminalMenu.STORAGE_COLUMNS * NihilityTerminalLayout.SLOT_STEP,
            NihilityTerminalMenu.STORAGE_ROWS * NihilityTerminalLayout.SLOT_STEP, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                Component.translatable("screen.templenihility.storage_tip_1"),
                Component.translatable("screen.templenihility.storage_tip_2"),
                Component.translatable("screen.templenihility.storage_tip_3")
            ), mouseX, mouseY);
            return;
        }
        if (!hoveringFilledSlot && isHoveringLocal(NihilityTerminalLayout.CRAFT_GRID_X, NihilityTerminalLayout.CRAFT_GRID_Y,
            3 * NihilityTerminalLayout.SLOT_STEP, 4 * NihilityTerminalLayout.SLOT_STEP, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                Component.translatable("screen.templenihility.crafting_tip_1"),
                Component.translatable("screen.templenihility.crafting_tip_2")
            ), mouseX, mouseY);
            return;
        }
        if (isHoveringLocal(NihilityTerminalLayout.CAPACITY_BAR_X, NihilityTerminalLayout.CAPACITY_BAR_Y - 10,
            NihilityTerminalLayout.CAPACITY_BAR_WIDTH, 14, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(
                "screen.templenihility.capacity_tip",
                formatCount(menu.getUsedSlots()), formatCount(menu.getTotalSlots()), formatCount(menu.getEmptySlots())),
                mouseX, mouseY);
        }
    }

    private boolean isHoveringLocal(int x, int y, int width, int height, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    private String formatCount(int value) {
        if (value < 1_000) {
            return Integer.toString(value);
        }
        if (value < 1_000_000) {
            return (value / 1_000) + "K";
        }
        return (value / 1_000_000) + "M";
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, String itemCount) {
        if (menu.isStorageSlot(slot.index) && !itemStack.isEmpty()) {
            ItemStack visual = itemStack.copyWithCount(Math.min(itemStack.getCount(), itemStack.getMaxStackSize()));
            super.renderSlotContents(graphics, visual, slot, menu.getVisibleCountText(slot.index));
            return;
        }
        super.renderSlotContents(graphics, itemStack, slot, itemCount);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && menu.isStorageSlot(hoveredSlot.index)) {
            lines.add(Component.translatable("screen.templenihility.aggregate_total",
                menu.getVisibleItemCount(hoveredSlot.index)));
        }
        return lines;
    }

    private void extractDisableChunkloadConfirm(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, imageWidth, imageHeight, 0xB8000000);
        graphics.fill(NihilityTerminalLayout.CONFIRM_PANEL_X, NihilityTerminalLayout.CONFIRM_PANEL_Y,
            NihilityTerminalLayout.CONFIRM_PANEL_X + NihilityTerminalLayout.CONFIRM_PANEL_WIDTH,
            NihilityTerminalLayout.CONFIRM_PANEL_Y + NihilityTerminalLayout.CONFIRM_PANEL_HEIGHT, 0xF21B1426);
        graphics.outline(NihilityTerminalLayout.CONFIRM_PANEL_X, NihilityTerminalLayout.CONFIRM_PANEL_Y,
            NihilityTerminalLayout.CONFIRM_PANEL_WIDTH, NihilityTerminalLayout.CONFIRM_PANEL_HEIGHT, 0xFF7EEBFF);
        graphics.outline(NihilityTerminalLayout.CONFIRM_PANEL_X + 3, NihilityTerminalLayout.CONFIRM_PANEL_Y + 3,
            NihilityTerminalLayout.CONFIRM_PANEL_WIDTH - 6, NihilityTerminalLayout.CONFIRM_PANEL_HEIGHT - 6, 0xFF493560);
        graphics.centeredText(font, Component.translatable("screen.templenihility.confirm_disable_chunkload_title"),
            imageWidth / 2, NihilityTerminalLayout.CONFIRM_TITLE_Y, 0xFFE9D8FF);
        graphics.textWithWordWrap(font,
            Component.translatable("screen.templenihility.confirm_disable_chunkload_message"),
            NihilityTerminalLayout.CONFIRM_MESSAGE_X, NihilityTerminalLayout.CONFIRM_MESSAGE_Y,
            NihilityTerminalLayout.CONFIRM_MESSAGE_WIDTH, 0xFFB9A8D6);
    }

    private void extractVoidShards(GuiGraphicsExtractor graphics, float partialTick) {
        float time = animationTicks + partialTick;
        int pulse = (int) ((Math.sin(time * 0.08F) + 1.0F) * 16.0F);
        int lineColor = (0x38 + pulse) << 24 | 0x7EEBFF;
        int darkGlow = 0x240B0614;

        graphics.fill(leftPos + NihilityTerminalLayout.LEFT_GLOW_X, topPos + NihilityTerminalLayout.LEFT_GLOW_Y,
            leftPos + NihilityTerminalLayout.LEFT_GLOW_X + NihilityTerminalLayout.LEFT_GLOW_WIDTH,
            topPos + NihilityTerminalLayout.LEFT_GLOW_Y + NihilityTerminalLayout.LEFT_GLOW_HEIGHT, darkGlow);
        graphics.fill(leftPos + NihilityTerminalLayout.RIGHT_GLOW_X, topPos + NihilityTerminalLayout.RIGHT_GLOW_Y,
            leftPos + NihilityTerminalLayout.RIGHT_GLOW_X + NihilityTerminalLayout.RIGHT_GLOW_WIDTH,
            topPos + NihilityTerminalLayout.RIGHT_GLOW_Y + NihilityTerminalLayout.RIGHT_GLOW_HEIGHT, darkGlow);
        graphics.fill(leftPos + NihilityTerminalLayout.TOP_GLOW_X, topPos + NihilityTerminalLayout.TOP_GLOW_Y,
            leftPos + NihilityTerminalLayout.TOP_GLOW_X + NihilityTerminalLayout.TOP_GLOW_WIDTH,
            topPos + NihilityTerminalLayout.TOP_GLOW_Y + NihilityTerminalLayout.TOP_GLOW_HEIGHT, lineColor);
        graphics.fill(leftPos + NihilityTerminalLayout.BOTTOM_GLOW_X, topPos + NihilityTerminalLayout.BOTTOM_GLOW_Y,
            leftPos + NihilityTerminalLayout.BOTTOM_GLOW_X + NihilityTerminalLayout.BOTTOM_GLOW_WIDTH,
            topPos + NihilityTerminalLayout.BOTTOM_GLOW_Y + NihilityTerminalLayout.BOTTOM_GLOW_HEIGHT, lineColor);

        drawFloatingShard(graphics, time, NihilityTerminalLayout.SHARD_LEFT_TOP_X,
            NihilityTerminalLayout.SHARD_LEFT_TOP_Y, NihilityTerminalLayout.SHARD_LEFT_TOP_SOURCE_X,
            NihilityTerminalLayout.SHARD_LEFT_TOP_SOURCE_Y, NihilityTerminalLayout.SHARD_LEFT_TOP_WIDTH,
            NihilityTerminalLayout.SHARD_LEFT_TOP_HEIGHT, 0.050F, 5.0F);
        drawFloatingShard(graphics, time, NihilityTerminalLayout.SHARD_RIGHT_TOP_X,
            NihilityTerminalLayout.SHARD_RIGHT_TOP_Y, NihilityTerminalLayout.SHARD_RIGHT_TOP_SOURCE_X,
            NihilityTerminalLayout.SHARD_RIGHT_TOP_SOURCE_Y, NihilityTerminalLayout.SHARD_RIGHT_TOP_WIDTH,
            NihilityTerminalLayout.SHARD_RIGHT_TOP_HEIGHT, 0.043F, 4.0F);
        drawFloatingShard(graphics, time, NihilityTerminalLayout.SHARD_LEFT_MIDDLE_X,
            NihilityTerminalLayout.SHARD_LEFT_MIDDLE_Y, NihilityTerminalLayout.SHARD_LEFT_MIDDLE_SOURCE_X,
            NihilityTerminalLayout.SHARD_LEFT_MIDDLE_SOURCE_Y, NihilityTerminalLayout.SHARD_LEFT_MIDDLE_WIDTH,
            NihilityTerminalLayout.SHARD_LEFT_MIDDLE_HEIGHT, 0.035F, 3.0F);
        drawFloatingShard(graphics, time, NihilityTerminalLayout.SHARD_RIGHT_BOTTOM_X,
            NihilityTerminalLayout.SHARD_RIGHT_BOTTOM_Y, NihilityTerminalLayout.SHARD_RIGHT_BOTTOM_SOURCE_X,
            NihilityTerminalLayout.SHARD_RIGHT_BOTTOM_SOURCE_Y, NihilityTerminalLayout.SHARD_RIGHT_BOTTOM_WIDTH,
            NihilityTerminalLayout.SHARD_RIGHT_BOTTOM_HEIGHT, 0.047F, 5.0F);
        drawFloatingShard(graphics, time, NihilityTerminalLayout.SHARD_TOP_X,
            NihilityTerminalLayout.SHARD_TOP_Y, NihilityTerminalLayout.SHARD_TOP_SOURCE_X,
            NihilityTerminalLayout.SHARD_TOP_SOURCE_Y, NihilityTerminalLayout.SHARD_TOP_WIDTH,
            NihilityTerminalLayout.SHARD_TOP_HEIGHT, 0.041F, 3.0F);
        drawFloatingShard(graphics, time, NihilityTerminalLayout.SHARD_BOTTOM_X,
            NihilityTerminalLayout.SHARD_BOTTOM_Y, NihilityTerminalLayout.SHARD_BOTTOM_SOURCE_X,
            NihilityTerminalLayout.SHARD_BOTTOM_SOURCE_Y, NihilityTerminalLayout.SHARD_BOTTOM_WIDTH,
            NihilityTerminalLayout.SHARD_BOTTOM_HEIGHT, 0.038F, 4.0F);
    }

    private void drawFloatingShard(GuiGraphicsExtractor graphics, float time, int baseX, int baseY,
                                   int sourceX, int sourceY, int width, int height,
                                   float speed, float drift) {
        int x = leftPos + baseX + Math.round((float) Math.sin(time * speed + baseX * 0.11F) * drift);
        int y = topPos + baseY + Math.round((float) Math.cos(time * speed + baseY * 0.07F) * drift);
        graphics.blit(RenderPipelines.GUI_TEXTURED, VOID_SHARDS, x, y, sourceX, sourceY, width, height,
            NihilityTerminalLayout.SHARD_ATLAS_WIDTH, NihilityTerminalLayout.SHARD_ATLAS_HEIGHT);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
