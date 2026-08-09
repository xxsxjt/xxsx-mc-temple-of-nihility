package com.templenihility.world;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerLevel;

/**
 * 管理员调试入口。正常玩法不得依赖聊天命令，玩家功能由物品、菜单、网络包和服务端动作服务触发。
 */
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

}
