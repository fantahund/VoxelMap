package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.mamiyaotaru.voxelmap.entityrender.AbstractEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public abstract class AbstractArmorHandler {
    protected final HashMap<Item, EntityArmorDataFactory> armorDataFactories = new HashMap<>();
    protected Entity entity;
    protected EntityRenderer renderer;
    protected int size;
    protected boolean addBorder;

    protected EntityArmorData getArmorData(Item item, int identifier) {
        EntityArmorDataFactory factory = armorDataFactories.get(item);
        if (factory != null) {
            return factory.createArmorData(identifier, size, addBorder);
        }

        return null;
    }

    protected EntityArmorData getOrCreateArmorData(Item item, Identifier texture, int identifier) {
        if (!armorDataFactories.containsKey(item)) {
            EntityArmorDataFactory factory = new EntityArmorDataFactory(item, texture);
            armorDataFactories.put(item, factory);
        }

        return getArmorData(item, identifier);
    }

    public void setupForEntity(Entity entity, EntityRenderer renderer, int size, boolean addBorder) {
        this.entity = entity;
        this.renderer = renderer;
        this.size = size;
        this.addBorder = addBorder;
    }

    public abstract EntityArmorData getArmorData();

    public abstract void renderArmorModel(AbstractEntityRenderer renderer);

    public abstract BufferedImage postProcessTexture(BufferedImage image, EntityArmorData armorData);

}
