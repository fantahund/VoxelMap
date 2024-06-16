package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class Events {

    private final MapSettingsManager mapOptions;
    private final RadarSettingsManager radarOptions;

    public Events(MapSettingsManager mapOptions, RadarSettingsManager radarOptions) {
        this.mapOptions = mapOptions;
        this.radarOptions = radarOptions;
    }

    public void initEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            radarOptions.radarAllowed = true;
            radarOptions.radarPlayersAllowed = true;
            radarOptions.radarMobsAllowed = true;
            mapOptions.cavesAllowed = true;
            mapOptions.serverTeleportCommand = null;
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> FabricModVoxelMap.instance.renderOverlay(drawContext));
    }
}
