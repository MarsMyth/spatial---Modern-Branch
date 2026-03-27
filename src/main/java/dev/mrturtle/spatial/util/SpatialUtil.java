package dev.mrturtle.spatial.util;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.Map;

public class SpatialUtil {
    private static final Map<String, String> ITEM_TEXTURES = Map.ofEntries(
            Map.entry("minecraft:chest", "chest"),
            Map.entry("minecraft:trapped_chest", "chest"),
            Map.entry("minecraft:fishing_rod", "rods"),
            Map.entry("minecraft:carrot_on_a_stick", "rods"),
            Map.entry("minecraft:warped_fungus_on_a_stick", "rods")
    );

    public static Identifier getSlotTexture(ItemStack stack) {
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String textureName = ITEM_TEXTURES.get(itemId);
        if (textureName == null)
            return null;
        return Identifier.of("spatial", "slots/" + textureName);
//        return Identifier.of("spatial", "slots/normal_slot");
    }

    public static int colorFromItemStack(ItemStack stack) {
        return colorFromItemStack(stack, false);
    }

    public static int colorFromItemStack(ItemStack stack, boolean isMainStack) {
        float hue = (float) stack.getItem().getName().hashCode() / Integer.MAX_VALUE;
        return Color.HSBtoRGB(hue, isMainStack ? 1.0f : 0.75f, isMainStack ? 0.75f : 0.65f);
    }

    public static boolean isInvalidInventory(Inventory inventory) {
        if (inventory instanceof PlayerInventory)
            return false;
        if (inventory instanceof ChestBlockEntity)
            return false;
        if (inventory instanceof DoubleInventory)
            return false;
        if (inventory instanceof EnderChestInventory)
            return false;
        if (inventory instanceof BarrelBlockEntity)
            return false;
        if (inventory instanceof ShulkerBoxBlockEntity)
            return false;
        if (inventory instanceof DispenserBlockEntity)
            return false;
        if (inventory instanceof VehicleInventory)
            return false;
        return true;
    }
}