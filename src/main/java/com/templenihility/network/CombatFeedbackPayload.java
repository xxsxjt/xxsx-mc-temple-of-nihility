package com.templenihility.network;

import com.templenihility.TempleNihilityMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CombatFeedbackPayload(
    int kindId,
    int energyDelta,
    String detail
) implements CustomPacketPayload {
    public static final Type<CombatFeedbackPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "combat_feedback"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatFeedbackPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.cast(), CombatFeedbackPayload::kindId,
        ByteBufCodecs.VAR_INT.cast(), CombatFeedbackPayload::energyDelta,
        ByteBufCodecs.stringUtf8(48).cast(), CombatFeedbackPayload::detail,
        CombatFeedbackPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
