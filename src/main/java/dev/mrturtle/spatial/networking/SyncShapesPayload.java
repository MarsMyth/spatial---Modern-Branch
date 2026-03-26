package dev.mrturtle.spatial.networking;

import dev.mrturtle.spatial.inventory.InventoryShape;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SyncShapesPayload(HashMap<Identifier, InventoryShape> shapes) implements CustomPayload {

    public static final CustomPayload.Id<SyncShapesPayload> ID =
            new CustomPayload.Id<>(SpatialNetworking.SYNC_SHAPES_PACKET_ID);

    public static final PacketCodec<RegistryByteBuf, SyncShapesPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.shapes.size());
                for (Map.Entry<Identifier, InventoryShape> entry : payload.shapes.entrySet()) {
                    buf.writeIdentifier(entry.getKey());
                    entry.getValue().writeTo(buf);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                HashMap<Identifier, InventoryShape> shapes = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    Identifier key = buf.readIdentifier();
                    InventoryShape value = InventoryShape.readFrom(buf);
                    shapes.put(key, value);
                }
                return new SyncShapesPayload(shapes);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}