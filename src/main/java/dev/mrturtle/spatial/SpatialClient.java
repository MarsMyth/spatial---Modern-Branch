package dev.mrturtle.spatial;

import dev.mrturtle.spatial.networking.SyncShapesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class SpatialClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                SyncShapesPayload.ID,
                (payload, context) -> {
                    Spatial.setShapes(payload.shapes());
                    Spatial.LOGGER.info("Received shape sync packet from server, loaded {} shapes", payload.shapes().size());
                }
        );
    }
}