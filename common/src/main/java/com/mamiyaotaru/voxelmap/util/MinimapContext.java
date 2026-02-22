package com.mamiyaotaru.voxelmap.util;

public class MinimapContext {
    public double playerX;
    public double playerY;
    public double playerZ;
    public float direction;
    public double zoomScale;
    public double zoomScaleAdjusted;

    public void updateVars(double playerX, double playerY, double playerZ, float direction, double zoomScale, double zoomScaleAdjusted) {
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.direction = direction;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = zoomScaleAdjusted;
    }
}
