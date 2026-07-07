package com.templenihility.blockentity;

import com.templenihility.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class NihilityTransportCoreBlockEntity extends BlockEntity {
    private static final int ITEM_TRANSFER_PER_SECOND = 16;
    private static final int FLUID_TRANSFER_PER_SECOND = FluidType.BUCKET_VOLUME / 4;
    private static final int ENERGY_TRANSFER_PER_SECOND = 256;
    private static final Direction[][] PAIRS = {
        { Direction.NORTH, Direction.SOUTH },
        { Direction.WEST, Direction.EAST },
        { Direction.DOWN, Direction.UP }
    };

    private int lastItemMoved;
    private int lastFluidMoved;
    private int lastEnergyMoved;

    public NihilityTransportCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIHILITY_TRANSPORT_CORE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NihilityTransportCoreBlockEntity core) {
        if (level.isClientSide() || level.getGameTime() % 20 != 0) {
            return;
        }

        int items = 0;
        int fluids = 0;
        int energy = 0;
        for (Direction[] pair : PAIRS) {
            items += moveItems(level, pos, pair[0], pair[1], ITEM_TRANSFER_PER_SECOND);
            fluids += moveFluids(level, pos, pair[0], pair[1], FLUID_TRANSFER_PER_SECOND);
            energy += moveEnergy(level, pos, pair[0], pair[1], ENERGY_TRANSFER_PER_SECOND);
        }

        if (items != core.lastItemMoved || fluids != core.lastFluidMoved || energy != core.lastEnergyMoved) {
            core.lastItemMoved = items;
            core.lastFluidMoved = fluids;
            core.lastEnergyMoved = energy;
            core.setChanged();
        }
    }

    public int getLastItemMoved() {
        return lastItemMoved;
    }

    public int getLastFluidMoved() {
        return lastFluidMoved;
    }

    public int getLastEnergyMoved() {
        return lastEnergyMoved;
    }

    private static int moveItems(Level level, BlockPos pos, Direction first, Direction second, int amount) {
        ResourceHandler<ItemResource> a = level.getCapability(Capabilities.Item.BLOCK, pos.relative(first), first.getOpposite());
        ResourceHandler<ItemResource> b = level.getCapability(Capabilities.Item.BLOCK, pos.relative(second), second.getOpposite());
        if (a == null || b == null || a == b) {
            return 0;
        }

        int moved = ResourceHandlerUtil.moveStacking(a, b, resource -> !resource.isEmpty(), amount, null);
        return moved > 0 ? moved : ResourceHandlerUtil.moveStacking(b, a, resource -> !resource.isEmpty(), amount, null);
    }

    private static int moveFluids(Level level, BlockPos pos, Direction first, Direction second, int amount) {
        ResourceHandler<FluidResource> a = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(first), first.getOpposite());
        ResourceHandler<FluidResource> b = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(second), second.getOpposite());
        if (a == null || b == null || a == b) {
            return 0;
        }

        int moved = ResourceHandlerUtil.move(a, b, resource -> !resource.isEmpty(), amount, null);
        return moved > 0 ? moved : ResourceHandlerUtil.move(b, a, resource -> !resource.isEmpty(), amount, null);
    }

    private static int moveEnergy(Level level, BlockPos pos, Direction first, Direction second, int amount) {
        EnergyHandler a = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(first), first.getOpposite());
        EnergyHandler b = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(second), second.getOpposite());
        if (a == null || b == null || a == b) {
            return 0;
        }

        int moved = EnergyHandlerUtil.move(a, b, amount, null);
        return moved > 0 ? moved : EnergyHandlerUtil.move(b, a, amount, null);
    }
}
