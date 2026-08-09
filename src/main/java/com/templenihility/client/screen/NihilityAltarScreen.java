package com.templenihility.client.screen;

import com.templenihility.TempleNihilityMod;
import com.templenihility.blockentity.NihilityAltarBlockEntity;
import com.templenihility.challenge.SaintChallengePolicy.StartStatus;
import com.templenihility.challenge.SaintChallengeService;
import com.templenihility.menu.NihilityAltarMenu;
import com.templenihility.ritual.NihilityRitualService;
import com.templenihility.ritual.RitualExecutionPolicy.Status;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class NihilityAltarScreen extends AbstractContainerScreen<NihilityAltarMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/gui/container/nihility_altar.png");
    private int animationTicks;
    private Button ritualButton;
    private Button challengeButton;

    public NihilityAltarScreen(NihilityAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 75;
    }

    @Override
    protected void init() {
        super.init();
        ritualButton = addRenderableWidget(Button.builder(Component.translatable("screen.templenihility.altar_button"),
                button -> sendButton(NihilityAltarMenu.BUTTON_PERFORM_RITUAL))
            .bounds(leftPos + 126, topPos + 16, 42, 16)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.altar_button_tip")))
            .build());
        challengeButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.altar_challenge"),
                button -> sendButton(NihilityAltarMenu.BUTTON_TOGGLE_SAINT_CHALLENGE))
            .bounds(leftPos + 8, topPos + 34, 42, 16)
            .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        animationTicks++;
        if (ritualButton != null) {
            ritualButton.active = menu.getRitualStatus() == Status.READY;
        }
        if (challengeButton != null) {
            StartStatus status = menu.getSaintChallengeStatus();
            challengeButton.active = status.canClick();
            challengeButton.setMessage(Component.translatable(
                status == StartStatus.OWN_CHALLENGE
                    ? "screen.templenihility.altar_challenge_quit"
                    : "screen.templenihility.altar_challenge"));
            challengeButton.setTooltip(Tooltip.create(Component.translatable(
                SaintChallengeService.messageKey(status))));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        graphics.text(font, Component.translatable("screen.templenihility.altar_tier",
            Component.translatable(menu.getTierNameKey())), 8, 18, 0xFF566064, false);
        graphics.text(font, Component.translatable("screen.templenihility.altar_input"), 50, 26, 0xFF6B7476, false);
        graphics.text(font, Component.translatable("screen.templenihility.altar_output"), 110, 26, 0xFF6B7476, false);
        drawErasureGlyph(graphics);
        drawRitualPreview(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("screen.templenihility.altar_parts",
            menu.getRuneCount(), menu.getCrystalCount(), menu.getPillarCount(), menu.getChiseledCount()),
            8, 65, 0xFF68777A, false);
        if (isStructureHovered(mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                Component.translatable("screen.templenihility.altar_structure_tip_1"),
                Component.translatable("screen.templenihility.altar_structure_tip_2"),
                Component.translatable("screen.templenihility.altar_structure_tip_3")
            ), mouseX, mouseY);
        }
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }

    private void drawErasureGlyph(GuiGraphicsExtractor graphics) {
        int pulse = (int) ((Math.sin(animationTicks * 0.16) + 1.0) * 32.0);
        int bright = (0x90 + pulse) << 24 | 0xC5C9CC;
        graphics.fill(78, 43, 85, 44, bright);
        graphics.fill(91, 43, 98, 44, bright);
        graphics.fill(87, 41, 89, 43, 0x668C9196);
        graphics.fill(87, 45, 89, 47, 0x668C9196);
    }

    private void drawRitualPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack input = menu.getSlot(NihilityAltarMenu.INPUT_SLOT).getItem();
        NihilityRitualService.RitualPreview preview = menu.getRitualPreview();
        Component status;
        int color;
        Status ritualStatus = menu.getRitualStatus();
        if (ritualStatus == Status.NO_INPUT) {
            status = Component.translatable("screen.templenihility.altar_preview_empty");
            color = 0xFF7B8587;
        } else if (ritualStatus == Status.NO_RECIPE || preview == null) {
            status = Component.translatable("screen.templenihility.altar_preview_unknown");
            color = 0xFF9A6666;
        } else if (ritualStatus == Status.NOT_ENOUGH_INPUT) {
            status = Component.translatable("screen.templenihility.altar_preview_missing",
                input.getCount(), preview.inputCount());
            color = 0xFF9A7657;
        } else if (ritualStatus == Status.TIER_TOO_LOW) {
            status = Component.translatable("screen.templenihility.altar_preview_tier",
                Component.translatable(NihilityAltarBlockEntity.tierNameKey(preview.minTier())));
            color = 0xFF9A7657;
        } else if (ritualStatus == Status.OUTPUT_BLOCKED) {
            status = Component.translatable("screen.templenihility.altar_preview_output");
            color = 0xFF9A6666;
        } else if (ritualStatus == Status.NOT_ENOUGH_ENERGY) {
            status = Component.translatable("screen.templenihility.altar_preview_energy", preview.energyCost());
            color = 0xFF9A7657;
        } else {
            status = Component.translatable("screen.templenihility.altar_preview_ready",
                preview.result().getCount(), preview.result().getHoverName(), preview.energyCost());
            color = 0xFF52777A;
        }

        graphics.text(font, compact(status, 160), 8, 55, color, false);
        if (preview != null && isPreviewHovered(mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                Component.translatable("screen.templenihility.altar_preview_input",
                    preview.inputCount(), input.getHoverName()),
                Component.translatable("screen.templenihility.altar_preview_result",
                    preview.result().getCount(), preview.result().getHoverName()),
                Component.translatable("screen.templenihility.altar_preview_requirement",
                    Component.translatable(NihilityAltarBlockEntity.tierNameKey(preview.minTier())),
                    preview.energyCost())
            ), mouseX, mouseY);
        }
    }

    private Component compact(Component component, int maxWidth) {
        String text = component.getString();
        if (font.width(text) <= maxWidth) {
            return component;
        }
        String suffix = "...";
        return Component.literal(font.plainSubstrByWidth(text, maxWidth - font.width(suffix)) + suffix);
    }

    private boolean isPreviewHovered(int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= 6 && localX < 170 && localY >= 52 && localY < 65;
    }

    private boolean isStructureHovered(int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= 6 && localX < 170 && localY >= 64 && localY < 75;
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
