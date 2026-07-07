package com.templenihility.world;

import com.mojang.brigadier.CommandDispatcher;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.init.ModItems;
import com.templenihility.item.NihilityTerminalItem;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** 手动生成圣殿的指令 */
public class TempleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("temple")
            .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("shrine")
                .executes(ctx -> spawn(ctx.getSource(), TempleType.SHRINE)))
            .then(Commands.literal("temple")
                .executes(ctx -> spawn(ctx.getSource(), TempleType.TEMPLE)))
            .then(Commands.literal("complex")
                .executes(ctx -> spawn(ctx.getSource(), TempleType.TEMPLE_COMPLEX)))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "§e/temple shrine|temple|complex — 在当前位置生成圣殿"), false);
                return 1;
            })
        );

        dispatcher.register(Commands.literal("nihility")
            .then(Commands.literal("help")
                .executes(ctx -> help(ctx.getSource())))
            .then(Commands.literal("terminal")
                .executes(ctx -> openTerminal(ctx.getSource())))
            .executes(ctx -> help(ctx.getSource()))
        );
    }

    private static int spawn(CommandSourceStack src, TempleType type) {
        if (!(src.getLevel() instanceof ServerLevel)) return 0;
        ServerLevel level = (ServerLevel) src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        try {
            TempleGenerator.generateTemple(level, pos, type);
            src.sendSuccess(() -> Component.literal("§a已生成: " + type.name()), false);
        } catch (Exception e) {
            src.sendFailure(Component.literal("§c生成失败: " + e.getMessage()));
        }
        return 1;
    }

    private static int openTerminal(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Only players can open a Void Terminal."));
            return 0;
        }

        Optional<ItemStack> terminal = findTerminal(player);
        if (terminal.isEmpty()) {
            src.sendFailure(Component.translatable("message.templenihility.terminal_missing"));
            return 0;
        }

        return NihilityTerminalItem.openBoundVault(player, terminal.get()) ? 1 : 0;
    }

    private static int help(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("message.templenihility.help_title"), false);
        src.sendSuccess(() -> Component.translatable("message.templenihility.help_vault"), false);
        src.sendSuccess(() -> Component.translatable("message.templenihility.help_terminal"), false);
        src.sendSuccess(() -> Component.translatable("message.templenihility.help_chunkload"), false);
        return 1;
    }

    private static Optional<ItemStack> findTerminal(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.NIHILITY_TERMINAL.get())) {
                return Optional.of(stack);
            }
        }

        if (ModList.get().isLoaded("curios")) {
            return CuriosCompat.findTerminal(player);
        }
        return Optional.empty();
    }
}
