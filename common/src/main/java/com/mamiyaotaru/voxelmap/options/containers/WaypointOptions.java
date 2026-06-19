package com.mamiyaotaru.voxelmap.options.containers;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.options.ServerSettingsManager;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumMinimap;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumWaypoint;
import com.mamiyaotaru.voxelmap.options.fields.BooleanField;
import com.mamiyaotaru.voxelmap.options.fields.EnumField;
import com.mamiyaotaru.voxelmap.options.fields.FloatField;
import com.mamiyaotaru.voxelmap.options.fields.IntegerField;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class WaypointOptions extends AbstractOptionsContainer {
    public final IntegerField waypointSort;
    public final IntegerField maxDistance;
    public final FloatField signScale;
    public final EnumField<OptionEnumWaypoint.Deathpoints> deathpoints;
    public final EnumField<OptionEnumWaypoint.UnitConversion> unitConversion;
    public final EnumField<OptionEnumWaypoint.LabelStyle> labelStyle;
    public final BooleanField highlightFocused;

    public WaypointOptions() {
        addOptionField((waypointSort = new IntegerField("Waypoint Sort By", "", 1, -4, 4)));
        addOptionField((maxDistance = new IntegerField("Waypoint Max Distance", "options.minimap.waypoints.distance", 10001, 50, 10001)).withFormat(this::formatMaxDistance));
        addOptionField((signScale = new FloatField("Waypoint Sign Scale", "options.minimap.waypoints.signScale", 1.0F, 0.5F, 1.5F, 0.01F)).withFormat(this::formatSignScale));
        addOptionField((deathpoints = new EnumField<>("Deathpoints", "options.minimap.waypoints.deathpoints", OptionEnumWaypoint.Deathpoints.MOST_RECENT)).withTooltip(this::buildDeathpointTooltip));
        addOptionField((unitConversion = new EnumField<>("Waypoint Unit Conversion", "options.minimap.waypoints.unitConversion", OptionEnumWaypoint.UnitConversion.FROM_1000M)).withTooltip(this::buildUnitConversionTooltip));
        addOptionField((labelStyle = new EnumField<>("Waypoint Label Style", "options.minimap.waypoints.labelStyle", OptionEnumWaypoint.LabelStyle.DEFAULT)));
        addOptionField((highlightFocused = new BooleanField("Highlight Focused Waypoint", "options.minimap.waypoints.highlightFocused", true)));
    }

    @Override
    public void updateOptionsActive() {
        boolean signsEnabled = VoxelConstants.getVoxelMapInstance().getMapOptions().inGameWaypoints.get() == OptionEnumMinimap.InGameWaypoints.SIGNS || VoxelConstants.getVoxelMapInstance().getMapOptions().inGameWaypoints.get() == OptionEnumMinimap.InGameWaypoints.BOTH;

        OptionField<?>[] signOptions = new OptionField[]{signScale, unitConversion, labelStyle, highlightFocused};
        for (OptionField<?> option : signOptions) {
            option.setActive(signsEnabled);
        }
    }

    @Override
    public void updateOptionsAllowed(ServerSettingsManager serverSettings) {
        boolean waypointsAllowed = serverSettings.waypointsAllowed.get();
        for (OptionField<?> option : optionByNames.values()) {
            option.setAllowed(waypointsAllowed);
        }

        deathpoints.setAllowed(deathpoints.isAllowed() && serverSettings.deathpointsAllowd.get());
    }

    public void setSort(int sort) {
        int cur = waypointSort.get();
        if (sort != cur && sort != -cur) {
            waypointSort.set(sort);
        } else {
            waypointSort.set(-cur);
        }
    }

    // Option Attributes

    private String formatMaxDistance(Integer value) {
        return value > 10000 ? I18n.get("options.minimap.waypoints.infinite") : String.format("%sm", value);
    }

    private String formatSignScale(Float value) {
        return String.format("%.2fx", value);
    }

    private Tooltip buildDeathpointTooltip(OptionEnumWaypoint.Deathpoints value) {
        return Tooltip.create(Component.translatable("options.minimap.waypoints.deathpoints.tooltip"));
    }

    private Tooltip buildUnitConversionTooltip(OptionEnumWaypoint.UnitConversion value) {
        return Tooltip.create(Component.translatable("options.minimap.waypoints.unitConversion.tooltip"));
    }
}
