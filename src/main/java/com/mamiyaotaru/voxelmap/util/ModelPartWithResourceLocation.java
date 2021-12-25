package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.model.ModelPart;
import net.minecraft.util.Identifier;

public class ModelPartWithResourceLocation {
    public ModelPart modelPart;
    public Identifier resourceLocation;

    public ModelPartWithResourceLocation(ModelPart modelPart, Identifier resourceLocation) {
        this.modelPart = modelPart;
        this.resourceLocation = resourceLocation;
    }
}
