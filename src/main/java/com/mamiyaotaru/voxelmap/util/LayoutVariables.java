package com.mamiyaotaru.voxelmap.util;

public class LayoutVariables {
    public int scScale = 0;
    public int mapX = 0;
    public int mapY = 0;
    public double zoomScale = 0.0;
    public double zoomScaleAdjusted = 0.0;

    public void updateVars(int scScale, int mapX, int mapY, double zoomScale, double zoomScaleAdjusted) {
        this.scScale = scScale;
        this.mapX = mapX;
        this.mapY = mapY;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = zoomScaleAdjusted;
    }
}
