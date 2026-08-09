package com.templenihility.action;

import com.templenihility.item.NihilityTerminalItem;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 服务端玩法动作的统一入口。
 *
 * <p>客户端只提交明确的动作意图，实际目标、权限、维度、区块、冷却和资源校验在这里或下游服务端
 * API 中完成。这个类不执行聊天命令，也不把玩法转换成命令字符串。</p>
 */
public final class NihilityActionService {
    private static final int TERMINAL_OPEN_DEBOUNCE_TICKS = 5;
    private static final TickRateLimiter<UUID> TERMINAL_OPEN_LIMITER = new TickRateLimiter<>();

    public static boolean openPortableTerminal(ServerPlayer player) {
        if (!acquireTerminalOpen(player)) {
            return false;
        }
        Optional<ItemStack> terminal = NihilityTerminalItem.findAvailableTerminal(player);
        if (terminal.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.templenihility.terminal_missing"));
            return false;
        }
        return NihilityTerminalItem.openBoundVault(player, terminal.get());
    }

    public static boolean openBoundTerminal(Player player, ItemStack terminal) {
        if (!(player instanceof ServerPlayer serverPlayer) || !acquireTerminalOpen(serverPlayer)) {
            return false;
        }
        return NihilityTerminalItem.openBoundVault(player, terminal);
    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TERMINAL_OPEN_LIMITER.clear(event.getEntity().getUUID());
    }

    private static boolean acquireTerminalOpen(ServerPlayer player) {
        return TERMINAL_OPEN_LIMITER.tryAcquire(
            player.getUUID(), player.level().getServer().getTickCount(), TERMINAL_OPEN_DEBOUNCE_TICKS);
    }

    private NihilityActionService() {
    }
}
