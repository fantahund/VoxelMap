package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;

public abstract class AbstractArmorHandler {
    protected final EntityMapImageManager imageManager;
    protected final HashMap<Identifier, EntityArmorDataFactory> armorDataFactories = new HashMap<>();
    protected Entity entity;
    protected EntityRenderer renderer;
    protected int size;
    protected boolean addBorder;

    public AbstractArmorHandler(EntityMapImageManager imageManager) {
        this.imageManager = imageManager;
    }

    protected EntityArmorData getArmorData(Identifier material, int size, boolean addBorder) {
        EntityArmorDataFactory factory = armorDataFactories.get(material);
        if (factory != null) {
            return factory.createArmorData(size, addBorder);
        }

        return null;
    }

    protected EntityArmorData getOrCreateArmorData(Identifier material, Identifier texture, int size, boolean addBorder) {
        if (!armorDataFactories.containsKey(material)) {
            EntityArmorDataFactory factory = new EntityArmorDataFactory(material, texture);
            armorDataFactories.put(material, factory);
        }

        return getArmorData(material, size, addBorder);
    }

    public void setupForEntity(Entity entity, EntityRenderer renderer, int size, boolean addBorder) {
        this.entity = entity;
        this.renderer = renderer;
        this.size = size;
        this.addBorder = addBorder;
    }

    public abstract EntityArmorData getArmorData();

    public abstract void renderArmorModel(EntityMapImageManager.CaptureContext context);

}
