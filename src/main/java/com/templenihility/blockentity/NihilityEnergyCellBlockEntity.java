package com.templenihility.blockentity;

import com.templenihility.energy.VoidEnergyBlockHandler;
import com.templenihility.energy.VoidPower;
import com.templenihility.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public class NihilityEnergyCellBlockEntity extends BlockEntity {
    public static final int MAX_ENERGY = 10_000;
    public static final int MAX_TRANSFER = 512;
    private static final int PLAYER_CHARGE_PER_SECOND = 3;
    private static final double PLAYER_CHARGE_RANGE = 6.0;

    private final VoidEnergyBlockHandler energyHandler = new VoidEnergyBlockHandler(
        this::getEnergy,
        this::setEnergyDirect,
        () -> MAX_ENERGY,
        MAX_TRANSFER,
        MAX_TRANSFER,
        this::setChanged);

    private int energy;

    public NihilityEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIHILITY_ENERGY_CELL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NihilityEnergyCellBlockEntity cell) {
        if (level.isClientSide() || level.getGameTime() % 20 != 0 || cell.energy <= 0) {
            return;
        }

        int before = cell.energy;
        for (Player player : level.players()) {
            if (cell.energy <= 0) {
                break;
            }
            if (!VoidPower.canStore(player)
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    > PLAYER_CHARGE_RANGE * PLAYER_CHARGE_RANGE) {
                continue;
            }

            int playerBefore = VoidPower.get(player);
            int moved = Math.min(PLAYER_CHARGE_PER_SECOND, cell.energy);
            int playerAfter = VoidPower.add(player, moved);
            cell.energy -= Math.max(0, playerAfter - playerBefore);
        }

        if (cell.energy != before) {
            cell.setChanged();
        }
    }

    public int getEnergy() {
        return energy;
    }

    public EnergyHandler getEnergyHandler() {
        return energyHandler;
    }

    public int addEnergy(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = energy;
        energy = Math.min(MAX_ENERGY, energy + amount);
        int accepted = energy - before;
        if (accepted > 0) {
            setChanged();
        }
        return accepted;
    }

    public int extractEnergy(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int extracted = Math.min(amount, energy);
        energy -= extracted;
        if (extracted > 0) {
            setChanged();
        }
        return extracted;
    }

    private void setEnergyDirect(int amount) {
        energy = Math.max(0, Math.min(MAX_ENERGY, amount));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        setEnergyDirect(input.getIntOr("VoidEnergy", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("VoidEnergy", energy);
    }
}
