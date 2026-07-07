package com.templenihility.blockentity;

import com.templenihility.init.ModBlockEntities;
import com.templenihility.init.ModBlocks;
import com.templenihility.init.ModItems;
import com.templenihility.energy.VoidPower;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NihilityAltarBlockEntity extends BlockEntity implements Container {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final int[] structureData = new int[5];
    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);

    public NihilityAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIHILITY_ALTAR.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public int[] getStructureData() {
        refreshStructure();
        return structureData;
    }

    public StructureInfo refreshStructure() {
        StructureInfo info = scanStructure();
        structureData[0] = info.tier();
        structureData[1] = info.runes();
        structureData[2] = info.crystals();
        structureData[3] = info.pillars();
        structureData[4] = info.chiseled();
        return info;
    }

    public boolean performRitual(Player player) {
        StructureInfo info = refreshStructure();
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.templenihility.altar_no_input"));
            playFailureEffects(info);
            return false;
        }

        RitualRecipe recipe = findRecipe(input, info.tier());
        if (recipe == null) {
            RitualRecipe known = findRecipeIgnoringTier(input);
            if (known != null && known.minTier() > info.tier()) {
                player.sendSystemMessage(Component.translatable("message.templenihility.altar_structure_too_weak",
                    Component.translatable(tierNameKey(known.minTier()))));
            } else if (known != null && input.getCount() < known.inputCount()) {
                player.sendSystemMessage(Component.translatable("message.templenihility.altar_not_enough",
                    known.inputCount(), input.getHoverName()));
            } else {
                player.sendSystemMessage(Component.translatable("message.templenihility.altar_no_recipe"));
            }
            playFailureEffects(info);
            return false;
        }

        ItemStack result = recipe.result().copy();
        ItemStack output = items.get(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > output.getMaxStackSize()) {
                player.sendSystemMessage(Component.translatable("message.templenihility.altar_output_blocked"));
                playFailureEffects(info);
                return false;
            }
        }

        if (!VoidPower.tryConsume(player, recipe.energyCost())) {
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.not_enough_void_power",
                recipe.energyCost(), VoidPower.get(player), VoidPower.getMax(player)));
            playFailureEffects(info);
            return false;
        }

        if (!output.isEmpty()) {
            output.grow(result.getCount());
        } else {
            items.set(OUTPUT_SLOT, result);
        }

        input.shrink(recipe.inputCount());
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        setChanged();
        playSuccessEffects(info);
        player.sendSystemMessage(Component.translatable("message.templenihility.altar_success",
            result.getCount(), result.getHoverName(), Component.translatable(tierNameKey(info.tier()))));
        if (recipe.energyCost() > 0) {
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.altar_energy_spent", recipe.energyCost(), VoidPower.get(player), VoidPower.getMax(player)));
        }
        return true;
    }

    public static String tierNameKey(int tier) {
        return switch (tier) {
            case 1 -> "screen.templenihility.altar_tier_1";
            case 2 -> "screen.templenihility.altar_tier_2";
            case 3 -> "screen.templenihility.altar_tier_3";
            default -> "screen.templenihility.altar_tier_0";
        };
    }

    private StructureInfo scanStructure() {
        if (level == null) {
            return new StructureInfo(0, 0, 0, 0, 0);
        }

        int runes = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(worldPosition.relative(direction)).is(ModBlocks.NIHILITY_RUNE_BRICKS.get())) {
                runes++;
            }
        }

        int crystals = 0;
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                if (level.getBlockState(worldPosition.offset(dx, 0, dz)).is(ModBlocks.NIHILITY_CRYSTAL_BLOCK.get())) {
                    crystals++;
                }
            }
        }

        int pillars = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(worldPosition.relative(direction, 2)).is(ModBlocks.NIHILITY_PILLAR.get())) {
                pillars++;
            }
        }

        int chiseled = 0;
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                if (level.getBlockState(worldPosition.offset(dx, 0, dz)).is(ModBlocks.CHISELED_NIHILITY_STONE.get())) {
                    chiseled++;
                }
            }
        }

        int tier = 0;
        if (runes >= 4) {
            tier = 1;
        }
        if (tier >= 1 && crystals >= 4) {
            tier = 2;
        }
        if (tier >= 2 && pillars >= 4 && chiseled >= 4) {
            tier = 3;
        }
        return new StructureInfo(tier, runes, crystals, pillars, chiseled);
    }

    public void playStructurePreviewEffects() {
        StructureInfo info = refreshStructure();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 1.05;
        double cz = worldPosition.getZ() + 0.5;
        if (info.tier() <= 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 8, 0.25, 0.12, 0.25, 0.01);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.35F, 0.65F);
            return;
        }

        serverLevel.sendParticles(ParticleTypes.ENCHANT, cx, cy, cz, 18 + info.tier() * 6, 0.35, 0.18, 0.35, 0.02);
        spawnComponentPulses(serverLevel, info, false);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.45F, 0.85F + info.tier() * 0.12F);
    }

    private void playFailureEffects(StructureInfo info) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 1.05;
        double cz = worldPosition.getZ() + 0.5;
        serverLevel.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 16, 0.35, 0.18, 0.35, 0.015);
        serverLevel.sendParticles(ParticleTypes.WITCH, cx, cy + 0.1, cz, 8, 0.22, 0.10, 0.22, 0.02);
        if (info.tier() > 0) {
            spawnRing(serverLevel, 1.15 + info.tier() * 0.45, ParticleTypes.REVERSE_PORTAL, 18, 0.0);
        }
        level.playSound(null, worldPosition, SoundEvents.VAULT_INSERT_ITEM_FAIL, SoundSource.BLOCKS, 0.65F, 0.78F);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.55F, 0.55F);
    }

    private void playSuccessEffects(StructureInfo info) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 1.0;
        double cz = worldPosition.getZ() + 0.5;
        int tier = Math.max(0, info.tier());
        serverLevel.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.45, cz, 18 + tier * 8, 0.22, 0.35, 0.22, 0.04);
        serverLevel.sendParticles(ParticleTypes.PORTAL, cx, cy + 0.25, cz, 36 + tier * 18, 0.55 + tier * 0.15, 0.28, 0.55 + tier * 0.15, 0.16);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, cx, cy + 0.65, cz, 32 + tier * 12, 0.65, 0.35, 0.65, 0.05);
        spawnVerticalSpiral(serverLevel, tier);
        spawnRing(serverLevel, 1.15, ParticleTypes.ENCHANT, 32, 0.03);
        if (tier >= 2) {
            spawnRing(serverLevel, 1.75, ParticleTypes.SOUL_FIRE_FLAME, 44, 0.02);
        }
        if (tier >= 3) {
            spawnRing(serverLevel, 2.55, ParticleTypes.ELECTRIC_SPARK, 56, 0.035);
        }
        spawnComponentPulses(serverLevel, info, true);

        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.85F, 0.85F + tier * 0.08F);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, 1.0F + tier * 0.1F);
        if (tier >= 2) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.55F, 1.05F);
        }
        if (tier >= 3) {
            level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.55F, 1.15F);
        }
    }

    private void spawnVerticalSpiral(ServerLevel serverLevel, int tier) {
        double cx = worldPosition.getX() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        int steps = 28 + Math.max(0, tier) * 10;
        for (int i = 0; i < steps; i++) {
            double angle = i * 0.45;
            double radius = 0.22 + i * 0.018;
            double x = cx + Math.cos(angle) * radius;
            double y = worldPosition.getY() + 0.35 + i * 0.045;
            double z = cz + Math.sin(angle) * radius;
            serverLevel.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL,
                x, y, z, 1, 0.015, 0.015, 0.015, 0.01);
        }
    }

    private void spawnRing(ServerLevel serverLevel, double radius, ParticleOptions particle, int points, double speed) {
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 1.02;
        double cz = worldPosition.getZ() + 0.5;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            double vx = Math.cos(angle) * 0.035;
            double vz = Math.sin(angle) * 0.035;
            serverLevel.sendParticles(particle, x, cy, z, 1, vx, 0.015, vz, speed);
        }
    }

    private void spawnComponentPulses(ServerLevel serverLevel, StructureInfo info, boolean strong) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos rune = worldPosition.relative(direction);
            if (level.getBlockState(rune).is(ModBlocks.NIHILITY_RUNE_BRICKS.get())) {
                pulseBlock(serverLevel, rune, strong ? ParticleTypes.ENCHANT : ParticleTypes.REVERSE_PORTAL, strong ? 8 : 3);
            }

            BlockPos pillar = worldPosition.relative(direction, 2);
            if (info.tier() >= 3 && level.getBlockState(pillar).is(ModBlocks.NIHILITY_PILLAR.get())) {
                pulseBlock(serverLevel, pillar, ParticleTypes.ELECTRIC_SPARK, strong ? 10 : 4);
                if (strong) {
                    drawParticleLine(serverLevel, pillar, ParticleTypes.ELECTRIC_SPARK);
                }
            }
        }

        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                BlockPos crystal = worldPosition.offset(dx, 0, dz);
                if (info.tier() >= 2 && level.getBlockState(crystal).is(ModBlocks.NIHILITY_CRYSTAL_BLOCK.get())) {
                    pulseBlock(serverLevel, crystal, ParticleTypes.SOUL_FIRE_FLAME, strong ? 10 : 4);
                    if (strong) {
                        drawParticleLine(serverLevel, crystal, ParticleTypes.SOUL);
                    }
                }
            }
        }

        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                BlockPos chiseled = worldPosition.offset(dx, 0, dz);
                if (info.tier() >= 3 && level.getBlockState(chiseled).is(ModBlocks.CHISELED_NIHILITY_STONE.get())) {
                    pulseBlock(serverLevel, chiseled, ParticleTypes.PORTAL, strong ? 12 : 5);
                }
            }
        }
    }

    private void pulseBlock(ServerLevel serverLevel, BlockPos pos, ParticleOptions particle, int count) {
        serverLevel.sendParticles(particle,
            pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
            count, 0.22, 0.18, 0.22, 0.025);
    }

    private void drawParticleLine(ServerLevel serverLevel, BlockPos from, ParticleOptions particle) {
        double ax = from.getX() + 0.5;
        double ay = from.getY() + 1.08;
        double az = from.getZ() + 0.5;
        double bx = worldPosition.getX() + 0.5;
        double by = worldPosition.getY() + 1.08;
        double bz = worldPosition.getZ() + 0.5;
        for (int i = 1; i <= 8; i++) {
            double t = i / 9.0;
            double x = ax + (bx - ax) * t;
            double y = ay + (by - ay) * t + Math.sin(t * Math.PI) * 0.25;
            double z = az + (bz - az) * t;
            serverLevel.sendParticles(particle, x, y, z, 1, 0.01, 0.01, 0.01, 0.01);
        }
    }

    private static RitualRecipe findRecipe(ItemStack stack, int tier) {
        for (RitualRecipe recipe : recipes()) {
            if (stack.is(recipe.input()) && stack.getCount() >= recipe.inputCount() && tier >= recipe.minTier()) {
                return recipe;
            }
        }
        return null;
    }

    private static RitualRecipe findRecipeIgnoringTier(ItemStack stack) {
        for (RitualRecipe recipe : recipes()) {
            if (stack.is(recipe.input())) {
                return recipe;
            }
        }
        return null;
    }

    private static List<RitualRecipe> recipes() {
        return List.of(
            new RitualRecipe(ModItems.NIHILITY_DUST.get(), 8, new ItemStack(ModItems.NIHILITY_SHARD.get()), 0, 0),
            new RitualRecipe(ModItems.NIHILITY_SHARD.get(), 4, new ItemStack(ModItems.NIHILITY_CRYSTAL.get()), 1, 3),
            new RitualRecipe(ModItems.NIHILITY_CRYSTAL.get(), 4, new ItemStack(ModItems.NIHILITY_RUNE.get()), 1, 6),
            new RitualRecipe(ModItems.NIHILITY_RUNE.get(), 4, new ItemStack(ModItems.TEMPLE_SEAL.get()), 2, 10),
            new RitualRecipe(ModItems.TEMPLE_SEAL.get(), 4, new ItemStack(ModItems.NIHILITY_CORE.get()), 2, 16),
            new RitualRecipe(ModItems.NIHILITY_CORE.get(), 4, new ItemStack(ModItems.NIHILITY_RESONANCE_CORE.get()), 3, 24),
            new RitualRecipe(ModItems.NIHILITY_RELIC_FRAGMENT.get(), 8, new ItemStack(ModItems.TEMPLE_SEAL.get()), 3, 20)
        );
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    public record StructureInfo(int tier, int runes, int crystals, int pillars, int chiseled) {
    }

    private record RitualRecipe(Item input, int inputCount, ItemStack result, int minTier, int energyCost) {
    }
}
