package com.mamiyaotaru.voxelmap.entityrender.armors;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class EntityArmorDataFactory {
    private final Item item;
    private final Identifier texture;

    public EntityArmorDataFactory(Item item, Identifier texture) {
        this.item = item;
        this.texture = texture;
    }

    public Item getItem() {
        return item;
    }

    public EntityArmorData createArmorData(int size, boolean addBorder) {
        return new EntityArmorData(item, texture, size, addBorder);
    }
}
