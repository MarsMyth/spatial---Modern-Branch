package dev.mrturtle.spatial.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetRotationPayload(int rotation) implements CustomPayload {
    public static final CustomPayload.Id<SetRotationPayload> ID =
            new CustomPayload.Id<>(Identifier.of("spatial", "set_rotation"));

    public static final PacketCodec<RegistryByteBuf, SetRotationPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeVarInt(payload.rotation),
                    buf -> new SetRotationPayload(buf.readVarInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}