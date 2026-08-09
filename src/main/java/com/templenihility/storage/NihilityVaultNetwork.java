package com.templenihility.storage;

import com.templenihility.TempleNihilityMod;
import com.templenihility.block.NihilityVaultBlock;
import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.config.ModConfig;
import com.templenihility.init.ModBlocks;
import com.templenihility.menu.NihilityTerminalMenu;
import com.templenihility.world.NihilityVisualEffects;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NihilityVaultNetwork {
    public static boolean open(ServerLevel level, BlockPos origin, Player player) {
        AccessResult access = inspect(level, origin, player, true);
        if (!access.allowed()) {
            sendFailure(player, access.failure());
            return false;
        }
        List<NihilityVaultBlockEntity> vaults = access.vaults();

        int totalSlots = stats(vaults).totalSlots();
        MenuProvider provider = new SimpleMenuProvider((id, inventory, p) ->
            new NihilityTerminalMenu(id, inventory, vaults),
            Component.translatable("container.templenihility.nihility_terminal", vaults.size(), totalSlots));
        player.openMenu(provider);
        NihilityVisualEffects.vaultOpen(level, origin, vaults.size());
        return true;
    }

    public static boolean prepareAccess(ServerLevel level, BlockPos origin, Player player) {
        AccessResult access = inspect(level, origin, player, true);
        if (!access.allowed()) {
            sendFailure(player, access.failure());
        }
        return access.allowed();
    }

    public static boolean canAccess(List<NihilityVaultBlockEntity> vaults, Player player) {
        if (isAdmin(player)) {
            return true;
        }
        UUID owner = null;
        for (NihilityVaultBlockEntity vault : vaults) {
            UUID current = vault.getOwner().orElse(null);
            if (current == null) {
                continue;
            }
            if (owner != null && !owner.equals(current)) {
                return false;
            }
            owner = current;
        }
        return owner == null || owner.equals(player.getUUID());
    }

    private static ScanResult scan(ServerLevel level, BlockPos origin, UUID accessorOwner) {
        List<NihilityVaultBlockEntity> vaults = new ArrayList<>();
        if (!level.isLoaded(origin)
            || !(level.getBlockEntity(origin) instanceof NihilityVaultBlockEntity originVault)) {
            return new ScanResult(vaults, false);
        }

        UUID networkOwner = originVault.getOwner().orElse(accessorOwner);
        int maxBlocks = ModConfig.VAULT_MAX_NETWORK_BLOCKS.get();
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
            UUID currentOwner = vault.getOwner().orElse(null);
            if (currentOwner != null && !currentOwner.equals(networkOwner)) {
                continue;
            }

            vaults.add(vault);
            if (vaults.size() > maxBlocks) {
                return new ScanResult(vaults, true);
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && level.getBlockState(next).is(ModBlocks.NIHILITY_VAULT.get())) {
                    queue.add(next.immutable());
                }
            }
        }

        vaults.sort(Comparator.comparingLong(v -> v.getBlockPos().asLong()));
        return new ScanResult(vaults, false);
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

    public static boolean setNetworkChunkLoaded(ServerLevel level, BlockPos origin, Player player, boolean enabled) {
        AccessResult access = inspect(level, origin, player, false);
        if (!access.allowed()) {
            sendFailure(player, access.failure());
            return false;
        }
        List<NihilityVaultBlockEntity> vaults = access.vaults();
        if (enabled) {
            Set<Long> chunks = new HashSet<>();
            for (NihilityVaultBlockEntity vault : vaults) {
                chunks.add(ChunkPos.containing(vault.getBlockPos()).pack());
            }
            int maxChunks = ModConfig.VAULT_MAX_FORCED_CHUNKS.get();
            if (chunks.size() > maxChunks) {
                player.sendSystemMessage(Component.translatable(
                    "message.templenihility.vault_chunkload_limit", chunks.size(), maxChunks));
                return false;
            }
        }
        return applyNetworkFlag(player, vaults, enabled,
            NihilityVaultBlockEntity::isChunkLoaded,
            NihilityVaultBlockEntity::setChunkLoaded,
            "chunk loading");
    }

    public static boolean setNetworkBreakProtected(ServerLevel level, BlockPos origin, Player player, boolean enabled) {
        AccessResult access = inspect(level, origin, player, false);
        if (!access.allowed()) {
            sendFailure(player, access.failure());
            return false;
        }
        return applyNetworkFlag(player, access.vaults(), enabled,
            NihilityVaultBlockEntity::isBreakProtected,
            NihilityVaultBlockEntity::setBreakProtected,
            "break protection");
    }

    private static AccessResult inspect(ServerLevel level, BlockPos origin, Player player, boolean claimUnowned) {
        ScanResult scan = scan(level, origin, player.getUUID());
        if (scan.vaults().isEmpty()) {
            return new AccessResult(List.of(), AccessFailure.NOT_FOUND);
        }
        if (scan.exceededLimit()) {
            return new AccessResult(List.of(), AccessFailure.TOO_LARGE);
        }

        Set<UUID> owners = new HashSet<>();
        for (NihilityVaultBlockEntity vault : scan.vaults()) {
            vault.getOwner().ifPresent(owners::add);
        }
        boolean admin = isAdmin(player);
        if (owners.size() > 1 && !admin) {
            return new AccessResult(List.of(), AccessFailure.MIXED_OWNERS);
        }
        UUID owner = owners.size() == 1 ? owners.iterator().next() : null;
        if (owner != null && !owner.equals(player.getUUID()) && !admin) {
            return new AccessResult(List.of(), AccessFailure.DENIED);
        }

        if (claimUnowned && owners.size() <= 1) {
            UUID claimOwner = owner == null ? player.getUUID() : owner;
            boolean claimedLegacyNetwork = owner == null;
            for (NihilityVaultBlockEntity vault : scan.vaults()) {
                if (vault.claimOwner(claimOwner)) {
                    NihilityVaultBlock.refreshConnections(level, vault.getBlockPos());
                }
            }
            if (claimedLegacyNetwork) {
                player.sendSystemMessage(Component.translatable("message.templenihility.vault_claimed"));
            }
        }
        return new AccessResult(List.copyOf(scan.vaults()), AccessFailure.NONE);
    }

    private static boolean isAdmin(Player player) {
        return player instanceof ServerPlayer serverPlayer
            && serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static boolean applyNetworkFlag(Player player, List<NihilityVaultBlockEntity> vaults,
                                            boolean enabled,
                                            Predicate<NihilityVaultBlockEntity> getter,
                                            BiConsumer<NihilityVaultBlockEntity, Boolean> setter,
                                            String operation) {
        List<VaultFlagSnapshot> changed = new ArrayList<>();
        try {
            for (NihilityVaultBlockEntity vault : vaults) {
                boolean previous = getter.test(vault);
                if (previous == enabled) {
                    continue;
                }
                changed.add(new VaultFlagSnapshot(vault, previous));
                setter.accept(vault, enabled);
            }
            return true;
        } catch (RuntimeException exception) {
            for (int index = changed.size() - 1; index >= 0; index--) {
                VaultFlagSnapshot snapshot = changed.get(index);
                try {
                    setter.accept(snapshot.vault(), snapshot.previous());
                } catch (RuntimeException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
            }
            TempleNihilityMod.LOGGER.error(
                "Failed to update Void Vault network {} for {}", operation, player.getScoreboardName(), exception);
            player.sendSystemMessage(Component.translatable("message.templenihility.vault_update_failed"));
            return false;
        }
    }

    private static void sendFailure(Player player, AccessFailure failure) {
        switch (failure) {
            case NOT_FOUND -> player.sendSystemMessage(Component.translatable("message.templenihility.vault_not_found"));
            case TOO_LARGE -> player.sendSystemMessage(Component.translatable(
                "message.templenihility.vault_network_too_large", ModConfig.VAULT_MAX_NETWORK_BLOCKS.get()));
            case MIXED_OWNERS -> player.sendSystemMessage(Component.translatable("message.templenihility.vault_mixed_owners"));
            case DENIED -> player.sendSystemMessage(Component.translatable("message.templenihility.vault_access_denied"));
            case NONE -> {
            }
        }
    }

    public record Stats(int vaultCount, int totalSlots, int usedSlots, int itemCount,
                        int chunkLoadedVaults, int breakProtectedVaults) {
        public int emptySlots() {
            return Math.max(0, totalSlots - usedSlots);
        }
    }

    private record ScanResult(List<NihilityVaultBlockEntity> vaults, boolean exceededLimit) {
    }

    private record VaultFlagSnapshot(NihilityVaultBlockEntity vault, boolean previous) {
    }

    private record AccessResult(List<NihilityVaultBlockEntity> vaults, AccessFailure failure) {
        boolean allowed() {
            return failure == AccessFailure.NONE;
        }
    }

    private enum AccessFailure {
        NONE,
        NOT_FOUND,
        TOO_LARGE,
        MIXED_OWNERS,
        DENIED
    }

    private NihilityVaultNetwork() {
    }
}
