package com.templenihility.network;

import com.templenihility.action.NihilityActionService;
import com.templenihility.menu.NihilityTerminalMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NihilityNetwork {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
            .playToServer(VaultSearchPayload.TYPE, VaultSearchPayload.CODEC, NihilityNetwork::handleVaultSearch)
            .playToServer(OpenTerminalPayload.TYPE, OpenTerminalPayload.CODEC, NihilityNetwork::handleOpenTerminal);
    }

    private static void handleVaultSearch(VaultSearchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof NihilityTerminalMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
                menu.setSearchQuery(payload.query());
            }
        });
    }

    private static void handleOpenTerminal(OpenTerminalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            NihilityActionService.openPortableTerminal(player);
        });
    }

    private NihilityNetwork() {
    }
}
