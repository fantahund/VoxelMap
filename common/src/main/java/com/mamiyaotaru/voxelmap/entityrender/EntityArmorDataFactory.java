package com.mamiyaotaru.voxelmap.entityrender;

import net.minecraft.resources.Identifier;

public class EntityArmorDataFactory {
    private final Identifier material;
    private final Identifier texture;

    public EntityArmorDataFactory(Identifier material, Identifier texture) {
        this.material = material;
        this.texture = texture;
    }

    public Identifier getMaterial() {
        return material;
    }

    public EntityArmorData createArmorData(int size, boolean addBorder) {
        return new EntityArmorData(material, texture, size, addBorder);
    }
}
