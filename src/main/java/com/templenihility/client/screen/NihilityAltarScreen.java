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
        super(menu, inventory, title, 192, 212);
        this.titleLabelX = 10;
        this.titleLabelY = 7;
        this.inventoryLabelX = 15;
        this.inventoryLabelY = 122;
    }

    @Override
    protected void init() {
        super.init();
        challengeButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.altar_challenge"),
                button -> sendButton(NihilityAltarMenu.BUTTON_TOGGLE_SAINT_CHALLENGE))
            .bounds(leftPos + 14, topPos + 66, 76, 16)
            .build());
        ritualButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.templenihility.altar_button"),
                button -> sendButton(NihilityAltarMenu.BUTTON_PERFORM_RITUAL))
            .bounds(leftPos + 102, topPos + 66, 76, 16)
            .tooltip(Tooltip.create(Component.translatable("screen.templenihility.altar_button_tip")))
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
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFEAF8FB, false);

        // Tier pill text centered inside (96..184, y=8)
        Component tierComp = compact(Component.translatable(menu.getTierNameKey()), 82);
        int tierX = 96 + Math.max(2, (88 - font.width(tierComp)) / 2);
        int tierColor = menu.getTier() >= 3 ? 0xFF5EEAD4 : (menu.getTier() >= 1 ? 0xFF66D9E8 : 0xFF94A3B8);
        graphics.text(font, tierComp, tierX, 8, tierColor, false);

        // Centered slot labels above Input (46..62 -> center 54) and Output (130..146 -> center 138)
        Component inLabel = Component.translatable("screen.templenihility.altar_input");
        Component outLabel = Component.translatable("screen.templenihility.altar_output");
        graphics.text(font, inLabel, 54 - font.width(inLabel) / 2, 28, 0xFF8CB4C2, false);
        graphics.text(font, outLabel, 138 - font.width(outLabel) / 2, 28, 0xFF8CB4C2, false);

        drawVoidConduit(graphics);
        drawRitualPreview(graphics, mouseX, mouseY);

        Component parts = compact(Component.translatable("screen.templenihility.altar_parts",
            menu.getRuneCount(), menu.getCrystalCount(), menu.getPillarCount(), menu.getChiseledCount()), 168);
        graphics.text(font, parts, 12, 105, 0xFF8CB4C2, false);
        if (isStructureHovered(mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                Component.translatable("screen.templenihility.altar_structure_tip_1"),
                Component.translatable("screen.templenihility.altar_structure_tip_2"),
                Component.translatable("screen.templenihility.altar_structure_tip_3")
            ), mouseX, mouseY);
        }

        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF8CB4C2, false);
    }

    private void drawVoidConduit(GuiGraphicsExtractor graphics) {
        boolean ready = menu.getRitualStatus() == Status.READY;
        double wave = (Math.sin(animationTicks * 0.20) + 1.0) * 0.5;
        int alpha = ready ? (int) (185 + wave * 70) : (int) (95 + wave * 45);
        int barColor = (alpha << 24) | (ready ? 0x5EEAD4 : 0x528896);
        int coreColor = (alpha << 24) | (ready ? 0xCCFBF1 : 0x8CB4C2);

        graphics.fill(73, 50, 115, 51, barColor);
        int pulseX = 74 + (animationTicks * (ready ? 2 : 1)) % 38;
        graphics.fill(pulseX, 49, Math.min(115, pulseX + 6), 52, coreColor);
        graphics.fill(95, 49, 98, 52, ready ? 0xFF5EEAD4 : 0xFF476978);
    }

    private void drawRitualPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack input = menu.getSlot(NihilityAltarMenu.INPUT_SLOT).getItem();
        NihilityRitualService.RitualPreview preview = menu.getRitualPreview();
        Component status;
        int color;
        Status ritualStatus = menu.getRitualStatus();
        if (ritualStatus == Status.NO_INPUT) {
            status = Component.translatable("screen.templenihility.altar_preview_empty");
            color = 0xFF94A3B8;
        } else if (ritualStatus == Status.NO_RECIPE || preview == null) {
            status = Component.translatable("screen.templenihility.altar_preview_unknown");
            color = 0xFFF87171;
        } else if (ritualStatus == Status.NOT_ENOUGH_INPUT) {
            status = Component.translatable("screen.templenihility.altar_preview_missing",
                input.getCount(), preview.inputCount());
            color = 0xFFFBBF24;
        } else if (ritualStatus == Status.TIER_TOO_LOW) {
            status = Component.translatable("screen.templenihility.altar_preview_tier",
                Component.translatable(NihilityAltarBlockEntity.tierNameKey(preview.minTier())));
            color = 0xFFFBBF24;
        } else if (ritualStatus == Status.OUTPUT_BLOCKED) {
            status = Component.translatable("screen.templenihility.altar_preview_output");
            color = 0xFFF87171;
        } else if (ritualStatus == Status.NOT_ENOUGH_ENERGY) {
            status = Component.translatable("screen.templenihility.altar_preview_energy", preview.energyCost());
            color = 0xFFFBBF24;
        } else {
            status = Component.translatable("screen.templenihility.altar_preview_ready",
                preview.result().getCount(), preview.result().getHoverName(), preview.energyCost());
            color = 0xFF5EEAD4;
        }
        graphics.text(font, compact(status, 168), 12, 95, color, false);
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
        return localX >= 8 && localX < 184 && localY >= 92 && localY < 104;
    }

    private boolean isStructureHovered(int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= 8 && localX < 184 && localY >= 104 && localY < 116;
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
