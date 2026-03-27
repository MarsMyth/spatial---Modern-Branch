package dev.mrturtle.spatial;

import dev.mrturtle.spatial.networking.SetRotationPayload;
import dev.mrturtle.spatial.networking.SyncShapesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.concurrent.atomic.AtomicInteger;

public class SpatialClient implements ClientModInitializer {
    public static int cursorRotation = 0;
    private static void applyRotationNbt(ItemStack stack, int rotation) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = customData != null ? customData.copyNbt() : new NbtCompound();
        nbt.putInt("spatialRotation", rotation);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                SyncShapesPayload.ID,
                (payload, context) -> {
                    Spatial.setShapes(payload.shapes());
                    Spatial.LOGGER.info("Received shape sync packet from server, loaded {} shapes", payload.shapes().size());
                }
        );

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen<?> handledScreen))
                return;

            // Read rotation from server NBT if cursor has an item, otherwise reset
            ItemStack initialCursor = handledScreen.getScreenHandler().getCursorStack();
            if (!initialCursor.isEmpty()) {
                NbtComponent customData = initialCursor.get(DataComponentTypes.CUSTOM_DATA);
                cursorRotation = customData != null ? customData.copyNbt().getInt("spatialRotation") : 0;
            } else {
                cursorRotation = 0;
            }

            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
                // Read cursor fresh every time
                ItemStack cursor = handledScreen.getScreenHandler().getCursorStack();
                if (cursor.isEmpty()) return true;
                if (Spatial.getShape(cursor).shape.size() <= 1) return true;
                cursorRotation = ((cursorRotation + (verticalAmount > 0 ? 1 : -1)) % 4 + 4) % 4;
                Spatial.LOGGER.info("cursorRotation is now: {}", cursorRotation);
                ClientPlayNetworking.send(new SetRotationPayload(cursorRotation));
                return false;
            });
        });
    }
}