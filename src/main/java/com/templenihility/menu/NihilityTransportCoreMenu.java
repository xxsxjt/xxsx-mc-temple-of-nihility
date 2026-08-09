package com.templenihility.menu;

import com.templenihility.blockentity.NihilityTransportCoreBlockEntity;
import com.templenihility.init.ModMenus;
import com.templenihility.transport.TransportPolicy.BlockedReason;
import com.templenihility.transport.TransportPolicy.FilterMode;
import com.templenihility.transport.TransportPolicy.Priority;
import com.templenihility.transport.TransportPolicy.RedstoneMode;
import com.templenihility.transport.TransportPolicy.ResourceMode;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NihilityTransportCoreMenu extends AbstractContainerMenu {
    public static final int FILTER_SLOT_COUNT = NihilityTransportCoreBlockEntity.FILTER_SLOTS;
    public static final int PLAYER_INV_START = FILTER_SLOT_COUNT;

    public static final int BUTTON_INPUT_DIRECTION = 0;
    public static final int BUTTON_OUTPUT_DIRECTION = 1;
    public static final int BUTTON_RESOURCE_MODE = 2;
    public static final int BUTTON_PRIORITY = 3;
    public static final int BUTTON_REDSTONE_MODE = 4;
    public static final int BUTTON_FILTER_MODE = 5;
    public static final int BUTTON_CLEAR_FILTERS = 6;

    private final Container filters;
    private final NihilityTransportCoreBlockEntity blockEntity;
    private final int[] data = new int[NihilityTransportCoreBlockEntity.MENU_DATA_COUNT];

    public NihilityTransportCoreMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(FILTER_SLOT_COUNT), null);
    }

    public NihilityTransportCoreMenu(int id, Inventory inventory, NihilityTransportCoreBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity);
    }

    private NihilityTransportCoreMenu(int id, Inventory inventory, Container filters,
                                      NihilityTransportCoreBlockEntity blockEntity) {
        super(ModMenus.NIHILITY_TRANSPORT_CORE.get(), id);
        this.filters = filters;
        this.blockEntity = blockEntity;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new FilterSlot(filters, column + row * 3,
                    NihilityTransportLayout.FILTER_X + column * NihilityTransportLayout.SLOT_STEP,
                    NihilityTransportLayout.FILTER_Y + row * NihilityTransportLayout.SLOT_STEP));
            }
        }
        addStandardInventorySlots(inventory,
            NihilityTransportLayout.PLAYER_INV_X, NihilityTransportLayout.PLAYER_INV_Y);

        for (int index = 0; index < data.length; index++) {
            int dataIndex = index;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return NihilityTransportCoreMenu.this.blockEntity == null
                        ? data[dataIndex]
                        : NihilityTransportCoreMenu.this.blockEntity.getMenuData(dataIndex);
                }

                @Override
                public void set(int value) {
                    data[dataIndex] = value;
                }
            });
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (blockEntity == null || !blockEntity.stillValid(player)) {
            return false;
        }
        switch (buttonId) {
            case BUTTON_INPUT_DIRECTION -> blockEntity.cycleInputDirection();
            case BUTTON_OUTPUT_DIRECTION -> blockEntity.cycleOutputDirection();
            case BUTTON_RESOURCE_MODE -> blockEntity.cycleResourceMode();
            case BUTTON_PRIORITY -> blockEntity.cyclePriority();
            case BUTTON_REDSTONE_MODE -> blockEntity.cycleRedstoneMode();
            case BUTTON_FILTER_MODE -> blockEntity.cycleFilterMode();
            case BUTTON_CLEAR_FILTERS -> blockEntity.clearFilters();
            default -> {
                return super.clickMenuButton(player, buttonId);
            }
        }
        broadcastFullState();
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < FILTER_SLOT_COUNT) {
            if (input == ContainerInput.PICKUP && (button == 0 || button == 1)) {
                ItemStack carried = getCarried();
                filters.setItem(slotId,
                    button == 0 && !carried.isEmpty() ? carried.copyWithCount(1) : ItemStack.EMPTY);
                broadcastFullState();
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || blockEntity.stillValid(player);
    }

    public Direction getInputDirection() {
        return NihilityTransportCoreBlockEntity.directionById(data[0]);
    }

    public Direction getOutputDirection() {
        return NihilityTransportCoreBlockEntity.directionById(data[1]);
    }

    public ResourceMode getResourceMode() {
        return ResourceMode.byId(data[2]);
    }

    public Priority getPriority() {
        return Priority.byId(data[3]);
    }

    public RedstoneMode getRedstoneMode() {
        return RedstoneMode.byId(data[4]);
    }

    public FilterMode getFilterMode() {
        return FilterMode.byId(data[5]);
    }

    public boolean isLegacyRouting() {
        return data[6] != 0;
    }

    public int getLastItemMoved() {
        return data[7];
    }

    public int getLastFluidMoved() {
        return data[8];
    }

    public int getLastEnergyMoved() {
        return data[9];
    }

    public BlockedReason getBlockedReason() {
        return BlockedReason.byId(data[10]);
    }

    public boolean hasFilterEntries() {
        return data[11] != 0;
    }

    private static final class FilterSlot extends Slot {
        private FilterSlot(Container container, int slot, int x, int y) {
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
