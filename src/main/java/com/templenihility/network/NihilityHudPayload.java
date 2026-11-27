package com.templenihility.network;

import com.templenihility.TempleNihilityMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NihilityHudPayload(
    int power,
    int maxPower,
    int armorPieces,
    int counterTicks,
    int gauntletStacks,
    int wardCooldownTicks,
    int overdriveTicks,
    int soulShieldTenths
) implements CustomPacketPayload {
    public static final Type<NihilityHudPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NihilityHudPayload> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeVarInt(value.power);
            buf.writeVarInt(value.maxPower);
            buf.writeVarInt(value.armorPieces);
            buf.writeVarInt(value.counterTicks);
            buf.writeVarInt(value.gauntletStacks);
            buf.writeVarInt(value.wardCooldownTicks);
            buf.writeVarInt(value.overdriveTicks);
            buf.writeVarInt(value.soulShieldTenths);
        },
        buf -> new NihilityHudPayload(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
