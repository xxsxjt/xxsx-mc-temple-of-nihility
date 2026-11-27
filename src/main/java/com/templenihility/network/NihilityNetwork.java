package com.templenihility.network;

import com.templenihility.action.NihilityActionService;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.effect.CombatFeedbackPolicy;
import com.templenihility.effect.NihilityRelicEffects;
import com.templenihility.energy.VoidPower;
import com.templenihility.item.NihilityArmorEvents;
import com.templenihility.menu.NihilityTerminalMenu;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NihilityNetwork {
    private static volatile Consumer<NihilityHudPayload> clientHudHandler = p -> {};
    private static volatile Consumer<CombatFeedbackPayload> clientFeedbackHandler = p -> {};

    public static void setClientHudHandler(Consumer<NihilityHudPayload> handler) {
        clientHudHandler = handler != null ? handler : p -> {};
    }

    public static void setClientFeedbackHandler(Consumer<CombatFeedbackPayload> handler) {
        clientFeedbackHandler = handler != null ? handler : p -> {};
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
            .playToServer(VaultSearchPayload.TYPE, VaultSearchPayload.CODEC, NihilityNetwork::handleVaultSearch)
            .playToServer(OpenTerminalPayload.TYPE, OpenTerminalPayload.CODEC, NihilityNetwork::handleOpenTerminal)
            .playToClient(NihilityHudPayload.TYPE, NihilityHudPayload.CODEC, NihilityNetwork::handleHudSync)
            .playToClient(CombatFeedbackPayload.TYPE, CombatFeedbackPayload.CODEC, NihilityNetwork::handleCombatFeedback);
    }

    public static void syncHud(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long now = serverPlayer.level().getGameTime();
        int power = VoidPower.get(serverPlayer);
        int maxPower = VoidPower.getMax(serverPlayer);
        int pieces = NihilityArmorEvents.countEquippedPieces(serverPlayer);
        int counterTicks = NihilityArmorEvents.getZeroCounterRemainingTicks(serverPlayer, now);
        int gauntletStacks = CuriosCompat.getGauntletStacks(serverPlayer, now);
        int wardCooldownTicks = CuriosCompat.getWardCooldownTicks(serverPlayer, now);
        int overdriveTicks = CuriosCompat.getOverdriveTicks(serverPlayer, now);
        int soulShieldTenths = Math.round(NihilityRelicEffects.getActiveSoulShield(serverPlayer) * 10.0F);
        PacketDistributor.sendToPlayer(serverPlayer, new NihilityHudPayload(
            power, maxPower, pieces, counterTicks, gauntletStacks,
            wardCooldownTicks, overdriveTicks, soulShieldTenths
        ));
    }

    public static void sendCombatFeedback(Player player, CombatFeedbackPolicy.Kind kind,
                                          int energyDelta, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer,
            new CombatFeedbackPayload(kind.id, energyDelta, detail == null ? "" : detail));
        syncHud(serverPlayer);
    }

    private static void handleHudSync(NihilityHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHudHandler.accept(payload));
    }

    private static void handleCombatFeedback(CombatFeedbackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientFeedbackHandler.accept(payload));
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
