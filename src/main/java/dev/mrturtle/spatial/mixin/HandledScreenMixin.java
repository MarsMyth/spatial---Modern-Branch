package dev.mrturtle.spatial.mixin;

import dev.mrturtle.spatial.Spatial;
import dev.mrturtle.spatial.SpatialClient;
import dev.mrturtle.spatial.inventory.InventoryPosition;
import dev.mrturtle.spatial.inventory.InventoryShape;
import dev.mrturtle.spatial.util.RotationUtil;
import dev.mrturtle.spatial.util.SpatialUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> {
    @Shadow protected abstract void drawItem(DrawContext context, ItemStack stack, int x, int y, String amountText);

    @Shadow protected int x;

    @Shadow protected int y;

    @Shadow @Final protected T handler;

    @Shadow @Nullable protected abstract Slot getSlotAt(double x, double y);

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static int getRotation(ItemStack stack) {
        return SpatialClient.cursorRotation;
    }

    private static InventoryShape getRotatedShapeFromStack(ItemStack stack) {
        InventoryShape base = Spatial.getShape(stack);
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return base;
        int rotation = customData.copyNbt().getInt("spatialRotation");
        return RotationUtil.applyRotation(base, rotation);
    }

    private InventoryShape getShapeWithRotation(ItemStack stack) {
        InventoryShape base = Spatial.getShape(stack);
        return RotationUtil.applyRotation(base, getRotation(stack));
    }

    // Draws the texture once spanning the full shape footprint, or falls back to per-slot color
    private void drawShapeBackground(DrawContext context, ItemStack stack, int mainX, int mainY, InventoryShape shape, boolean isMain) {
        Identifier texture = SpatialUtil.getSlotTexture(stack);
        InventoryPosition anchor = shape.shape.get(0);
        if (texture != null) {
            int minX = shape.shape.stream().mapToInt(p -> p.x).min().orElse(0);
            int minY = shape.shape.stream().mapToInt(p -> p.y).min().orElse(0);
            int maxX = shape.shape.stream().mapToInt(p -> p.x).max().orElse(0);
            int maxY = shape.shape.stream().mapToInt(p -> p.y).max().orElse(0);

            // mainX/mainY is the anchor slot's screen pos; offset back to top-left of bounding box
            int originX = mainX - (anchor.x - minX) * 18;
            int originY = mainY - (anchor.y - minY) * 18;
            int pixelWidth  = (maxX - minX + 1) * 18;
            int pixelHeight = (maxY - minY + 1) * 18;

            context.drawGuiTexture(texture, originX - 1, originY - 1, pixelWidth, pixelHeight);
        } else {
            for (InventoryPosition pos : shape.shape) {
                int px = (pos.x - anchor.x) * 18 + mainX;
                int py = (pos.y - anchor.y) * 18 + mainY;
                boolean isMainPos = pos == anchor;
                context.fill(RenderLayer.getGuiOverlay(),
                        px - 1, py - 1, px + 17, py + 17,
                        SpatialUtil.colorFromItemStack(stack, isMainPos));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Draw the shape highlight on placed slots (only on the real item, not ghosts)
    // -----------------------------------------------------------------------

    @Inject(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;III)V", shift = At.Shift.AFTER, ordinal = 0))
    public void drawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ItemStack stack = slot.getStack();
        if (stack.isEmpty())
            return;
        if (handler instanceof CreativeInventoryScreen.CreativeScreenHandler)
            return;

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null && customData.copyNbt().getBoolean("isSpatialCopy"))
            return;

        Identifier texture = SpatialUtil.getSlotTexture(stack);
        if (texture == null && Spatial.getShape(stack).shape.size() == 1)
            return;

        InventoryShape shape = getRotatedShapeFromStack(stack);

        boolean isHotbarSlot = slot.inventory instanceof PlayerInventory
                && (slot.getIndex() <= 8 || slot.getIndex() == 40);
        if (isHotbarSlot) {
            if ((HandledScreen)(Object)this instanceof InventoryScreen)  // <-- new line
                return;
            if (texture != null)
                context.drawGuiTexture(texture, slot.x - 1, slot.y - 1, 18, 18);
            else
                context.fill(RenderLayer.getGuiOverlay(), slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17,
                        SpatialUtil.colorFromItemStack(stack, true));
            return;
        }

        if (shape.shape.size() == 1)
            return;

        drawShapeBackground(context, stack, slot.x, slot.y, shape, true);
    }

    // -----------------------------------------------------------------------
    // Draw the cursor stack with rotation applied
    // -----------------------------------------------------------------------

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawItem(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", shift = At.Shift.BEFORE, ordinal = 0))
    public void renderCursorStack(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ItemStack stack = handler.getCursorStack();
        if (stack.isEmpty())
            return;
        if (Spatial.getShape(stack).shape.size() == 1)
            return;
        InventoryShape shape = getShapeWithRotation(stack);

        int mainScreenX = mouseX - x - 8;
        int mainScreenY = mouseY - y - 8;
        InventoryPosition anchor = shape.shape.get(0);

        // Draw green tint per slot when held
        for (InventoryPosition pos : shape.shape) {
            int i = (pos.x - anchor.x) * 18 + mainScreenX;
            int j = (pos.y - anchor.y) * 18 + mainScreenY;
            boolean isMainPos = pos == anchor;
            context.fill(RenderLayer.getGuiOverlay(),
                    i - 1, j - 1, i + 17, j + 17,
                    isMainPos ? 0x8000AA00 : 0x5000AA00);
            if (!isMainPos)
                drawItem(context, stack.copyWithCount(1), i, j, null);
        }
    }

    // -----------------------------------------------------------------------
    // Highlight slots the rotated shape would occupy
    // -----------------------------------------------------------------------

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlight(Lnet/minecraft/client/gui/DrawContext;III)V", shift = At.Shift.AFTER, ordinal = 0))
    public void renderHighlightedSlots(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ItemStack stack = handler.getCursorStack();
        if (stack.isEmpty())
            return;
        InventoryShape shape = getShapeWithRotation(stack);
        for (InventoryPosition pos : shape.shape) {
            if (pos == shape.shape.get(0))
                continue;
            int i = (pos.x - shape.shape.get(0).x) * 18 + mouseX;
            int j = (pos.y - shape.shape.get(0).y) * 18 + mouseY;
            Slot slot = getSlotAt(i, j);
            if (slot == null)
                continue;
            if (!slot.canBeHighlighted())
                continue;
            HandledScreen.drawSlotHighlight(context, slot.x, slot.y, 0);
        }
    }

    // -----------------------------------------------------------------------
    // Suppress tooltip for spatial copy ghost items
    // -----------------------------------------------------------------------

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    public void getTooltipFromItem(ItemStack stack, CallbackInfoReturnable<List<Text>> cir) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null)
            return;
        if (!customData.copyNbt().getBoolean("isSpatialCopy"))
            return;
        cir.setReturnValue(List.of());
    }
}