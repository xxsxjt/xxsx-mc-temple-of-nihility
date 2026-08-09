package com.templenihility.network;

import com.templenihility.TempleNihilityMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VaultSearchPayload(int containerId, String query) implements CustomPacketPayload {
    public static final Type<VaultSearchPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "vault_search"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VaultSearchPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.cast(), VaultSearchPayload::containerId,
        ByteBufCodecs.stringUtf8(64).cast(), VaultSearchPayload::query,
        VaultSearchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
