package com.templenihility.menu;

import com.templenihility.blockentity.NihilityAltarBlockEntity;
import com.templenihility.challenge.SaintChallengePolicy.StartStatus;
import com.templenihility.challenge.SaintChallengeService;
import com.templenihility.init.ModMenus;
import com.templenihility.ritual.NihilityRitualService;
import com.templenihility.ritual.RitualExecutionPolicy.Status;
import com.templenihility.energy.VoidPower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    public static final int PREVIEW_SLOT = 2;
    public static final int PLAYER_INV_START = 3;
    public static final int BUTTON_PERFORM_RITUAL = 0;
    public static final int BUTTON_TOGGLE_SAINT_CHALLENGE = 1;

    private final Container altar;
    private final Container preview;
    private final NihilityAltarBlockEntity blockEntity;
    private final net.minecraft.world.level.Level level;
    private final Player player;
    private final int[] data = new int[11];
    private long lastPeriodicRefresh = Long.MIN_VALUE;

    public NihilityAltarMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(2), new SimpleContainer(1), null);
    }

    public NihilityAltarMenu(int id, Inventory inventory, NihilityAltarBlockEntity altar) {
        this(id, inventory, altar, new SimpleContainer(1), altar);
    }

    private NihilityAltarMenu(int id, Inventory inventory, Container altar, Container preview,
                              NihilityAltarBlockEntity blockEntity) {
        super(ModMenus.NIHILITY_ALTAR.get(), id);
        this.altar = altar;
        this.preview = preview;
        this.blockEntity = blockEntity;
        this.level = inventory.player.level();
        this.player = inventory.player;
        this.data[9] = Status.NO_INPUT.ordinal();
        this.data[10] = StartStatus.PLAYER_INVALID.ordinal();

        addSlot(new InputSlot(altar, INPUT_SLOT, 56, 35));
        addSlot(new OutputSlot(altar, OUTPUT_SLOT, 116, 35));
        addSlot(new PreviewSlot(preview, 0, -1000, -1000));
        addStandardInventorySlots(inventory, 8, 84);

        refreshAllData();
        for (int i = 0; i < data.length; i++) {
            addDataSlot(DataSlot.shared(data, i));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_PERFORM_RITUAL && blockEntity != null) {
            boolean changed = blockEntity.performRitual(player);
            refreshAllData();
            broadcastFullState();
            return changed;
        }
        if (buttonId == BUTTON_TOGGLE_SAINT_CHALLENGE
            && blockEntity != null
            && level instanceof ServerLevel serverLevel
            && player instanceof ServerPlayer serverPlayer) {
            boolean changed = SaintChallengeService.toggle(
                serverLevel, blockEntity.getBlockPos(), serverPlayer, getTier());
            refreshAllData();
            broadcastFullState();
            if (changed) {
                serverPlayer.closeContainer();
            }
            return changed;
        }
        return super.clickMenuButton(player, buttonId);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        refreshAllData();
    }

    @Override
    public void broadcastChanges() {
        long gameTime = level.getGameTime();
        if (blockEntity != null
            && (lastPeriodicRefresh == Long.MIN_VALUE || gameTime - lastPeriodicRefresh >= 5)) {
            refreshAllData();
            lastPeriodicRefresh = gameTime;
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index == PREVIEW_SLOT) {
            return ItemStack.EMPTY;
        }
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

    public NihilityRitualService.RitualPreview getRitualPreview() {
        ItemStack result = slots.get(PREVIEW_SLOT).getItem();
        if (data[5] == 0 || result.isEmpty()) {
            return null;
        }
        return new NihilityRitualService.RitualPreview(data[6], result.copy(), data[7], data[8]);
    }

    public Status getRitualStatus() {
        return Status.byId(data[9]);
    }

    public StartStatus getSaintChallengeStatus() {
        return StartStatus.byId(data[10]);
    }

    private void refreshAllData() {
        if (blockEntity == null) {
            return;
        }
        int[] source = blockEntity.getStructureData();
        System.arraycopy(source, 0, data, 0, Math.min(source.length, 5));

        NihilityRitualService.RitualPlan plan = NihilityRitualService.inspect(
            level,
            altar.getItem(INPUT_SLOT),
            altar.getItem(OUTPUT_SLOT),
            data[0],
            VoidPower.get(player));
        data[9] = plan.status().ordinal();
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            data[10] = SaintChallengeService.inspect(
                serverLevel, blockEntity.getBlockPos(), serverPlayer, data[0]).status().ordinal();
        }
        if (plan.recipe().isEmpty()) {
            data[5] = 0;
            data[6] = 0;
            data[7] = 0;
            data[8] = 0;
            preview.setItem(0, ItemStack.EMPTY);
            return;
        }

        var recipe = plan.requiredRecipe();
        data[5] = 1;
        data[6] = recipe.inputCount();
        data[7] = recipe.minTier();
        data[8] = recipe.energyCost();
        preview.setItem(0, recipe.result());
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

    private static final class PreviewSlot extends Slot {
        private PreviewSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
