package com.mamiyaotaru.voxelmap.options.enums;

public class OptionEnumMinimap {
    public enum ColorPickerMode implements IOptionEnum {
        SIMPLE("options.minimap.colorPickerMode.simple"),
        FULL("options.minimap.colorPickerMode.full");

        private final String key;

        ColorPickerMode(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum CoordInfo implements IOptionEnum {
        OFF("options.off"),
        DEFAULT("options.minimap.showCoordinates.default"),
        CLASSIC("options.minimap.showCoordinates.classic");

        private final String key;

        CoordInfo(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum Location implements IOptionEnum {
        TOP_LEFT("options.minimap.location.topLeft"),
        TOP_RIGHT("options.minimap.location.topRight"),
        BOTTOM_RIGHT("options.minimap.location.bottomRight"),
        BOTTOM_LEFT("options.minimap.location.bottomLeft");

        private final String key;

        Location(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum Size implements IOptionEnum {
        SMALL("options.minimap.size.small"),
        MEDIUM("options.minimap.size.medium"),
        LARGE("options.minimap.size.large"),
        XL("options.minimap.size.xl"),
        XXL("options.minimap.size.xxl"),
        XXXL("options.minimap.size.xxxl");

        private final String key;

        Size(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum InGameWaypoints implements IOptionEnum {
        OFF("options.off"),
        BEACONS("options.minimap.inGameWaypoints.beacons"),
        SIGNS("options.minimap.inGameWaypoints.signs"),
        BOTH("options.minimap.inGameWaypoints.both");

        private final String key;

        InGameWaypoints(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum TerrainDepth implements IOptionEnum {
        OFF("options.off"),
        HEIGHT_MAP("options.minimap.terrain.height"),
        SLOPE_MAP("options.minimap.terrain.slope"),
        BOTH("options.minimap.terrain.both");

        private final String key;

        TerrainDepth(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum BiomeOverlay implements IOptionEnum {
        OFF("options.off"),
        SOLID("options.minimap.biomeOverlay.transparent"),
        TRANSPARENT("options.minimap.biomeOverlay.solid");

        private final String key;

        BiomeOverlay(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }
}
