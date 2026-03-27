package dev.mrturtle.spatial.mixin;

import dev.mrturtle.spatial.Spatial;
import dev.mrturtle.spatial.inventory.InventoryShape;
import dev.mrturtle.spatial.util.RotationUtil;
import dev.mrturtle.spatial.util.SpatialUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final public Inventory inventory;

    @Shadow @Final private int index;

    @Shadow public abstract int getMaxItemCount();

    @Unique
    private static boolean isSpatialCopy(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null)
            return false;
        return customData.copyNbt().getBoolean("isSpatialCopy");
    }

    @Unique
    private static InventoryShape getRotatedShape(ItemStack stack) {
        InventoryShape base = Spatial.getShape(stack);
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null)
            return base;
        int rotation = customData.copyNbt().getInt("spatialRotation");
        return RotationUtil.applyRotation(base, rotation);
    }

    @Inject(method = "canInsert", at = @At("RETURN"), cancellable = true)
    public void canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue())
            return;
        if (isSpatialCopy(stack))
            return;
        if (SpatialUtil.isInvalidInventory(inventory))
            return;
        if (inventory instanceof PlayerInventory)
            if (index <= 8 || index == 40)
                return;
        InventoryShape shape = getRotatedShape(stack);
        cir.setReturnValue(shape.canPlaceAt(inventory, index));
    }

    @Inject(method = "setStack(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    public void setStack(ItemStack stack, ItemStack previousStack, CallbackInfo ci) {
        if (previousStack.isEmpty())
            return;
        if (isSpatialCopy(previousStack))
            return;
        if (SpatialUtil.isInvalidInventory(inventory))
            return;
        if (inventory instanceof PlayerInventory) {
            if (index <= 8 || index == 40)
                return;
            if (getMaxItemCount() == 1)
                return;
        }
        InventoryShape shape = getRotatedShape(previousStack); // was Spatial.getShape()
        shape.removeAt(inventory, index);
    }

    @Inject(method = "setStackNoCallbacks", at = @At("RETURN"))
    public void setStackNoCallbacks(ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty())
            return;
        if (isSpatialCopy(stack))
            return;
        if (SpatialUtil.isInvalidInventory(inventory))
            return;
        if (inventory instanceof PlayerInventory) {
            if (index <= 8 || index == 40)
                return;
            if (getMaxItemCount() == 1)
                return;
        }
        InventoryShape shape = getRotatedShape(stack);
        shape.placeAt(inventory, index, stack);
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"))
    public void insertStack(ItemStack stack, int count, CallbackInfoReturnable<ItemStack> cir) {
        if (stack == cir.getReturnValue())
            return;
        if (isSpatialCopy(stack))
            return;
        if (SpatialUtil.isInvalidInventory(inventory))
            return;
        InventoryShape shape = getRotatedShape(stack);
        shape.placeAt(inventory, index, stack);
    }

    @Inject(method = "takeStack", at = @At("HEAD"), cancellable = true)
    public void takeStack(int amount, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = inventory.getStack(index);
        if (!isSpatialCopy(stack))
            return;
        if (SpatialUtil.isInvalidInventory(inventory))
            return;
        cir.setReturnValue(ItemStack.EMPTY);
    }

    @Redirect(method = "canTakePartial", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;canInsert(Lnet/minecraft/item/ItemStack;)Z"))
    public boolean canInsertRedirect(Slot instance, ItemStack stack) {
        return true;
    }
}