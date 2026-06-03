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
    private final ClientLevel world;
    private final int minLayer;
    private final int maxLayer;
    private final BooleanField caveModeField;
    private final AbstractWidget caveModeWidget;
    private final IntegerField caveLayerField;
    private final AbstractWidget caveLayerWidget;

    public CaveModeOverlay(PersistentMap map, int x, int y, int width, int height) {
        super(map, x, y, width, height);

        world = VoxelConstants.getClientWorld();
        minLayer = PersistentMap.getMinCaveLayer(world);
        maxLayer = PersistentMap.getMaxCaveLayer(world);

        caveModeField = map.getOptions().showCaves;
        caveModeWidget = caveModeField.createWidget(x + width / 2 - 50, y + 5, 100, 20, this::caveModeUpdated);
        addWidget(caveModeWidget);

        int layerValue = map.isAutoCaveEnabled() ? maxLayer + 1 : map.getCaveLayer();
        (caveLayerField = new IntegerField("WorldMap Cave Layer", "options.worldmap.caveMode.layer", layerValue, minLayer, maxLayer + 1)).withListener(this::updateCaveLayer).withFormat(this::formatLayerRange);
        caveLayerWidget = caveLayerField.createWidget(x + width / 2 - 50, y + 30, 100, 20, (e) -> {});
        addWidget(caveLayerWidget);

        caveModeUpdated(caveModeField.get());
    }

    private void caveModeUpdated(boolean value) {
        boolean allowed = VoxelConstants.getVoxelMapInstance().getServerSettings().manualCavesAllowed.get();
        caveLayerField.setActive(value);
        caveLayerField.setAllowed(allowed);
        if (value && allowed) {
            caveLayerField.set(maxLayer + 1);
        }
        ((IOptionWidget) caveModeWidget).refresh();
        ((IOptionWidget) caveLayerWidget).refresh();
    }

    private void updateCaveLayer(int value) {
        if (value > maxLayer) {
            map.enableAutoCave();
        } else {
            map.setManualCaveLayer(value);
        }
    }

    private String formatLayerRange(int value) {
        if (value > maxLayer) {
            return I18n.get("options.worldmap.caveMode.layer.auto");
        } else {
            int blockMin = PersistentMap.caveLayerToBlock(world, value);
            int blockMax = blockMin + PersistentMap.CAVE_LAYER_HEIGHT;
            return I18n.get("options.worldmap.caveMode.layer.range", blockMin, blockMax);
        }
    }
}
