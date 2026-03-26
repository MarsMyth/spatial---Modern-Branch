package dev.mrturtle.spatial.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    public void dropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null)
            return;
        if (!customData.copyNbt().getBoolean("isSpatialCopy"))
            return;
        cir.setReturnValue(null);
    }

    @Inject(method = "changeGameMode", at = @At("HEAD"))
    public void onChangeGameMode(GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (gameMode != GameMode.CREATIVE)
            return;
        // Remove all spatial copy ghost items from the inventory before creative mode
        // bypasses the normal removeAt() guard
        DefaultedList<ItemStack> main = this.getInventory().main;
        for (int i = 0; i < main.size(); i++) {
            ItemStack stack = main.get(i);
            if (stack.isEmpty())
                continue;
            NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData == null)
                continue;
            NbtCompound nbt = customData.copyNbt();
            if (!nbt.getBoolean("isSpatialCopy"))
                continue;
            main.set(i, ItemStack.EMPTY);
        }
        this.playerScreenHandler.updateToClient();
    }
}