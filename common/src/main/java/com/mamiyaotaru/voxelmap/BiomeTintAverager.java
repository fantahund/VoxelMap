package com.mamiyaotaru.voxelmap;

import java.util.function.IntBinaryOperator;

final class BiomeTintAverager {
    private BiomeTintAverager() {}

    static int average3x3(int centerX, int centerZ, IntBinaryOperator colorAt) {
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int x = centerX - 1; x <= centerX + 1; ++x) {
            for (int z = centerZ - 1; z <= centerZ + 1; ++z) {
                int color = colorAt.applyAsInt(x, z);
                red += color >> 16 & 0xFF;
                green += color >> 8 & 0xFF;
                blue += color & 0xFF;
            }
        }
        return (red / 9 & 0xFF) << 16 | (green / 9 & 0xFF) << 8 | blue / 9 & 0xFF;
    }
}
