package com.templenihility.menu;

import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.init.ModMenus;
import com.templenihility.storage.NihilityVaultNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

public class NihilityTerminalMenu extends AbstractContainerMenu {
    public static final int STORAGE_COLUMNS = 9;
    public static final int STORAGE_ROWS = 5;
    public static final int VISIBLE_SLOTS = STORAGE_COLUMNS * STORAGE_ROWS;
    public static final int CRAFT_RESULT_SLOT = VISIBLE_SLOTS;
    public static final int CRAFT_INPUT_START = CRAFT_RESULT_SLOT + 1;
    public static final int CRAFT_INPUT_COUNT = 9;
    public static final int PLAYER_INV_START = CRAFT_INPUT_START + CRAFT_INPUT_COUNT;
    public static final int BUTTON_PREV_PAGE = 0;
    public static final int BUTTON_NEXT_PAGE = 1;
    public static final int BUTTON_CYCLE_SORT = 2;
    public static final int BUTTON_CLEAR_SEARCH = 3;
    public static final int BUTTON_TOGGLE_CHUNKLOAD = 4;
    public static final int BUTTON_TOGGLE_BREAK_PROTECTION = 5;
    public static final int BUTTON_CHAR_BASE = 1000;

    private final List<NihilityVaultBlockEntity> vaults;
    private final TerminalViewContainer terminalContainer;
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player player;
    private final int[] data = new int[11];
    private final List<SlotRef> allRefs = new ArrayList<>();
    private final List<StackGroup> viewGroups = new ArrayList<>();
    private String search = "";
    private boolean rebuildingView;

    public NihilityTerminalMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, List.of());
    }

    public NihilityTerminalMenu(int id, Inventory inventory, List<NihilityVaultBlockEntity> vaults) {
        super(ModMenus.NIHILITY_TERMINAL.get(), id);
        this.vaults = vaults;
        this.player = inventory.player;
        this.terminalContainer = new TerminalViewContainer(this);
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);

        rebuildAllRefs();
        rebuildView();

        for (int row = 0; row < STORAGE_ROWS; row++) {
            for (int col = 0; col < STORAGE_COLUMNS; col++) {
                addSlot(new AggregateSlot(terminalContainer, col + row * STORAGE_COLUMNS,
                    8 + col * 18, 55 + row * 18));
            }
        }

        addSlot(new ResultSlot(this.player, craftSlots, resultSlots, 0, 222, 79));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3, 181 + col * 18, 61 + row * 18));
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
        if (buttonId == BUTTON_TOGGLE_CHUNKLOAD) {
            toggleChunkLoading(player);
            return true;
        }
        if (buttonId == BUTTON_TOGGLE_BREAK_PROTECTION) {
            toggleBreakProtection(player);
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
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == craftSlots && player.level() instanceof ServerLevel level) {
            updateCraftingResult(this, level, player, craftSlots, resultSlots, null);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, craftSlots);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (isStorageSlot(index)) {
            int amount = Math.min(source.getCount(), source.getMaxStackSize());
            ItemStack moving = source.copyWithCount(amount);
            if (!moveItemStackTo(moving, PLAYER_INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            int moved = amount - moving.getCount();
            if (moved <= 0) {
                return ItemStack.EMPTY;
            }
            terminalContainer.removeItem(index, moved);
            rebuildView();
            broadcastFullState();
            return source.copyWithCount(moved);
        }

        if (index == CRAFT_RESULT_SLOT) {
            source.getItem().onCraftedBy(source, player);
            if (!moveItemStackTo(source, PLAYER_INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(source, original);
        } else if (index >= CRAFT_INPUT_START && index < PLAYER_INV_START) {
            if (!moveItemStackTo(source, PLAYER_INV_START, slots.size(), false)) {
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

        if (source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, source);
        if (index == CRAFT_RESULT_SLOT) {
            player.drop(source, false);
        }
        rebuildView();
        broadcastFullState();
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
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

    public boolean isNetworkChunkLoaded() {
        return getVaultCount() > 0 && getChunkLoadedVaults() >= getVaultCount();
    }

    public int getFilteredSlotCount() {
        return data[9];
    }

    public int getBreakProtectedVaults() {
        return data[10];
    }

    public boolean isNetworkBreakProtected() {
        return getVaultCount() > 0 && getBreakProtectedVaults() >= getVaultCount();
    }

    public boolean isStorageSlot(int index) {
        return index >= 0 && index < VISIBLE_SLOTS;
    }

    public int getVisibleItemCount(int slot) {
        StackGroup group = visibleGroup(slot);
        if (group != null) {
            return group.totalCount();
        }
        if (slot >= 0 && slot < VISIBLE_SLOTS) {
            return terminalContainer.clientStacks.get(slot).getCount();
        }
        return 0;
    }

    public String getVisibleCountText(int slot) {
        int count = getVisibleItemCount(slot);
        if (count <= 1) {
            return null;
        }
        if (count < 1_000) {
            return Integer.toString(count);
        }
        if (count < 1_000_000) {
            return (count / 1_000) + "K";
        }
        return (count / 1_000_000) + "M";
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

    private void toggleChunkLoading(Player player) {
        if (vaults.isEmpty() || !(vaults.getFirst().getLevel() instanceof ServerLevel level)) {
            return;
        }

        boolean enabled = !isNetworkChunkLoaded();
        NihilityVaultNetwork.setNetworkChunkLoaded(level, vaults.getFirst().getBlockPos(), enabled);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
            enabled ? "message.templenihility.vault_chunkload_on" : "message.templenihility.vault_chunkload_off",
            vaults.size()));
        rebuildView();
        broadcastFullState();
    }

    private void toggleBreakProtection(Player player) {
        if (vaults.isEmpty() || !(vaults.getFirst().getLevel() instanceof ServerLevel level)) {
            return;
        }

        boolean enabled = !isNetworkBreakProtected();
        NihilityVaultNetwork.setNetworkBreakProtected(level, vaults.getFirst().getBlockPos(), enabled);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
            enabled ? "message.templenihility.vault_protection_on" : "message.templenihility.vault_protection_off",
            vaults.size()));
        rebuildView();
        broadcastFullState();
    }

    private void rebuildAllRefs() {
        allRefs.clear();
        int slotOffset = 0;
        for (int vaultIndex = 0; vaultIndex < vaults.size(); vaultIndex++) {
            NihilityVaultBlockEntity vault = vaults.get(vaultIndex);
            NonNullList<ItemStack> items = vault.getItems();
            for (int slot = 0; slot < items.size(); slot++) {
                allRefs.add(new SlotRef(vault, items, slot, slotOffset + slot));
            }
            slotOffset += items.size();
        }
        NihilityVaultNetwork.Stats stats = NihilityVaultNetwork.stats(vaults);
        data[3] = stats.vaultCount();
        data[4] = stats.totalSlots();
        data[5] = stats.usedSlots();
        data[6] = stats.emptySlots();
        data[7] = stats.itemCount();
        data[8] = stats.chunkLoadedVaults();
        data[10] = stats.breakProtectedVaults();
    }

    private void rebuildView() {
        rebuildingView = true;
        try {
            rebuildAllRefs();
            viewGroups.clear();
            List<StackGroup> groups = buildGroups();
            String needle = search.toLowerCase(Locale.ROOT);
            for (StackGroup group : groups) {
                if (!needle.isEmpty() && !matches(group.display(), needle)) {
                    continue;
                }
                viewGroups.add(group);
            }

            Comparator<StackGroup> comparator = switch (data[2]) {
                case 1 -> Comparator.comparing(group -> sortName(group.display()));
                case 2 -> Comparator.<StackGroup>comparingInt(StackGroup::totalCount).reversed();
                case 3 -> Comparator.comparing(group -> sortMod(group.display()));
                default -> Comparator.comparingInt(StackGroup::firstGlobalIndex);
            };
            viewGroups.sort(comparator.thenComparingInt(StackGroup::firstGlobalIndex));

            data[1] = Math.max(0, (viewGroups.size() - 1) / VISIBLE_SLOTS);
            data[0] = Math.max(0, Math.min(data[0], data[1]));
            data[9] = viewGroups.size();
        } finally {
            rebuildingView = false;
        }
    }

    private List<StackGroup> buildGroups() {
        Map<Integer, List<StackGroup>> byHash = new HashMap<>();
        List<StackGroup> groups = new ArrayList<>();
        for (SlotRef ref : allRefs) {
            ItemStack stack = ref.get();
            if (stack.isEmpty()) {
                continue;
            }
            int hash = ItemStack.hashItemAndComponents(stack);
            StackGroup group = findMatchingGroup(byHash.get(hash), stack);
            if (group == null) {
                group = new StackGroup(stack, ref.globalIndex());
                groups.add(group);
                byHash.computeIfAbsent(hash, ignored -> new ArrayList<>()).add(group);
            }
            group.add(ref);
        }
        return groups;
    }

    private @Nullable StackGroup findMatchingGroup(@Nullable List<StackGroup> candidates, ItemStack stack) {
        if (candidates == null) {
            return null;
        }
        for (StackGroup group : candidates) {
            if (ItemStack.isSameItemSameComponents(group.display(), stack)) {
                return group;
            }
        }
        return null;
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

    private StackGroup visibleGroup(int slot) {
        int index = data[0] * VISIBLE_SLOTS + slot;
        return index >= 0 && index < viewGroups.size() ? viewGroups.get(index) : null;
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

    private static void updateCraftingResult(AbstractContainerMenu menu, ServerLevel level, Player player,
                                             CraftingContainer container, ResultContainer resultSlots,
                                             @Nullable RecipeHolder<CraftingRecipe> recipeHint) {
        CraftingInput input = container.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> maybeRecipe =
            level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level, recipeHint);
        if (maybeRecipe.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeHolder = maybeRecipe.get();
            CraftingRecipe craftingRecipe = recipeHolder.value();
            if (resultSlots.setRecipeUsed(serverPlayer, recipeHolder)) {
                ItemStack recipeResult = craftingRecipe.assemble(input);
                if (recipeResult.isItemEnabled(level.enabledFeatures())) {
                    result = recipeResult;
                }
            }
        }

        resultSlots.setItem(0, result);
        menu.setRemoteSlot(CRAFT_RESULT_SLOT, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
            menu.containerId, menu.incrementStateId(), CRAFT_RESULT_SLOT, result));
    }

    private static final class AggregateSlot extends Slot {
        private AggregateSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean allowModification(Player player) {
            return mayPickup(player);
        }
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
            StackGroup group = menu.visibleGroup(slot);
            if (group != null) {
                return group.displayStack();
            }
            return clientStacks.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            StackGroup group = menu.visibleGroup(slot);
            if (group == null) {
                return ContainerHelper.removeItem(clientStacks, slot, amount);
            }
            ItemStack removed = group.remove(amount);
            if (!removed.isEmpty()) {
                setChanged();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            StackGroup group = menu.visibleGroup(slot);
            if (group == null) {
                return ContainerHelper.takeItem(clientStacks, slot);
            }
            return group.remove(group.display().getMaxStackSize());
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            StackGroup group = menu.visibleGroup(slot);
            if (group == null) {
                clientStacks.set(slot, stack);
                return;
            }
            if (!stack.isEmpty()) {
                ItemStack inserting = stack.copy();
                menu.insertIntoNetwork(inserting);
            }
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
                removeItemNoUpdate(i);
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

    private static final class StackGroup {
        private final ItemStack display;
        private final List<SlotRef> refs = new ArrayList<>();
        private final int firstGlobalIndex;
        private int totalCount;

        private StackGroup(ItemStack stack, int firstGlobalIndex) {
            this.display = stack.copyWithCount(1);
            this.firstGlobalIndex = firstGlobalIndex;
        }

        private void add(SlotRef ref) {
            refs.add(ref);
            totalCount = Math.min(Integer.MAX_VALUE, totalCount + ref.get().getCount());
        }

        private ItemStack display() {
            return display;
        }

        private int totalCount() {
            return totalCount;
        }

        private int firstGlobalIndex() {
            return firstGlobalIndex;
        }

        private ItemStack displayStack() {
            return display.copyWithCount(Math.max(1, totalCount));
        }

        private ItemStack remove(int amount) {
            if (amount <= 0 || totalCount <= 0) {
                return ItemStack.EMPTY;
            }
            int remaining = Math.min(amount, totalCount);
            int removed = 0;
            for (SlotRef ref : refs) {
                ItemStack stack = ref.get();
                if (stack.isEmpty()) {
                    continue;
                }
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                removed += taken;
                remaining -= taken;
                if (stack.isEmpty()) {
                    ref.set(ItemStack.EMPTY);
                }
                ref.vault.setChanged();
                if (remaining <= 0) {
                    break;
                }
            }
            totalCount -= removed;
            return removed <= 0 ? ItemStack.EMPTY : display.copyWithCount(removed);
        }
    }
}
