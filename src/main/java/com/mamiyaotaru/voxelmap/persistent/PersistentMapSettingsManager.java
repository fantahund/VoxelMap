package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import net.minecraft.client.resource.language.I18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class PersistentMapSettingsManager implements ISubSettingsManager {
    protected int mapX;
    protected int mapZ;
    protected float zoom = 4.0F;
    private float minZoomPower = -1.0F;
    private float maxZoomPower = 4.0F;
    protected float minZoom = 0.5F;
    protected float maxZoom = 16.0F;
    protected int cacheSize = 500;
    protected boolean outputImages;
    public boolean showWaypoints = true;
    public boolean showWaypointNames = true;

    @Override
    public void loadSettings(File settingsFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(settingsFile));

            String sCurrentLine;
            while ((sCurrentLine = in.readLine()) != null) {
                String[] curLine = sCurrentLine.split(":");
                switch (curLine[0]) {
                    case "Worldmap Zoom" -> this.zoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Minimum Zoom" -> this.minZoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Maximum Zoom" -> this.maxZoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Cache Size" -> this.cacheSize = Integer.parseInt(curLine[1]);
                    case "Show Worldmap Waypoints" -> this.showWaypoints = Boolean.parseBoolean(curLine[1]);
                    case "Show Worldmap Waypoint Names" -> this.showWaypointNames = Boolean.parseBoolean(curLine[1]);
                    case "Output Images" -> this.outputImages = Boolean.parseBoolean(curLine[1]);
                }
            }

            in.close();
        } catch (Exception ignored) {}

        for (int power = -3; power <= 5; ++power) {
            if (Math.pow(2.0, power) == this.minZoom) {
                this.minZoomPower = power;
            }

            if (Math.pow(2.0, power) == this.maxZoom) {
                this.maxZoomPower = power;
            }
        }

        this.bindCacheSize();
        this.bindZoom();
    }

    @Override
    public void saveAll(PrintWriter out) {
        out.println("Worldmap Zoom:" + this.zoom);
        out.println("Worldmap Minimum Zoom:" + this.minZoom);
        out.println("Worldmap Maximum Zoom:" + this.maxZoom);
        out.println("Worldmap Cache Size:" + this.cacheSize);
        out.println("Show Worldmap Waypoints:" + this.showWaypoints);
        out.println("Show Worldmap Waypoint Names:" + this.showWaypointNames);
    }

    @Override
    public String getKeyText(EnumOptionsMinimap options) {
        String s = I18n.translate(options.getName()) + ": ";
        if (options.isFloat()) {
            float f = this.getOptionFloatValue(options);
            if (options == EnumOptionsMinimap.MINZOOM) {
                return s + (float) Math.pow(2.0, f) + "x";
            }

            if (options == EnumOptionsMinimap.MAXZOOM) {
                return s + (float) Math.pow(2.0, f) + "x";
            }

            if (options == EnumOptionsMinimap.CACHESIZE) {
                return s + (int) f;
            }
        }

        if (options.isBoolean()) {
            boolean flag = this.getOptionBooleanValue(options);
            return flag ? s + I18n.translate("options.on") : s + I18n.translate("options.off");
        } else {
            return s;
        }
    }

    @Override
    public float getOptionFloatValue(EnumOptionsMinimap options) {
        if (options == EnumOptionsMinimap.MINZOOM) {
            return this.minZoomPower;
        } else if (options == EnumOptionsMinimap.MAXZOOM) {
            return this.maxZoomPower;
        } else {
            return options == EnumOptionsMinimap.CACHESIZE ? this.cacheSize : 0.0F;
        }
    }

    public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions) {
        return switch (par1EnumOptions) {
            case SHOWWAYPOINTS -> this.showWaypoints;
            case SHOWWAYPOINTNAMES -> this.showWaypointNames;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean)");
        };
    }

    @Override
    public void setOptionFloatValue(EnumOptionsMinimap options, float value) {
        if (options == EnumOptionsMinimap.MINZOOM) {
            this.minZoomPower = ((int) (value * 8.0F) - 3);
            this.minZoom = (float) Math.pow(2.0, this.minZoomPower);
            if (this.maxZoom < this.minZoom) {
                this.maxZoom = this.minZoom;
                this.maxZoomPower = this.minZoomPower;
            }
        } else if (options == EnumOptionsMinimap.MAXZOOM) {
            this.maxZoomPower = ((int) (value * 8.0F) - 3);
            this.maxZoom = (float) Math.pow(2.0, this.maxZoomPower);
            if (this.minZoom > this.maxZoom) {
                this.minZoom = this.maxZoom;
                this.minZoomPower = this.maxZoomPower;
            }
        } else if (options == EnumOptionsMinimap.CACHESIZE) {
            this.cacheSize = (int) (value * 5000.0F);
            this.cacheSize = Math.max(this.cacheSize, 30);

            for (int minCacheSize = (int) ((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F); this.cacheSize < minCacheSize; minCacheSize = (int) ((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F)) {
                ++this.minZoomPower;
                this.minZoom = (float) Math.pow(2.0, this.minZoomPower);
            }

            if (this.maxZoom < this.minZoom) {
                this.maxZoom = this.minZoom;
                this.maxZoomPower = this.minZoomPower;
            }
        }

        this.bindZoom();
        this.bindCacheSize();
    }

    public void setOptionValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case SHOWWAYPOINTS -> this.showWaypoints = !this.showWaypoints;
            case SHOWWAYPOINTNAMES -> this.showWaypointNames = !this.showWaypointNames;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName());
        }

    }

    private void bindCacheSize() {
        int minCacheSize = (int) ((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F);
        this.cacheSize = Math.max(this.cacheSize, minCacheSize);
    }

    private void bindZoom() {
        this.zoom = Math.max(this.zoom, this.minZoom);
        this.zoom = Math.min(this.zoom, this.maxZoom);
    }
}
