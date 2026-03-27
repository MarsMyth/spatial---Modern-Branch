package dev.mrturtle.spatial.util;

import dev.mrturtle.spatial.inventory.InventoryPosition;
import dev.mrturtle.spatial.inventory.InventoryShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RotationUtil {
    public static InventoryShape rotate(InventoryShape shape, boolean clockwise) {
        List<InventoryPosition> rotated = new ArrayList<>();

        int maxX = shape.shape.stream().mapToInt(p -> p.x).max().orElse(0);
        int maxY = shape.shape.stream().mapToInt(p -> p.y).max().orElse(0);

        for (InventoryPosition pos : shape.shape) {
            if (clockwise)
                rotated.add(new InventoryPosition(maxY - pos.y, pos.x));
            else
                rotated.add(new InventoryPosition(pos.y, maxX - pos.x));
        }

        // Normalise so min x and min y are both 0
        int minX = rotated.stream().mapToInt(p -> p.x).min().orElse(0);
        int minY = rotated.stream().mapToInt(p -> p.y).min().orElse(0);

        List<InventoryPosition> normalised = new ArrayList<>();
        for (InventoryPosition pos : rotated)
            normalised.add(new InventoryPosition(pos.x - minX, pos.y - minY));

        // Sort so shape.get(0) is always top-left (min y first, then min x)
        normalised.sort(Comparator.comparingInt((InventoryPosition p) -> p.y)
                .thenComparingInt(p -> p.x));

        int newWidth  = normalised.stream().mapToInt(p -> p.x).max().orElse(0) + 1;
        int newHeight = normalised.stream().mapToInt(p -> p.y).max().orElse(0) + 1;
        return new InventoryShape(normalised, newWidth, newHeight);
    }

    public static InventoryShape applyRotation(InventoryShape base, int rotation) {
        InventoryShape result = base;
        for (int i = 0; i < (rotation % 4); i++)
            result = rotate(result, true);
        return result;
    }
}