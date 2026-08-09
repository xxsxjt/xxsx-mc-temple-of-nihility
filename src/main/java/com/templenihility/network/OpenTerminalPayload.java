package com.templenihility.network;

import com.templenihility.TempleNihilityMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenTerminalPayload() implements CustomPacketPayload {
    public static final OpenTerminalPayload INSTANCE = new OpenTerminalPayload();
    public static final Type<OpenTerminalPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "open_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTerminalPayload> CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
