package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.model.ModelPart;
import net.minecraft.util.Identifier;

public record ModelPartWithResourceLocation(ModelPart modelPart, Identifier resourceLocation) {}