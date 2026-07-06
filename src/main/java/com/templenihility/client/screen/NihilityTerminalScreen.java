package com.templenihility.client.screen;

import com.templenihility.TempleNihilityMod;
import com.templenihility.menu.NihilityTerminalMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private Button chunkloadButton;
    private Button breakProtectionButton;
    private Button confirmDisableButton;
    private Button cancelDisableButton;
    private final List<Button> mainButtons = new ArrayList<>();
    private boolean syncingSearch;
    private boolean confirmingDisableChunkload;
    private int animationTicks;

    public NihilityTerminalScreen(NihilityTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 248, 248);
        this.inventoryLabelY = 154;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;

        searchBox = new EditBox(font, leftPos + 8, topPos + 32, 76, 16,
            Component.translatable("screen.templenihility.search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::syncSearch);
        addRenderableWidget(searchBox);

        addMainButton(Button.builder(Component.literal("<"), button -> sendButton(NihilityTerminalMenu.BUTTON_PREV_PAGE))
            .bounds(leftPos + 88, topPos + 32, 17, 16)
            .build());
        addMainButton(Button.builder(Component.literal(">"), button -> sendButton(NihilityTerminalMenu.BUTTON_NEXT_PAGE))
            .bounds(leftPos + 107, topPos + 32, 17, 16)
            .build());
        addMainButton(Button.builder(Component.translatable("screen.templenihility.sort_short"),
                button -> sendButton(NihilityTerminalMenu.BUTTON_CYCLE_SORT))
            .bounds(leftPos + 128, topPos + 32, 34, 16)
            .build());
        addMainButton(Button.builder(Component.literal("X"), button -> {
                searchBox.setValue("");
                sendButton(NihilityTerminalMenu.BUTTON_CLEAR_SEARCH);
            })
            .bounds(leftPos + 164, topPos + 32, 14, 16)
            .build());
        chunkloadButton = addMainButton(Button.builder(chunkloadButtonText(), button -> onChunkloadPressed())
            .bounds(leftPos + 181, topPos + 15, 58, 15)
            .build());
        breakProtectionButton = addMainButton(Button.builder(breakProtectionButtonText(),
                button -> sendButton(NihilityTerminalMenu.BUTTON_TOGGLE_BREAK_PROTECTION))
            .bounds(leftPos + 181, topPos + 32, 58, 15)
            .build());

        confirmDisableButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.confirm_disable_chunkload_yes"),
                button -> confirmDisableChunkload())
            .bounds(leftPos + 64, topPos + 132, 54, 18)
            .build());
        cancelDisableButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.confirm_disable_chunkload_no"),
                button -> setConfirmingDisableChunkload(false))
            .bounds(leftPos + 130, topPos + 132, 54, 18)
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
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF3F314D, false);
        graphics.text(font, Component.translatable("screen.templenihility.vault_status_line",
            menu.getVaultCount(), menu.getFilteredSlotCount(), menu.getStoredItemCount()),
            8, 18, 0xFF5A4B68, false);
        graphics.text(font, Component.translatable("screen.templenihility.crafting_label"),
            181, 51, 0xFF5A4B68, false);
        graphics.text(font, Component.translatable("screen.templenihility.vault_stats_compact",
            menu.getUsedSlots(), menu.getTotalSlots(), menu.getEmptySlots()),
            8, 145, 0xFF5A4B68, false);
        graphics.text(font, Component.translatable(menu.getSortLabelKey()), 108, 145, 0xFF396E78, false);
        graphics.text(font, Component.literal((menu.getPage() + 1) + "/" + (menu.getMaxPage() + 1)), 146, 18, 0xFF396E78, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF3F314D, false);

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
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (confirmingDisableChunkload && event.isEscape()) {
            setConfirmingDisableChunkload(false);
            return true;
        }
        return super.keyPressed(event);
    }

    public Rect2i getSearchAreaForJei() {
        return new Rect2i(leftPos + 8, topPos + 32, 76, 16);
    }

    public Optional<ItemStack> getJeiIngredientUnderMouse(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localY < 55 || localY >= 55 + NihilityTerminalMenu.STORAGE_ROWS * 18
            || localX < 8 || localX >= 8 + NihilityTerminalMenu.STORAGE_COLUMNS * 18) {
            return Optional.empty();
        }

        int col = (localX - 8) / 18;
        int row = (localY - 55) / 18;
        int slotIndex = col + row * NihilityTerminalMenu.STORAGE_COLUMNS;
        if ((localX - 8) % 18 >= 16 || (localY - 55) % 18 >= 16 || slotIndex < 0 || slotIndex >= NihilityTerminalMenu.VISIBLE_SLOTS) {
            return Optional.empty();
        }

        ItemStack stack = menu.slots.get(slotIndex).getItem();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy());
    }

    public Rect2i getJeiSlotArea(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        int col = Math.max(0, Math.min(NihilityTerminalMenu.STORAGE_COLUMNS - 1, (localX - 8) / 18));
        int row = Math.max(0, Math.min(NihilityTerminalMenu.STORAGE_ROWS - 1, (localY - 55) / 18));
        return new Rect2i(leftPos + 8 + col * 18, topPos + 55 + row * 18, 16, 16);
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
        for (Button button : mainButtons) {
            button.active = !value;
        }
        if (confirmDisableButton != null) {
            confirmDisableButton.visible = value;
            confirmDisableButton.active = value;
        }
        if (cancelDisableButton != null) {
            cancelDisableButton.visible = value;
            cancelDisableButton.active = value;
        }
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
        graphics.fill(48, 74, 200, 156, 0xF21B1426);
        graphics.outline(48, 74, 152, 82, 0xFF7EEBFF);
        graphics.outline(51, 77, 146, 76, 0xFF493560);
        graphics.centeredText(font, Component.translatable("screen.templenihility.confirm_disable_chunkload_title"),
            imageWidth / 2, 84, 0xFFE9D8FF);
        graphics.textWithWordWrap(font,
            Component.translatable("screen.templenihility.confirm_disable_chunkload_message"),
            60, 102, 128, 0xFFB9A8D6);
    }

    private void extractVoidShards(GuiGraphicsExtractor graphics, float partialTick) {
        float time = animationTicks + partialTick;
        int pulse = (int) ((Math.sin(time * 0.08F) + 1.0F) * 16.0F);
        int lineColor = (0x38 + pulse) << 24 | 0x7EEBFF;
        int darkGlow = 0x240B0614;

        graphics.fill(leftPos - 4, topPos + 6, leftPos - 2, topPos + imageHeight - 10, darkGlow);
        graphics.fill(leftPos + imageWidth + 2, topPos + 12, leftPos + imageWidth + 4, topPos + imageHeight - 16, darkGlow);
        graphics.fill(leftPos + 8, topPos - 4, leftPos + imageWidth - 8, topPos - 2, lineColor);
        graphics.fill(leftPos + 12, topPos + imageHeight + 2, leftPos + imageWidth - 12, topPos + imageHeight + 4, lineColor);

        drawFloatingShard(graphics, time, -27, 10, 0, 0, 23, 23, 0.050F, 5.0F);
        drawFloatingShard(graphics, time, imageWidth + 5, 30, 24, 0, 22, 22, 0.043F, 4.0F);
        drawFloatingShard(graphics, time, -22, 142, 0, 24, 18, 28, 0.035F, 3.0F);
        drawFloatingShard(graphics, time, imageWidth + 8, 166, 32, 24, 20, 26, 0.047F, 5.0F);
        drawFloatingShard(graphics, time, 42, -19, 16, 40, 26, 18, 0.041F, 3.0F);
        drawFloatingShard(graphics, time, 112, imageHeight + 4, 38, 3, 20, 24, 0.038F, 4.0F);
    }

    private void drawFloatingShard(GuiGraphicsExtractor graphics, float time, int baseX, int baseY,
                                   int sourceX, int sourceY, int width, int height,
                                   float speed, float drift) {
        int x = leftPos + baseX + Math.round((float) Math.sin(time * speed + baseX * 0.11F) * drift);
        int y = topPos + baseY + Math.round((float) Math.cos(time * speed + baseY * 0.07F) * drift);
        graphics.blit(RenderPipelines.GUI_TEXTURED, VOID_SHARDS, x, y, sourceX, sourceY, width, height, 64, 64);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
