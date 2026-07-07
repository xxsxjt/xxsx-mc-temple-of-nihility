package com.templenihility.blockentity;

import com.templenihility.energy.VoidEnergyBlockHandler;
import com.templenihility.energy.VoidPower;
import com.templenihility.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public class NihilityEnergyPrismBlockEntity extends BlockEntity {
    public static final int MAX_BUFFER = 1_200;
    public static final int MAX_TRANSFER = 128;
    private static final int GENERATE_PER_SECOND = 8;
    private static final int CELL_TRANSFER_PER_SECOND = 32;
    private static final int PLAYER_CHARGE_PER_SECOND = 2;
    private static final double PLAYER_CHARGE_RANGE = 8.0;

    private final VoidEnergyBlockHandler energyHandler = new VoidEnergyBlockHandler(
        this::getEnergy,
        this::setEnergyDirect,
        () -> MAX_BUFFER,
        MAX_TRANSFER,
        MAX_TRANSFER,
        this::setChanged);

    private int energy;

    public NihilityEnergyPrismBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIHILITY_ENERGY_PRISM.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NihilityEnergyPrismBlockEntity prism) {
        if (level.isClientSide() || level.getGameTime() % 20 != 0) {
            return;
        }

        int before = prism.energy;
        prism.energy = Math.min(MAX_BUFFER, prism.energy + GENERATE_PER_SECOND);
        prism.pushToAdjacentCells(level, pos);
        prism.chargeNearbyPlayers(level, pos);

        if (prism.energy != before) {
            prism.setChanged();
        }
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 40 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.08, pos.getZ() + 0.5,
                6, 0.26, 0.18, 0.26, 0.025);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.18F, 1.35F);
        }
    }

    public int getEnergy() {
        return energy;
    }

    public EnergyHandler getEnergyHandler() {
        return energyHandler;
    }

    private void pushToAdjacentCells(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (energy <= 0) {
                return;
            }
            if (level.getBlockEntity(pos.relative(direction)) instanceof NihilityEnergyCellBlockEntity cell) {
                int accepted = cell.addEnergy(Math.min(CELL_TRANSFER_PER_SECOND, energy));
                energy -= accepted;
            }
        }
    }

    private void chargeNearbyPlayers(Level level, BlockPos pos) {
        for (Player player : level.players()) {
            if (energy <= 0) {
                return;
            }
            if (!VoidPower.canStore(player)
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    > PLAYER_CHARGE_RANGE * PLAYER_CHARGE_RANGE) {
                continue;
            }

            int playerBefore = VoidPower.get(player);
            int playerAfter = VoidPower.add(player, Math.min(PLAYER_CHARGE_PER_SECOND, energy));
            energy -= Math.max(0, playerAfter - playerBefore);
        }
    }

    private void setEnergyDirect(int amount) {
        energy = Math.max(0, Math.min(MAX_BUFFER, amount));
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
