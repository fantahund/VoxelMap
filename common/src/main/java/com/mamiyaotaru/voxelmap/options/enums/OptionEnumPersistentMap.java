package com.mamiyaotaru.voxelmap.options.enums;

public class OptionEnumPersistentMap {
    public enum ShowWaypoints implements IOptionEnum {
        OFF("options.off"),
        NEARBY("options.worldmap.showWaypoints.nearby"),
        ALL("options.worldmap.showWaypoints.all");

        private final String key;

        ShowWaypoints(String key) { this.key = key; }

        @Override public String getKey() { return key; }
    }
}
