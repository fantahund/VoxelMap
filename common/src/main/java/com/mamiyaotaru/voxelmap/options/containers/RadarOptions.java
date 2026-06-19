package com.mamiyaotaru.voxelmap.options.containers;

import com.mamiyaotaru.voxelmap.gui.GuiOptionsScreenMinimap;
import com.mamiyaotaru.voxelmap.options.ServerSettingsManager;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumRadar;
import com.mamiyaotaru.voxelmap.options.fields.BooleanField;
import com.mamiyaotaru.voxelmap.options.fields.EnumField;
import com.mamiyaotaru.voxelmap.options.fields.FloatField;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.io.PrintWriter;
import java.util.HashSet;

public class RadarOptions extends AbstractOptionsContainer {
    public final EnumField<OptionEnumRadar.RadarMode> radarMode;
    public final BooleanField showRadar;
    public final EnumField<OptionEnumRadar.ShowMobs> showMobs;
    public final BooleanField showMobNames;
    public final BooleanField showMobHelmets;
    public final BooleanField showPlayers;
    public final BooleanField showPlayerNames;
    public final BooleanField showPlayerHelmets;
    public final BooleanField filtering;
    public final BooleanField outlines;
    public final BooleanField showFacing;
    public final BooleanField showFullNames;
    public final BooleanField showElevation;
    public final BooleanField hideSneaking;
    public final BooleanField hideInvisible;
    public final FloatField fontScale;
    public final HashSet<Identifier> hiddenMobs = new HashSet<>();

    public RadarOptions() {
        addOptionField((radarMode = new EnumField<>("Radar Mode", "options.minimap.radar.radarMode", OptionEnumRadar.RadarMode.FULL)).withListener(this::updateRadarMode));
        addOptionField((showRadar = new BooleanField("Show Radar", "options.minimap.radar.showRadar", true)));
        addOptionField((showMobs = new EnumField<>("Show Mobs", "options.minimap.radar.showMobs", OptionEnumRadar.ShowMobs.HOSTILES)));
        addOptionField((showMobNames = new BooleanField("Show Mob Names", "options.minimap.radar.showMobNames", true)));
        addOptionField((showMobHelmets = new BooleanField("Show Mob Helmets", "options.minimap.radar.showMobHelmets", true)));
        addOptionField((showPlayers = new BooleanField("Show Players", "options.minimap.radar.showPlayers", true)));
        addOptionField((showPlayerNames = new BooleanField("Show Player Names", "options.minimap.radar.showPlayerNames", true)));
        addOptionField((showPlayerHelmets = new BooleanField("Show Player Helmets", "options.minimap.radar.showPlayerHelmets", true)));
        addOptionField((filtering = new BooleanField("Radar Filtering", "options.minimap.radar.iconFiltering", true)));
        addOptionField((outlines = new BooleanField("Radar Outlines", "options.minimap.radar.iconOutlines", true)));
        addOptionField((showFacing = new BooleanField("Show Facing", "options.minimap.radar.showFacing", true)));
        addOptionField((showFullNames = new BooleanField("Show Full Entity Names", "options.minimap.radar.showFullNames", false)));
        addOptionField((showElevation = new BooleanField("Show Entity Elevation", "options.minimap.radar.showElevation", true)));
        addOptionField((hideSneaking = new BooleanField("Hide Sneaking Players", "options.minimap.radar.hideSneaking", true)));
        addOptionField((hideInvisible = new BooleanField("Hide Invisible Entities", "options.minimap.radar.hideInvisible", true)));
        addOptionField((fontScale = new FloatField("Radar Font Scale", "", 1.0F, 0.0F, 2.0F)));
    }

    @Override
    public void updateOptionsActive() {
        boolean radarEnabled = showRadar.get();
        for (OptionField<?> option : optionByNames.values()) {
            if (option != showRadar) {
                option.setActive(radarEnabled);
            }
        }

        boolean mobsEnabled = showMobs.get() != OptionEnumRadar.ShowMobs.OFF;
        showMobNames.setActive(showMobNames.isActive() && mobsEnabled);
        showMobHelmets.setActive(showMobHelmets.isActive() && mobsEnabled);

        boolean playersEnabled = showPlayers.get();
        showPlayerNames.setActive(showPlayerNames.isActive() && playersEnabled);
        showPlayerHelmets.setActive(showPlayerHelmets.isActive() && playersEnabled);
    }

    @Override
    public void updateOptionsAllowed(ServerSettingsManager serverSettings) {
        boolean radarAllowed = serverSettings.radarAllowed.get() && (serverSettings.radarMobsAllowed.get() || serverSettings.radarPlayersAllowed.get());
        for (OptionField<?> option : optionByNames.values()) {
            option.setAllowed(radarAllowed);
        }

        boolean mobsAllowed = serverSettings.radarAllowed.get() && serverSettings.radarMobsAllowed.get();
        showMobs.setAllowed(showMobs.isAllowed() && mobsAllowed);
        showMobNames.setAllowed(showMobNames.isAllowed() && mobsAllowed);
        showMobHelmets.setAllowed(showMobHelmets.isAllowed() && mobsAllowed);

        boolean playersAllowed = serverSettings.radarAllowed.get() && serverSettings.radarPlayersAllowed.get();
        showPlayers.setAllowed(showPlayers.isAllowed() && playersAllowed);
        showPlayerNames.setAllowed(showPlayerNames.isAllowed() && playersAllowed);
        showPlayerHelmets.setAllowed(showPlayerHelmets.isAllowed() && playersAllowed);
    }

    @Override
    public void loadLine(String[] keyValue) {
        super.loadLine(keyValue);

        if (keyValue[0].equals("Hidden Mobs")) {
            applyHiddenMobs(keyValue[1]);
        }
    }

    @Override
    public void saveAll(PrintWriter out) {
        super.saveAll(out);

        out.print("Hidden Mobs:");
        for (Identifier mob : hiddenMobs) {
            out.print(mob.toString() + ",");
        }
        out.println();
    }

    private void applyHiddenMobs(String hiddenMobs) {
        String[] mobsToHide = hiddenMobs.split(",");
        this.hiddenMobs.clear();
        for (String s : mobsToHide) {
            Identifier.read(s).ifSuccess(this.hiddenMobs::add);
        }
    }

    public boolean isMobEnabled(Entity entity) {
        return isMobEnabled(entity.getType());
    }

    public boolean isMobEnabled(EntityType<?> type) {
        return !hiddenMobs.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    // Option Attributes

    private void updateRadarMode(OptionEnumRadar.RadarMode value) {
        if (Minecraft.getInstance().screen instanceof GuiOptionsScreenMinimap screen) {
            screen.rebuildOptionWidgets();
        }
    }
}
