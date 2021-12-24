package com.mamiyaotaru.voxelmap.util;

import net.minecraft.util.Identifier;
import net.minecraft.client.model.ModelPart;

public class ModelPartWithResourceLocation {
   public ModelPart modelPart;
   public Identifier resourceLocation;

   public ModelPartWithResourceLocation(ModelPart modelPart, Identifier resourceLocation) {
      this.modelPart = modelPart;
      this.resourceLocation = resourceLocation;
   }
}
