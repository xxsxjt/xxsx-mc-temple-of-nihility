package com.templenihility.storage;

import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.init.ModBlocks;
import com.templenihility.menu.NihilityTerminalMenu;
import com.templenihility.world.NihilityVisualEffects;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NihilityVaultNetwork {
    public static boolean open(ServerLevel level, BlockPos origin, Player player) {
        List<NihilityVaultBlockEntity> vaults = collect(level, origin);
        if (vaults.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.templenihility.vault_not_found"));
            return false;
        }

        int totalSlots = stats(vaults).totalSlots();
        MenuProvider provider = new SimpleMenuProvider((id, inventory, p) ->
            new NihilityTerminalMenu(id, inventory, vaults),
            Component.translatable("container.templenihility.nihility_terminal", vaults.size(), totalSlots));
        player.openMenu(provider);
        NihilityVisualEffects.vaultOpen(level, origin, vaults.size());
        return true;
    }

    public static List<NihilityVaultBlockEntity> collect(ServerLevel level, BlockPos origin) {
        List<NihilityVaultBlockEntity> vaults = new ArrayList<>();
        if (!level.isLoaded(origin) || !(level.getBlockEntity(origin) instanceof NihilityVaultBlockEntity)) {
            return vaults;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());

        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos) || !level.isLoaded(pos)) {
                continue;
            }

            if (!(level.getBlockEntity(pos) instanceof NihilityVaultBlockEntity vault)) {
                continue;
            }

            vaults.add(vault);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && level.getBlockState(next).is(ModBlocks.NIHILITY_VAULT.get())) {
                    queue.add(next.immutable());
                }
            }
        }

        vaults.sort(Comparator.comparingLong(v -> v.getBlockPos().asLong()));
        return vaults;
    }

    public static Stats stats(List<NihilityVaultBlockEntity> vaults) {
        int totalSlots = 0;
        int usedSlots = 0;
        int itemCount = 0;
        int chunkLoaded = 0;
        int breakProtected = 0;
        for (NihilityVaultBlockEntity vault : vaults) {
            totalSlots += vault.getItems().size();
            if (vault.isChunkLoaded()) {
                chunkLoaded++;
            }
            if (vault.isBreakProtected()) {
                breakProtected++;
            }
            for (ItemStack stack : vault.getItems()) {
                if (!stack.isEmpty()) {
                    usedSlots++;
                    itemCount = Math.min(Integer.MAX_VALUE, itemCount + stack.getCount());
                }
            }
        }
        return new Stats(vaults.size(), totalSlots, usedSlots, itemCount, chunkLoaded, breakProtected);
    }

    public static boolean setNetworkChunkLoaded(ServerLevel level, BlockPos origin, boolean enabled) {
        List<NihilityVaultBlockEntity> vaults = collect(level, origin);
        if (vaults.isEmpty()) {
            return false;
        }
        for (NihilityVaultBlockEntity vault : vaults) {
            vault.setChunkLoaded(enabled);
        }
        return true;
    }

    public static boolean setNetworkBreakProtected(ServerLevel level, BlockPos origin, boolean enabled) {
        List<NihilityVaultBlockEntity> vaults = collect(level, origin);
        if (vaults.isEmpty()) {
            return false;
        }
        for (NihilityVaultBlockEntity vault : vaults) {
            vault.setBreakProtected(enabled);
        }
        return true;
    }

    public record Stats(int vaultCount, int totalSlots, int usedSlots, int itemCount,
                        int chunkLoadedVaults, int breakProtectedVaults) {
        public int emptySlots() {
            return Math.max(0, totalSlots - usedSlots);
        }
    }

    private NihilityVaultNetwork() {
    }
}
