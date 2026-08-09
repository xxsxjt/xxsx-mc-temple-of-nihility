package com.templenihility.blockentity;

import com.templenihility.config.ModConfig;
import com.templenihility.init.ModBlockEntities;
import com.templenihility.transport.TransportPolicy;
import com.templenihility.transport.TransportPolicy.BlockedReason;
import com.templenihility.transport.TransportPolicy.FilterMode;
import com.templenihility.transport.TransportPolicy.Priority;
import com.templenihility.transport.TransportPolicy.RedstoneMode;
import com.templenihility.transport.TransportPolicy.ResourceMode;
import com.templenihility.transport.TransportPolicy.ResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class NihilityTransportCoreBlockEntity extends BlockEntity implements Container {
    public static final int FILTER_SLOTS = 9;
    public static final int MENU_DATA_COUNT = 12;

    private static final int CONFIG_VERSION = 1;
    private static final Direction[][] LEGACY_PAIRS = {
        { Direction.NORTH, Direction.SOUTH },
        { Direction.WEST, Direction.EAST },
        { Direction.DOWN, Direction.UP }
    };

    private NonNullList<ItemStack> filterItems = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private Direction inputDirection = Direction.NORTH;
    private Direction outputDirection = Direction.SOUTH;
    private ResourceMode resourceMode = ResourceMode.ALL;
    private Priority priority = Priority.BALANCED;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private FilterMode filterMode = FilterMode.OFF;
    private BlockedReason lastBlockedReason = BlockedReason.IDLE;
    private boolean legacyRouting;
    private int rotation;
    private int lastItemMoved;
    private int lastFluidMoved;
    private int lastEnergyMoved;

    public NihilityTransportCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIHILITY_TRANSPORT_CORE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NihilityTransportCoreBlockEntity core) {
        int interval = ModConfig.TRANSPORT_INTERVAL_TICKS.get();
        if (level.isClientSide() || level.getGameTime() % interval != 0) {
            return;
        }

        if (core.legacyRouting) {
            core.runLegacyTransfer(level, pos);
        } else {
            core.runDirectedTransfer(level, pos);
        }
    }

    private void runDirectedTransfer(Level level, BlockPos pos) {
        if (!TransportPolicy.allowsRedstone(redstoneMode, level.hasNeighborSignal(pos))) {
            recordResult(0, 0, 0, BlockedReason.REDSTONE_CONTROL);
            return;
        }
        if (inputDirection == outputDirection) {
            recordResult(0, 0, 0, BlockedReason.INVALID_DIRECTION);
            return;
        }

        BlockPos sourcePos = pos.relative(inputDirection);
        BlockPos targetPos = pos.relative(outputDirection);
        if (!level.isLoaded(sourcePos)) {
            recordResult(0, 0, 0, BlockedReason.SOURCE_UNLOADED);
            return;
        }
        if (!level.isLoaded(targetPos)) {
            recordResult(0, 0, 0, BlockedReason.TARGET_UNLOADED);
            return;
        }

        List<BlockedReason> failures = new ArrayList<>();
        List<ResourceType> order = TransportPolicy.orderedResources(resourceMode, priority, rotation);
        if (resourceMode == ResourceMode.ALL && priority == Priority.BALANCED) {
            rotation = (rotation + 1) % 3;
        }

        for (ResourceType type : order) {
            TransferAttempt attempt = switch (type) {
                case ITEMS -> moveItems(level, sourcePos, targetPos);
                case FLUIDS -> moveFluids(level, sourcePos, targetPos);
                case ENERGY -> moveEnergy(level, sourcePos, targetPos);
            };
            if (attempt.moved() > 0) {
                recordResult(
                    type == ResourceType.ITEMS ? attempt.moved() : 0,
                    type == ResourceType.FLUIDS ? attempt.moved() : 0,
                    type == ResourceType.ENERGY ? attempt.moved() : 0,
                    BlockedReason.RUNNING);
                return;
            }
            failures.add(attempt.reason());
        }

        recordResult(0, 0, 0, TransportPolicy.chooseDiagnostic(failures));
    }

    private TransferAttempt moveItems(Level level, BlockPos sourcePos, BlockPos targetPos) {
        ResourceHandler<ItemResource> source = level.getCapability(
            Capabilities.Item.BLOCK, sourcePos, inputDirection.getOpposite());
        if (source == null) {
            return TransferAttempt.failed(BlockedReason.SOURCE_CAPABILITY_MISSING);
        }
        ResourceHandler<ItemResource> target = level.getCapability(
            Capabilities.Item.BLOCK, targetPos, outputDirection.getOpposite());
        if (target == null) {
            return TransferAttempt.failed(BlockedReason.TARGET_CAPABILITY_MISSING);
        }
        if (source == target) {
            return TransferAttempt.failed(BlockedReason.SAME_HANDLER);
        }
        if (filterMode == FilterMode.WHITELIST && !hasFilterEntries()) {
            return TransferAttempt.failed(BlockedReason.FILTER_EMPTY);
        }

        boolean hasItems = false;
        boolean hasAllowedItems = false;
        for (int index = 0; index < source.size(); index++) {
            ItemResource resource = source.getResource(index);
            if (source.getAmountAsLong(index) <= 0 || resource.isEmpty()) {
                continue;
            }
            hasItems = true;
            if (allowsItem(resource)) {
                hasAllowedItems = true;
            }
        }
        if (!hasItems) {
            return TransferAttempt.failed(BlockedReason.SOURCE_EMPTY);
        }
        if (!hasAllowedItems) {
            return TransferAttempt.failed(BlockedReason.FILTER_REJECTED);
        }

        Predicate<ItemResource> predicate = this::allowsItem;
        if (ResourceHandlerUtil.findExtractableResource(source, predicate, null) == null) {
            return TransferAttempt.failed(BlockedReason.SOURCE_BLOCKED);
        }
        int moved = ResourceHandlerUtil.moveStacking(
            source, target, predicate, ModConfig.TRANSPORT_ITEMS_PER_OPERATION.get(), null);
        return moved > 0 ? TransferAttempt.moved(moved) : TransferAttempt.failed(BlockedReason.TARGET_BLOCKED);
    }

    private TransferAttempt moveFluids(Level level, BlockPos sourcePos, BlockPos targetPos) {
        ResourceHandler<FluidResource> source = level.getCapability(
            Capabilities.Fluid.BLOCK, sourcePos, inputDirection.getOpposite());
        if (source == null) {
            return TransferAttempt.failed(BlockedReason.SOURCE_CAPABILITY_MISSING);
        }
        ResourceHandler<FluidResource> target = level.getCapability(
            Capabilities.Fluid.BLOCK, targetPos, outputDirection.getOpposite());
        if (target == null) {
            return TransferAttempt.failed(BlockedReason.TARGET_CAPABILITY_MISSING);
        }
        if (source == target) {
            return TransferAttempt.failed(BlockedReason.SAME_HANDLER);
        }
        if (ResourceHandlerUtil.isEmpty(source)) {
            return TransferAttempt.failed(BlockedReason.SOURCE_EMPTY);
        }
        if (ResourceHandlerUtil.findExtractableResource(source, resource -> !resource.isEmpty(), null) == null) {
            return TransferAttempt.failed(BlockedReason.SOURCE_BLOCKED);
        }

        int moved = ResourceHandlerUtil.move(
            source, target, resource -> !resource.isEmpty(), ModConfig.TRANSPORT_FLUID_PER_OPERATION.get(), null);
        return moved > 0 ? TransferAttempt.moved(moved) : TransferAttempt.failed(BlockedReason.TARGET_BLOCKED);
    }

    private TransferAttempt moveEnergy(Level level, BlockPos sourcePos, BlockPos targetPos) {
        EnergyHandler source = level.getCapability(
            Capabilities.Energy.BLOCK, sourcePos, inputDirection.getOpposite());
        if (source == null) {
            return TransferAttempt.failed(BlockedReason.SOURCE_CAPABILITY_MISSING);
        }
        EnergyHandler target = level.getCapability(
            Capabilities.Energy.BLOCK, targetPos, outputDirection.getOpposite());
        if (target == null) {
            return TransferAttempt.failed(BlockedReason.TARGET_CAPABILITY_MISSING);
        }
        if (source == target) {
            return TransferAttempt.failed(BlockedReason.SAME_HANDLER);
        }
        if (source.getAmountAsLong() <= 0) {
            return TransferAttempt.failed(BlockedReason.SOURCE_EMPTY);
        }
        int amount = ModConfig.TRANSPORT_ENERGY_PER_OPERATION.get();
        try (Transaction simulation = Transaction.open(null)) {
            if (source.extract(amount, simulation) <= 0) {
                return TransferAttempt.failed(BlockedReason.SOURCE_BLOCKED);
            }
        }
        if (target.getAmountAsLong() >= target.getCapacityAsLong()) {
            return TransferAttempt.failed(BlockedReason.TARGET_BLOCKED);
        }

        int moved = EnergyHandlerUtil.move(source, target, amount, null);
        return moved > 0 ? TransferAttempt.moved(moved) : TransferAttempt.failed(BlockedReason.TARGET_BLOCKED);
    }

    private void runLegacyTransfer(Level level, BlockPos pos) {
        int items = 0;
        int fluids = 0;
        int energy = 0;
        for (Direction[] pair : LEGACY_PAIRS) {
            items += moveLegacyItems(level, pos, pair[0], pair[1]);
            fluids += moveLegacyFluids(level, pos, pair[0], pair[1]);
            energy += moveLegacyEnergy(level, pos, pair[0], pair[1]);
        }
        recordResult(items, fluids, energy,
            items + fluids + energy > 0 ? BlockedReason.RUNNING : BlockedReason.IDLE);
    }

    private static int moveLegacyItems(Level level, BlockPos pos, Direction first, Direction second) {
        ResourceHandler<ItemResource> a = level.getCapability(
            Capabilities.Item.BLOCK, pos.relative(first), first.getOpposite());
        ResourceHandler<ItemResource> b = level.getCapability(
            Capabilities.Item.BLOCK, pos.relative(second), second.getOpposite());
        if (a == null || b == null || a == b) {
            return 0;
        }
        int amount = ModConfig.TRANSPORT_ITEMS_PER_OPERATION.get();
        int moved = ResourceHandlerUtil.moveStacking(a, b, resource -> !resource.isEmpty(), amount, null);
        return moved > 0 ? moved : ResourceHandlerUtil.moveStacking(b, a, resource -> !resource.isEmpty(), amount, null);
    }

    private static int moveLegacyFluids(Level level, BlockPos pos, Direction first, Direction second) {
        ResourceHandler<FluidResource> a = level.getCapability(
            Capabilities.Fluid.BLOCK, pos.relative(first), first.getOpposite());
        ResourceHandler<FluidResource> b = level.getCapability(
            Capabilities.Fluid.BLOCK, pos.relative(second), second.getOpposite());
        if (a == null || b == null || a == b) {
            return 0;
        }
        int amount = ModConfig.TRANSPORT_FLUID_PER_OPERATION.get();
        int moved = ResourceHandlerUtil.move(a, b, resource -> !resource.isEmpty(), amount, null);
        return moved > 0 ? moved : ResourceHandlerUtil.move(b, a, resource -> !resource.isEmpty(), amount, null);
    }

    private static int moveLegacyEnergy(Level level, BlockPos pos, Direction first, Direction second) {
        EnergyHandler a = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(first), first.getOpposite());
        EnergyHandler b = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(second), second.getOpposite());
        if (a == null || b == null || a == b) {
            return 0;
        }
        int amount = ModConfig.TRANSPORT_ENERGY_PER_OPERATION.get();
        int moved = EnergyHandlerUtil.move(a, b, amount, null);
        return moved > 0 ? moved : EnergyHandlerUtil.move(b, a, amount, null);
    }

    private boolean allowsItem(ItemResource resource) {
        boolean hasEntries = false;
        boolean matches = false;
        for (ItemStack filter : filterItems) {
            if (filter.isEmpty()) {
                continue;
            }
            hasEntries = true;
            if (resource.matches(filter)) {
                matches = true;
                break;
            }
        }
        return TransportPolicy.allowsItem(filterMode, hasEntries, matches);
    }

    private boolean hasFilterEntries() {
        return filterItems.stream().anyMatch(stack -> !stack.isEmpty());
    }

    private void recordResult(int items, int fluids, int energy, BlockedReason reason) {
        lastItemMoved = items;
        lastFluidMoved = fluids;
        lastEnergyMoved = energy;
        lastBlockedReason = reason;
    }

    public void cycleInputDirection() {
        leaveLegacyRouting();
        inputDirection = nextDirection(inputDirection, outputDirection);
        configurationChanged();
    }

    public void cycleOutputDirection() {
        leaveLegacyRouting();
        outputDirection = nextDirection(outputDirection, inputDirection);
        configurationChanged();
    }

    public void cycleResourceMode() {
        leaveLegacyRouting();
        resourceMode = ResourceMode.byId(resourceMode.ordinal() + 1);
        configurationChanged();
    }

    public void cyclePriority() {
        leaveLegacyRouting();
        priority = Priority.byId(priority.ordinal() + 1);
        configurationChanged();
    }

    public void cycleRedstoneMode() {
        leaveLegacyRouting();
        redstoneMode = RedstoneMode.byId(redstoneMode.ordinal() + 1);
        configurationChanged();
    }

    public void cycleFilterMode() {
        leaveLegacyRouting();
        filterMode = FilterMode.byId(filterMode.ordinal() + 1);
        configurationChanged();
    }

    public void clearFilters() {
        leaveLegacyRouting();
        filterItems = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
        configurationChanged();
    }

    private void leaveLegacyRouting() {
        legacyRouting = false;
    }

    private void configurationChanged() {
        lastBlockedReason = BlockedReason.IDLE;
        setChanged();
    }

    private static Direction nextDirection(Direction current, Direction forbidden) {
        Direction[] values = Direction.values();
        return values[TransportPolicy.nextDistinctDirectionId(
            current.ordinal(), forbidden.ordinal(), values.length)];
    }

    public int getMenuData(int index) {
        return switch (index) {
            case 0 -> inputDirection.ordinal();
            case 1 -> outputDirection.ordinal();
            case 2 -> resourceMode.ordinal();
            case 3 -> priority.ordinal();
            case 4 -> redstoneMode.ordinal();
            case 5 -> filterMode.ordinal();
            case 6 -> legacyRouting ? 1 : 0;
            case 7 -> lastItemMoved;
            case 8 -> lastFluidMoved;
            case 9 -> lastEnergyMoved;
            case 10 -> lastBlockedReason.ordinal();
            case 11 -> hasFilterEntries() ? 1 : 0;
            default -> 0;
        };
    }

    public static Direction directionById(int id) {
        Direction[] values = Direction.values();
        return values[Math.floorMod(id, values.length)];
    }

    @Override
    public int getContainerSize() {
        return filterItems.size();
    }

    @Override
    public boolean isEmpty() {
        return filterItems.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return filterItems.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(filterItems, slot, amount);
        if (!removed.isEmpty()) {
            leaveLegacyRouting();
            configurationChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(filterItems, slot);
        if (!removed.isEmpty()) {
            leaveLegacyRouting();
            configurationChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        filterItems.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        leaveLegacyRouting();
        configurationChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved()
            && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        clearFilters();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filterItems = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, filterItems);
        legacyRouting = input.getIntOr("TransportConfigVersion", 0) < CONFIG_VERSION;
        inputDirection = directionById(input.getIntOr("InputDirection", Direction.NORTH.ordinal()));
        outputDirection = directionById(input.getIntOr("OutputDirection", Direction.SOUTH.ordinal()));
        if (inputDirection == outputDirection) {
            outputDirection = inputDirection.getOpposite();
        }
        resourceMode = ResourceMode.byId(input.getIntOr("ResourceMode", ResourceMode.ALL.ordinal()));
        priority = Priority.byId(input.getIntOr("Priority", Priority.BALANCED.ordinal()));
        redstoneMode = RedstoneMode.byId(input.getIntOr("RedstoneMode", RedstoneMode.IGNORE.ordinal()));
        filterMode = FilterMode.byId(input.getIntOr("FilterMode", FilterMode.OFF.ordinal()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, filterItems);
        output.putInt("TransportConfigVersion", CONFIG_VERSION);
        output.putInt("InputDirection", inputDirection.ordinal());
        output.putInt("OutputDirection", outputDirection.ordinal());
        output.putInt("ResourceMode", resourceMode.ordinal());
        output.putInt("Priority", priority.ordinal());
        output.putInt("RedstoneMode", redstoneMode.ordinal());
        output.putInt("FilterMode", filterMode.ordinal());
    }

    private record TransferAttempt(int moved, BlockedReason reason) {
        private static TransferAttempt moved(int amount) {
            return new TransferAttempt(amount, BlockedReason.RUNNING);
        }

        private static TransferAttempt failed(BlockedReason reason) {
            return new TransferAttempt(0, reason);
        }
    }
}
