package com.mamiyaotaru.voxelmap.options.enums;

public class OptionEnumWaypoint {
    public enum Deathpoints implements IOptionEnum {
        OFF("options.off"),
        MOST_RECENT("options.minimap.waypoints.deathpoints.mostRecent"),
        ALL("options.minimap.waypoints.deathpoints.all");

        private final String key;

        Deathpoints(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum UnitConversion implements IOptionEnum {
        OFF("options.off"),
        FROM_1000M("options.minimap.waypoints.unitConversion.from1000m"),
        FROM_10000M("options.minimap.waypoints.unitConversion.from10000m");

        private final String key;

        UnitConversion(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum LabelStyle implements IOptionEnum {
        OFF("options.off"),
        DEFAULT("options.minimap.waypoints.labelStyle.default"),
        CLASSIC_TOP("options.minimap.waypoints.labelStyle.classic1"),
        CLASSIC_BOTTOM("options.minimap.waypoints.labelStyle.classic2");

        private final String key;

        LabelStyle(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }
}
