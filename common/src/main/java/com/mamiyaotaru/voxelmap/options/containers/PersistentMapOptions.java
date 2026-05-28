package com.mamiyaotaru.voxelmap.options.containers;

import com.mamiyaotaru.voxelmap.options.ServerSettingsManager;
import com.mamiyaotaru.voxelmap.options.fields.BooleanField;
import com.mamiyaotaru.voxelmap.options.fields.FloatField;
import com.mamiyaotaru.voxelmap.options.fields.IntegerField;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;

public class PersistentMapOptions extends AbstractOptionsContainer {
    private static final int MIN_ZOOM_EXP = -3;
    private static final int MAX_ZOOM_EXP = 5;

    public final BooleanField showCoordinates;
    public final BooleanField showCaves;
    public final BooleanField showWaypoints;
    public final BooleanField showWaypointNames;
    public final BooleanField showDistantWaypoints;
    public final BooleanField outputImages;

    public final IntegerField cacheSize;
    public final FloatField zoomExponent;
    public final FloatField minZoomExponent;
    public final FloatField maxZoomExponent;

    private boolean skipNextZoomAndCacheUpdate;

    public int mapX;
    public int mapZ;

    private float zoom;
    private float minZoom;
    private float maxZoom;

    public PersistentMapOptions() {
        addOptionField((showCoordinates = new BooleanField("Show WorldMap Coordinates", "options.worldmap.showCoordinates", true)));
        addOptionField((showCaves = new BooleanField("Enable WorldMap Cave Mode", "options.worldmap.caveMode", true)));
        addOptionField((showWaypoints = new BooleanField("Show WorldMap Waypoints", "options.worldmap.showWaypoints", true)));
        addOptionField((showWaypointNames = new BooleanField("Show WorldMap Waypoint Names", "options.worldmap.showWaypointNames", true)));
        addOptionField((showDistantWaypoints = new BooleanField("Show WorldMap Distant Waypoints", "options.worldmap.showDistantWaypoints", true)));
        addOptionField((outputImages = new BooleanField("WorldMap Output Images", "", false)));

        addOptionField((cacheSize = new IntegerField("WorldMap Cache Size", "options.worldmap.cacheSize", 500, 30, 5000)).withListener(this::updateCacheSize));
        addOptionField((zoomExponent = new FloatField("WorldMap Zoom", "", 2.0F, MIN_ZOOM_EXP, MAX_ZOOM_EXP)).withListener(this::updateZoom));
        addOptionField((minZoomExponent = new FloatField("WorldMap Minimum Zoom", "options.worldmap.minZoom", -1.0F, MIN_ZOOM_EXP, MAX_ZOOM_EXP, 1.0F)).withListener(this::updateMinZoom).withFormat(this::formatZoomAmount));
        addOptionField((maxZoomExponent = new FloatField("WorldMap Maximum Zoom", "options.worldmap.maxZoom", 4.0F, MIN_ZOOM_EXP, MAX_ZOOM_EXP, 1.0F)).withListener(this::updateMaxZoom).withFormat(this::formatZoomAmount));
    }

    @Override
    public void loadLine(String[] keyValue) {
        super.loadLine(keyValue);

        zoom = (float) Math.pow(2.0, zoomExponent.get());
        minZoom = (float) Math.pow(2.0, minZoomExponent.get());
        maxZoom = (float) Math.pow(2.0, maxZoomExponent.get());
    }

    @Override
    public void updateOptionsActive() {
        boolean waypointEnabled = showWaypoints.get();
        showWaypointNames.setActive(waypointEnabled);
        showDistantWaypoints.setActive(waypointEnabled);
    }

    @Override
    public void updateOptionsAllowed(ServerSettingsManager serverSettings) {
        boolean worldMapAllowed = serverSettings.worldmapAllowed.get();
        for (OptionField<?> option : optionByNames.values()) {
            option.setAllowed(worldMapAllowed);
        }

        boolean waypointsAllowed = serverSettings.waypointsAllowed.get();
        showWaypoints.setAllowed(showWaypoints.isAllowed() && waypointsAllowed);
        showWaypointNames.setAllowed(showWaypointNames.isAllowed() && waypointsAllowed);
        showDistantWaypoints.setAllowed(showDistantWaypoints.isAllowed() && waypointsAllowed);
    }

    public float getZoom()  {
        return zoom;
    }

    public float getMinZoom() {
        return minZoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    // Option Attributes

    private String formatZoomAmount(Float value) {
        return String.format("%sx", Math.pow(2.0, value));
    }

    private int calculateMinCacheSize() {
        double amount = Math.pow(2.0, minZoomExponent.get());
        return (int) ((1600.0 / amount / 256.0 + 4.0) * (1100.0 / amount / 256.0 + 3.0));
    }

    private void updateCacheSize(Integer value) {
        updateZoomAndCache(cacheSize);
    }

    private void updateMinZoom(Float value) {
        updateZoomAndCache(minZoomExponent);
    }

    private void updateMaxZoom(Float value) {
        updateZoomAndCache(maxZoomExponent);
    }

    private void updateZoom(Float value) {
        zoom = (float) Math.pow(2.0, value);
    }

    private void updateZoomAndCache(OptionField<?> changedField) {
        if (skipNextZoomAndCacheUpdate) {
            return;
        }
        skipNextZoomAndCacheUpdate = true;

        try {
            if (changedField == cacheSize) {
                while (cacheSize.get() < calculateMinCacheSize()) {
                    minZoomExponent.set(minZoomExponent.get() + 1.0F);
                }
                if (maxZoomExponent.get() < minZoomExponent.get()) {
                    maxZoomExponent.set(minZoomExponent.get());
                }
            } else if (changedField == minZoomExponent) {
                if (maxZoomExponent.get() < minZoomExponent.get()) {
                    maxZoomExponent.set(minZoomExponent.get());
                }
            } else if (changedField == maxZoomExponent) {
                if (minZoomExponent.get() > maxZoomExponent.get()) {
                    minZoomExponent.set(maxZoomExponent.get());
                }
            }

            minZoom = (float) Math.pow(2.0, minZoomExponent.get());
            maxZoom = (float) Math.pow(2.0, maxZoomExponent.get());
            zoomExponent.set(Math.min(Math.max(zoomExponent.get(), minZoomExponent.get()), maxZoomExponent.get()));

            cacheSize.set(Math.max(cacheSize.get(), calculateMinCacheSize()));

        } finally {
            skipNextZoomAndCacheUpdate = false;
        }
    }
}
