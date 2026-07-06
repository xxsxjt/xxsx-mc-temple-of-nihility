package com.templenihility.menu;

import com.templenihility.blockentity.NihilityAltarBlockEntity;
import com.templenihility.init.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NihilityAltarMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int PLAYER_INV_START = 2;
    public static final int BUTTON_PERFORM_RITUAL = 0;

    private final Container altar;
    private final NihilityAltarBlockEntity blockEntity;
    private final int[] data = new int[5];

    public NihilityAltarMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(2), null);
    }

    public NihilityAltarMenu(int id, Inventory inventory, NihilityAltarBlockEntity altar) {
        this(id, inventory, altar, altar);
    }

    private NihilityAltarMenu(int id, Inventory inventory, Container altar, NihilityAltarBlockEntity blockEntity) {
        super(ModMenus.NIHILITY_ALTAR.get(), id);
        this.altar = altar;
        this.blockEntity = blockEntity;

        addSlot(new InputSlot(altar, INPUT_SLOT, 56, 35));
        addSlot(new OutputSlot(altar, OUTPUT_SLOT, 116, 35));
        addStandardInventorySlots(inventory, 8, 84);

        refreshStructureData();
        for (int i = 0; i < data.length; i++) {
            addDataSlot(DataSlot.shared(data, i));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_PERFORM_RITUAL && blockEntity != null) {
            boolean changed = blockEntity.performRitual(player);
            refreshStructureData();
            broadcastFullState();
            return changed;
        }
        return super.clickMenuButton(player, buttonId);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        refreshStructureData();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index == INPUT_SLOT || index == OUTPUT_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, INPUT_SLOT, INPUT_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return altar.stillValid(player);
    }

    public int getTier() {
        return data[0];
    }

    public int getRuneCount() {
        return data[1];
    }

    public int getCrystalCount() {
        return data[2];
    }

    public int getPillarCount() {
        return data[3];
    }

    public int getChiseledCount() {
        return data[4];
    }

    public String getTierNameKey() {
        return NihilityAltarBlockEntity.tierNameKey(getTier());
    }

    private void refreshStructureData() {
        if (blockEntity == null) {
            return;
        }
        int[] source = blockEntity.getStructureData();
        System.arraycopy(source, 0, data, 0, Math.min(source.length, data.length));
    }

    private static final class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
