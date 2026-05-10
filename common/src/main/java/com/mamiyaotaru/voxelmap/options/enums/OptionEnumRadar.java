package com.mamiyaotaru.voxelmap.options.enums;

public class OptionEnumRadar {
    public enum RadarMode implements IOptionEnum {
        SIMPLE("options.minimap.radar.radarMode.simple"),
        FULL("options.minimap.radar.radarMode.full");

        private final String key;

        RadarMode(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }

    public enum ShowMobs implements IOptionEnum {
        OFF("options.off"),
        HOSTILES("options.minimap.radar.showMobs.showHostiles"),
        NEUTRALS("options.minimap.radar.showMobs.showNeutrals"),
        BOTH("options.minimap.radar.showMobs.showAll");

        private final String key;

        ShowMobs(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }
}
