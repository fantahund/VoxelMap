package com.mamiyaotaru.voxelmap.persistent.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.widgets.IOptionWidget;
import com.mamiyaotaru.voxelmap.options.fields.BooleanField;
import com.mamiyaotaru.voxelmap.options.fields.IntegerField;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;

public class CaveModeOverlay extends PersistentMapOverlay.OverlayElement {
    private final PersistentMap map;
    private final ClientLevel world;
    private final int autoLayer;
    private final int surfaceLayer;
    private final BooleanField caveModeField;
    private final AbstractWidget caveModeWidget;
    private final IntegerField caveLayerField;
    private final AbstractWidget caveLayerWidget;

    public CaveModeOverlay(PersistentMap map, int x, int y, int width, int height) {
        super(map, x, y, width, height);

        this.map = map;
        world = VoxelConstants.getClientWorld();
        int minLayer = PersistentMap.getMinCaveLayer(world);
        int maxLayer = PersistentMap.getMaxCaveLayer(world);
        autoLayer = maxLayer + 2;
        surfaceLayer = maxLayer + 1;

        caveModeField = map.getOptions().showCaves;
        caveModeWidget = caveModeField.createWidget(x + width / 2 - 50, y + 5, 100, 20, this::caveModeUpdated);
        addWidget(caveModeWidget);

        (caveLayerField = new IntegerField("WorldMap Cave Layer", "options.worldmap.caveMode.layer", getLayerValue(), minLayer, maxLayer + 2)).withListener(this::updateCaveLayer).withFormat(this::formatLayerRange);
        caveLayerWidget = caveLayerField.createWidget(x + width / 2 - 50, y + 30, 100, 20, (e) -> {});
        addWidget(caveLayerWidget);

        caveModeUpdated(caveModeField.get());
    }

    private int getLayerValue() {
        if (map.isAutoCaveEnabled()) {
            return autoLayer;
        }
        if (!map.isUnderground()) {
            return surfaceLayer;
        }
        return map.getCaveLayer();
    }

    private void caveModeUpdated(boolean value) {
        boolean allowed = VoxelConstants.getVoxelMapInstance().getServerSettings().manualCavesAllowed.get();
        caveLayerField.setActive(value);
        caveLayerField.setAllowed(allowed);
        if (!value || !allowed) {
            caveLayerField.set(autoLayer);
        }
        ((IOptionWidget) caveModeWidget).refresh();
        ((IOptionWidget) caveLayerWidget).refresh();
    }

    private void updateCaveLayer(int value) {
        if (value == autoLayer) {
            map.enableAutoCave();
        } else if (value == surfaceLayer) {
            map.setCaveLayer(0, false);
        } else {
            map.setCaveLayer(value, true);
        }
    }

    private String formatLayerRange(int value) {
        if (value == autoLayer) {
            return I18n.get("options.worldmap.caveMode.layer.auto");
        }
        if (value == surfaceLayer) {
            return I18n.get("options.worldmap.caveMode.layer.surface");
        }
        int blockMin = PersistentMap.caveLayerToBlock(world, value);
        int blockMax = blockMin + PersistentMap.CAVE_LAYER_HEIGHT;
        return I18n.get("options.worldmap.caveMode.layer.range", blockMin, blockMax);
    }
}
