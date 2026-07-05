package com.templenihility.menu;

import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.init.ModMenus;
import com.templenihility.storage.NihilityVaultNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NihilityTerminalMenu extends AbstractContainerMenu {
    public static final int VISIBLE_SLOTS = 54;
    public static final int BUTTON_PREV_PAGE = 0;
    public static final int BUTTON_NEXT_PAGE = 1;
    public static final int BUTTON_CYCLE_SORT = 2;
    public static final int BUTTON_CLEAR_SEARCH = 3;
    public static final int BUTTON_CHAR_BASE = 1000;

    private final List<NihilityVaultBlockEntity> vaults;
    private final TerminalViewContainer terminalContainer;
    private final int[] data = new int[10];
    private final List<SlotRef> allRefs = new ArrayList<>();
    private final List<SlotRef> viewRefs = new ArrayList<>();
    private String search = "";
    private boolean rebuildingView;

    public NihilityTerminalMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, List.of());
    }

    public NihilityTerminalMenu(int id, Inventory inventory, List<NihilityVaultBlockEntity> vaults) {
        super(ModMenus.NIHILITY_TERMINAL.get(), id);
        this.vaults = vaults;
        this.terminalContainer = new TerminalViewContainer(this);

        rebuildAllRefs();
        rebuildView();

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(terminalContainer, col + row * 9, 8 + col * 18, 45 + row * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 166);

        for (int i = 0; i < data.length; i++) {
            addDataSlot(DataSlot.shared(data, i));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_PREV_PAGE) {
            setPage(getPage() - 1);
            return true;
        }
        if (buttonId == BUTTON_NEXT_PAGE) {
            setPage(getPage() + 1);
            return true;
        }
        if (buttonId == BUTTON_CYCLE_SORT) {
            data[2] = (data[2] + 1) % 4;
            setPage(0);
            return true;
        }
        if (buttonId == BUTTON_CLEAR_SEARCH) {
            search = "";
            setPage(0);
            return true;
        }
        if (buttonId >= BUTTON_CHAR_BASE) {
            int codePoint = buttonId - BUTTON_CHAR_BASE;
            if (codePoint > 0 && search.length() < 64) {
                search += Character.toString((char) codePoint);
                setPage(0);
                return true;
            }
        }
        return super.clickMenuButton(player, buttonId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < VISIBLE_SLOTS) {
            if (!moveItemStackTo(source, VISIBLE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!insertIntoNetwork(source)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        rebuildView();
        broadcastFullState();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return vaults.isEmpty() || vaults.stream().anyMatch(vault -> !vault.isRemoved());
    }

    public int getPage() {
        return data[0];
    }

    public int getMaxPage() {
        return data[1];
    }

    public int getSortMode() {
        return data[2];
    }

    public int getVaultCount() {
        return data[3];
    }

    public int getTotalSlots() {
        return data[4];
    }

    public int getUsedSlots() {
        return data[5];
    }

    public int getEmptySlots() {
        return data[6];
    }

    public int getStoredItemCount() {
        return data[7];
    }

    public int getChunkLoadedVaults() {
        return data[8];
    }

    public int getFilteredSlotCount() {
        return data[9];
    }

    public String getSortLabelKey() {
        return switch (getSortMode()) {
            case 1 -> "screen.templenihility.sort_name";
            case 2 -> "screen.templenihility.sort_count";
            case 3 -> "screen.templenihility.sort_mod";
            default -> "screen.templenihility.sort_position";
        };
    }

    private void setPage(int page) {
        data[0] = Math.max(0, Math.min(page, data[1]));
        rebuildView();
        broadcastFullState();
    }

    private void rebuildAllRefs() {
        allRefs.clear();
        for (int vaultIndex = 0; vaultIndex < vaults.size(); vaultIndex++) {
            NihilityVaultBlockEntity vault = vaults.get(vaultIndex);
            NonNullList<ItemStack> items = vault.getItems();
            for (int slot = 0; slot < items.size(); slot++) {
                allRefs.add(new SlotRef(vault, items, slot, vaultIndex * NihilityVaultBlockEntity.SLOTS_PER_VAULT + slot));
            }
        }
        NihilityVaultNetwork.Stats stats = NihilityVaultNetwork.stats(vaults);
        data[3] = stats.vaultCount();
        data[4] = stats.totalSlots();
        data[5] = stats.usedSlots();
        data[6] = stats.emptySlots();
        data[7] = stats.itemCount();
        data[8] = stats.chunkLoadedVaults();
    }

    private void rebuildView() {
        rebuildingView = true;
        try {
            rebuildAllRefs();
            viewRefs.clear();
            String needle = search.toLowerCase(Locale.ROOT);
            for (SlotRef ref : allRefs) {
                ItemStack stack = ref.get();
                if (!needle.isEmpty() && (stack.isEmpty() || !matches(stack, needle))) {
                    continue;
                }
                viewRefs.add(ref);
            }

            Comparator<SlotRef> comparator = switch (data[2]) {
                case 1 -> Comparator.comparing(ref -> sortName(ref.get()));
                case 2 -> Comparator.<SlotRef>comparingInt(ref -> ref.get().isEmpty() ? 0 : ref.get().getCount()).reversed();
                case 3 -> Comparator.comparing(ref -> sortMod(ref.get()));
                default -> Comparator.comparingInt(SlotRef::globalIndex);
            };
            viewRefs.sort(comparator.thenComparingInt(SlotRef::globalIndex));

            data[1] = Math.max(0, (viewRefs.size() - 1) / VISIBLE_SLOTS);
            data[0] = Math.max(0, Math.min(data[0], data[1]));
            data[9] = viewRefs.size();
        } finally {
            rebuildingView = false;
        }
    }

    private void markContentChanged() {
        if (rebuildingView || vaults.isEmpty()) {
            return;
        }
        rebuildView();
        broadcastFullState();
    }

    private boolean matches(ItemStack stack, String needle) {
        return sortName(stack).contains(needle) || sortMod(stack).contains(needle);
    }

    private String sortName(ItemStack stack) {
        return stack.isEmpty() ? "\uffff" : stack.getHoverName().getString().toLowerCase(Locale.ROOT);
    }

    private String sortMod(ItemStack stack) {
        if (stack.isEmpty()) {
            return "\uffff";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().toLowerCase(Locale.ROOT);
    }

    private SlotRef visibleRef(int slot) {
        int index = data[0] * VISIBLE_SLOTS + slot;
        return index >= 0 && index < viewRefs.size() ? viewRefs.get(index) : null;
    }

    private boolean insertIntoNetwork(ItemStack source) {
        if (source.isEmpty()) {
            return false;
        }

        int before = source.getCount();
        for (SlotRef ref : allRefs) {
            ItemStack existing = ref.get();
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, source)) {
                int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), source.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    source.shrink(moved);
                    ref.vault.setChanged();
                }
            }
            if (source.isEmpty()) {
                return true;
            }
        }

        for (SlotRef ref : allRefs) {
            if (ref.get().isEmpty()) {
                int moved = Math.min(source.getMaxStackSize(), source.getCount());
                ref.set(source.copyWithCount(moved));
                source.shrink(moved);
                ref.vault.setChanged();
            }
            if (source.isEmpty()) {
                return true;
            }
        }
        return source.getCount() != before;
    }

    private static final class TerminalViewContainer implements Container {
        private final NihilityTerminalMenu menu;
        private final NonNullList<ItemStack> clientStacks = NonNullList.withSize(VISIBLE_SLOTS, ItemStack.EMPTY);

        private TerminalViewContainer(NihilityTerminalMenu menu) {
            this.menu = menu;
        }

        @Override
        public int getContainerSize() {
            return VISIBLE_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < VISIBLE_SLOTS; i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            SlotRef ref = menu.visibleRef(slot);
            return ref == null ? clientStacks.get(slot) : ref.get();
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            SlotRef ref = menu.visibleRef(slot);
            if (ref == null) {
                return ContainerHelper.removeItem(clientStacks, slot, amount);
            }
            ItemStack removed = ContainerHelper.removeItem(ref.items, ref.slot, amount);
            if (!removed.isEmpty()) {
                ref.vault.setChanged();
                setChanged();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            SlotRef ref = menu.visibleRef(slot);
            if (ref == null) {
                return ContainerHelper.takeItem(clientStacks, slot);
            }
            ItemStack removed = ref.get();
            ref.set(ItemStack.EMPTY);
            ref.vault.setChanged();
            setChanged();
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            SlotRef ref = menu.visibleRef(slot);
            if (ref == null) {
                clientStacks.set(slot, stack);
                return;
            }
            ref.set(stack);
            ref.vault.setChanged();
            setChanged();
        }

        @Override
        public void setChanged() {
            menu.markContentChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return menu.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < VISIBLE_SLOTS; i++) {
                setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private record SlotRef(NihilityVaultBlockEntity vault, NonNullList<ItemStack> items, int slot, int globalIndex) {
        ItemStack get() {
            return items.get(slot);
        }

        void set(ItemStack stack) {
            items.set(slot, stack);
        }
    }
}
